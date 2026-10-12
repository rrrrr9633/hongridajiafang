package top.sama.haode.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import top.sama.haode.order.api.PaymentController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WechatPayService {
    private final String appId;
    private final String mchid;
    private final String serialNo;
    private final String privateKeyPath;
    private final String publicKeyPath;
    private final String publicKeyId;
    private final String apiV3Key;
    private final String notifyUrl;
    private final String refundNotifyUrl;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();
    private final SecureRandom random = new SecureRandom();
    private volatile PrivateKey privateKey;
    private volatile PublicKey publicKey;

    public WechatPayService(
            @Value("${wechat.app-id:}") String appId,
            @Value("${wechat.pay.mchid:}") String mchid,
            @Value("${wechat.pay.serial-no:}") String serialNo,
            @Value("${wechat.pay.private-key-path:}") String privateKeyPath,
            @Value("${wechat.pay.public-key-path:}") String publicKeyPath,
            @Value("${wechat.pay.public-key-id:}") String publicKeyId,
            @Value("${wechat.pay.api-v3-key:}") String apiV3Key,
            @Value("${wechat.pay.notify-url:}") String notifyUrl,
            @Value("${wechat.pay.refund-notify-url:}") String refundNotifyUrl,
            ObjectMapper objectMapper
    ) {
        this.appId = trim(appId);
        this.mchid = trim(mchid);
        this.serialNo = trim(serialNo);
        this.privateKeyPath = trim(privateKeyPath);
        this.publicKeyPath = trim(publicKeyPath);
        this.publicKeyId = trim(publicKeyId);
        this.apiV3Key = trim(apiV3Key);
        this.notifyUrl = trim(notifyUrl);
        this.refundNotifyUrl = trim(refundNotifyUrl).isBlank()
                ? this.notifyUrl.replace("/notify", "/refund-notify")
                : trim(refundNotifyUrl);
        this.objectMapper = objectMapper;
    }

    public boolean configured() {
        return !appId.isBlank() && !mchid.isBlank() && !serialNo.isBlank()
                && !privateKeyPath.isBlank() && !apiV3Key.isBlank() && !notifyUrl.isBlank()
                && !publicKeyPath.isBlank() && !publicKeyId.isBlank()
                && Files.isRegularFile(Path.of(privateKeyPath))
                && Files.isRegularFile(Path.of(publicKeyPath));
    }

    public PaymentController.WechatPaymentResponse prepay(UUID outTradeId, BigDecimal amount, String openid, String description) {
        ensureReady();
        if (openid == null || openid.isBlank() || openid.startsWith("dev-")) {
            throw new IllegalStateException("当前账号没有可用的微信 openid，无法拉起支付");
        }
        int fen = toFen(amount);
        if (fen < 1) throw new IllegalArgumentException("支付金额无效");
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("appid", appId);
            body.put("mchid", mchid);
            body.put("description", trimDescription(description));
            body.put("out_trade_no", outTradeNo(outTradeId));
            body.put("notify_url", notifyUrl);
            body.put("amount", Map.of("total", fen, "currency", "CNY"));
            body.put("payer", Map.of("openid", openid));
            JsonNode node = call("POST", "/v3/pay/transactions/jsapi", objectMapper.writeValueAsString(body));
            String prepayId = node.path("prepay_id").asText("");
            if (prepayId.isBlank()) throw new IllegalStateException("微信未返回 prepay_id");
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String nonce = nonce();
            String packageValue = "prepay_id=" + prepayId;
            String message = appId + "\n" + timestamp + "\n" + nonce + "\n" + packageValue + "\n";
            return new PaymentController.WechatPaymentResponse(timestamp, nonce, packageValue, "RSA", sign(message));
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("微信下单失败：" + exception.getMessage(), exception);
        }
    }

    public void verifyNotify(String timestamp, String nonce, String body, String signature, String serial) {
        ensureReady();
        if (blank(timestamp) || blank(nonce) || blank(signature) || blank(serial)) {
            throw new IllegalArgumentException("支付回调缺少签名头");
        }
        if (!publicKeyId.equals(serial)) {
            throw new IllegalArgumentException("支付回调公钥编号不匹配");
        }
        long epoch;
        try {
            epoch = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("支付回调时间戳无效");
        }
        if (Math.abs(Instant.now().getEpochSecond() - epoch) > 300) {
            throw new IllegalArgumentException("支付回调已过期");
        }
        String message = timestamp + "\n" + nonce + "\n" + (body == null ? "" : body) + "\n";
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey());
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(signature))) {
                throw new IllegalArgumentException("支付回调签名无效");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("支付回调验签失败", exception);
        }
    }

    public NotifyResult parseNotify(String body) {
        ensureReady();
        try {
            JsonNode transaction = decryptResource(objectMapper.readTree(body == null ? "{}" : body).path("resource"));
            return toNotifyResult(transaction);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("支付回调解析失败", exception);
        }
    }

    public RefundNotifyResult parseRefundNotify(String body) {
        ensureReady();
        try {
            JsonNode refund = decryptResource(objectMapper.readTree(body == null ? "{}" : body).path("resource"));
            return new RefundNotifyResult(
                    refund.path("out_refund_no").asText(""),
                    refund.path("refund_id").asText(""),
                    refund.path("refund_status").asText("")
            );
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("退款回调解析失败", exception);
        }
    }

    public Optional<NotifyResult> queryByOutTradeNo(UUID outTradeId) {
        ensureReady();
        String path = "/v3/pay/transactions/out-trade-no/" + outTradeNo(outTradeId) + "?mchid=" + mchid;
        try {
            JsonNode node = call("GET", path, "");
            String code = node.path("code").asText("");
            if ("RESOURCE_NOT_EXISTS".equals(code) || "ORDER_NOT_EXIST".equals(code)) return Optional.empty();
            if (node.path("out_trade_no").asText("").isBlank()) return Optional.empty();
            return Optional.of(toNotifyResult(node));
        } catch (IllegalStateException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("ORDER_NOT_EXIST")) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    public RefundResult refund(String transactionId, UUID refundId, BigDecimal refundAmount, BigDecimal totalAmount, String reason) {
        ensureReady();
        if (transactionId == null || transactionId.isBlank()) throw new IllegalStateException("缺少微信支付单号，无法退款");
        int refundFen = toFen(refundAmount);
        int totalFen = toFen(totalAmount);
        if (refundFen < 1 || totalFen < 1 || refundFen > totalFen) throw new IllegalArgumentException("退款金额无效");
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("transaction_id", transactionId);
            body.put("out_refund_no", outTradeNo(refundId));
            body.put("reason", trimDescription(reason));
            if (!refundNotifyUrl.isBlank()) body.put("notify_url", refundNotifyUrl);
            body.put("amount", Map.of("refund", refundFen, "total", totalFen, "currency", "CNY"));
            JsonNode node = call("POST", "/v3/refund/domestic/refunds", objectMapper.writeValueAsString(body));
            return new RefundResult(
                    node.path("out_refund_no").asText(outTradeNo(refundId)),
                    node.path("refund_id").asText(""),
                    node.path("status").asText("")
            );
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("微信退款失败：" + exception.getMessage(), exception);
        }
    }

    public static String outTradeNo(UUID id) {
        return id.toString().replace("-", "");
    }

    public static UUID idFromOutTradeNo(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.length() != 32) throw new IllegalArgumentException("支付单号无效");
        String raw = outTradeNo.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5"
        );
        return UUID.fromString(raw);
    }

    public static UUID checkoutIdFromOutTradeNo(String outTradeNo) {
        return idFromOutTradeNo(outTradeNo);
    }

    private NotifyResult toNotifyResult(JsonNode transaction) {
        return new NotifyResult(
                transaction.path("out_trade_no").asText(""),
                transaction.path("transaction_id").asText(""),
                transaction.path("trade_state").asText(""),
                transaction.path("mchid").asText("")
        );
    }

    private JsonNode decryptResource(JsonNode resource) throws Exception {
        String plaintext = decrypt(
                resource.path("associated_data").asText(""),
                resource.path("nonce").asText(""),
                resource.path("ciphertext").asText("")
        );
        return objectMapper.readTree(plaintext);
    }

    private JsonNode call(String method, String path, String body) {
        try {
            String payload = body == null ? "" : body;
            var spec = restClient.method(HttpMethod.valueOf(method))
                    .uri("https://api.mch.weixin.qq.com" + path)
                    .header("Authorization", authorization(method, path, payload))
                    .accept(MediaType.APPLICATION_JSON);
            if ("POST".equals(method) || "PATCH".equals(method) || "PUT".equals(method)) {
                spec = spec.contentType(MediaType.APPLICATION_JSON).body(payload);
            }
            return spec.exchange((request, response) -> {
                String text = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonNode node = objectMapper.readTree(text == null || text.isBlank() ? "{}" : text);
                int status = response.getStatusCode().value();
                String code = node.path("code").asText("");
                if (status == 404 || "RESOURCE_NOT_EXISTS".equals(code) || "ORDER_NOT_EXIST".equals(code)) {
                    return node;
                }
                if (status >= 400) {
                    String message = node.path("message").asText(text);
                    throw new IllegalStateException(code.isBlank() ? message : code + " " + message);
                }
                return node;
            });
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("请求微信支付失败：" + exception.getMessage(), exception);
        }
    }

    private void ensureReady() {
        if (!configured()) throw new IllegalStateException("微信支付尚未配置");
        privateKey();
        publicKey();
    }

    private String authorization(String method, String path, String body) throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = nonce();
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        String signature = sign(message);
        return "WECHATPAY2-SHA256-RSA2048 mchid=\"" + mchid + "\",nonce_str=\"" + nonce
                + "\",timestamp=\"" + timestamp + "\",serial_no=\"" + serialNo
                + "\",signature=\"" + signature + "\"";
    }

    private String sign(String message) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey());
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private PrivateKey privateKey() {
        if (privateKey == null) {
            synchronized (this) {
                if (privateKey == null) {
                    try {
                        privateKey = KeyFactory.getInstance("RSA")
                                .generatePrivate(new PKCS8EncodedKeySpec(readPem(privateKeyPath)));
                    } catch (Exception exception) {
                        throw new IllegalStateException("无法读取微信支付私钥", exception);
                    }
                }
            }
        }
        return privateKey;
    }

    private PublicKey publicKey() {
        if (publicKey == null) {
            synchronized (this) {
                if (publicKey == null) {
                    try {
                        publicKey = KeyFactory.getInstance("RSA")
                                .generatePublic(new X509EncodedKeySpec(readPem(publicKeyPath)));
                    } catch (Exception exception) {
                        throw new IllegalStateException("无法读取微信支付公钥", exception);
                    }
                }
            }
        }
        return publicKey;
    }

    private static byte[] readPem(String path) throws Exception {
        String pem = Files.readString(Path.of(path))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(pem);
    }

    private String decrypt(String associatedData, String nonce, String ciphertext) throws Exception {
        byte[] key = apiV3Key.getBytes(StandardCharsets.UTF_8);
        byte[] packed = Base64.getDecoder().decode(ciphertext);
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                javax.crypto.Cipher.DECRYPT_MODE,
                new javax.crypto.spec.SecretKeySpec(key, "AES"),
                new javax.crypto.spec.GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8))
        );
        cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(packed), StandardCharsets.UTF_8);
    }

    private String nonce() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static int toFen(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private static String trimDescription(String description) {
        String text = description == null || description.isBlank() ? "红日大家纺" : description.trim();
        return text.length() > 40 ? text.substring(0, 40) : text;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record NotifyResult(String outTradeNo, String transactionId, String tradeState, String mchid) {}
    public record RefundNotifyResult(String outRefundNo, String refundId, String refundStatus) {}
    public record RefundResult(String outRefundNo, String refundId, String status) {}
}
