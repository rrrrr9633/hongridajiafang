package top.sama.haode.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import top.sama.haode.order.api.PaymentController;
import top.sama.haode.order.domain.Checkout;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WechatPayService {
    private final String appId;
    private final String mchid;
    private final String serialNo;
    private final String privateKeyPath;
    private final String apiV3Key;
    private final String notifyUrl;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();
    private final SecureRandom random = new SecureRandom();
    private volatile PrivateKey privateKey;

    public WechatPayService(
            @Value("${wechat.app-id:}") String appId,
            @Value("${wechat.pay.mchid:}") String mchid,
            @Value("${wechat.pay.serial-no:}") String serialNo,
            @Value("${wechat.pay.private-key-path:}") String privateKeyPath,
            @Value("${wechat.pay.api-v3-key:}") String apiV3Key,
            @Value("${wechat.pay.notify-url:}") String notifyUrl,
            ObjectMapper objectMapper
    ) {
        this.appId = appId == null ? "" : appId.trim();
        this.mchid = mchid == null ? "" : mchid.trim();
        this.serialNo = serialNo == null ? "" : serialNo.trim();
        this.privateKeyPath = privateKeyPath == null ? "" : privateKeyPath.trim();
        this.apiV3Key = apiV3Key == null ? "" : apiV3Key.trim();
        this.notifyUrl = notifyUrl == null ? "" : notifyUrl.trim();
        this.objectMapper = objectMapper;
    }

    public boolean configured() {
        return !appId.isBlank() && !mchid.isBlank() && !serialNo.isBlank()
                && !privateKeyPath.isBlank() && !apiV3Key.isBlank() && !notifyUrl.isBlank()
                && Files.isRegularFile(Path.of(privateKeyPath));
    }

    public PaymentController.WechatPaymentResponse prepay(Checkout checkout, String openid, String description) {
        ensureReady();
        if (openid == null || openid.isBlank() || openid.startsWith("dev-")) {
            throw new IllegalStateException("当前账号没有可用的微信 openid，无法拉起支付");
        }
        int fen = checkout.getAmount().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
        if (fen < 1) throw new IllegalArgumentException("支付金额无效");
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("appid", appId);
            body.put("mchid", mchid);
            body.put("description", trimDescription(description));
            body.put("out_trade_no", outTradeNo(checkout.getId()));
            body.put("notify_url", notifyUrl);
            body.put("amount", Map.of("total", fen, "currency", "CNY"));
            body.put("payer", Map.of("openid", openid));
            String json = objectMapper.writeValueAsString(body);
            String response = restClient.post()
                    .uri("https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", authorization("POST", "/v3/pay/transactions/jsapi", json))
                    .body(json)
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(response == null ? "{}" : response);
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

    public NotifyResult parseNotify(String body) {
        ensureReady();
        try {
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            JsonNode resource = root.path("resource");
            String plaintext = decrypt(
                    resource.path("associated_data").asText(""),
                    resource.path("nonce").asText(""),
                    resource.path("ciphertext").asText("")
            );
            JsonNode transaction = objectMapper.readTree(plaintext);
            return new NotifyResult(
                    transaction.path("out_trade_no").asText(""),
                    transaction.path("transaction_id").asText(""),
                    transaction.path("trade_state").asText(""),
                    transaction.path("mchid").asText("")
            );
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("支付回调解析失败", exception);
        }
    }

    public static String outTradeNo(UUID checkoutId) {
        return checkoutId.toString().replace("-", "");
    }

    public static UUID checkoutIdFromOutTradeNo(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.length() != 32) throw new IllegalArgumentException("支付单号无效");
        String raw = outTradeNo.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5"
        );
        return UUID.fromString(raw);
    }

    private void ensureReady() {
        if (!configured()) throw new IllegalStateException("微信支付尚未配置");
        privateKey();
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
                        String pem = Files.readString(Path.of(privateKeyPath))
                                .replace("-----BEGIN PRIVATE KEY-----", "")
                                .replace("-----END PRIVATE KEY-----", "")
                                .replaceAll("\\s", "");
                        privateKey = KeyFactory.getInstance("RSA")
                                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
                    } catch (Exception exception) {
                        throw new IllegalStateException("无法读取微信支付私钥", exception);
                    }
                }
            }
        }
        return privateKey;
    }

    private String decrypt(String associatedData, String nonce, String ciphertext) throws Exception {
        byte[] key = apiV3Key.getBytes(StandardCharsets.UTF_8);
        byte[] packed = Base64.getDecoder().decode(ciphertext);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(packed), StandardCharsets.UTF_8);
    }

    private String nonce() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String trimDescription(String description) {
        String text = description == null || description.isBlank() ? "红日大家纺" : description.trim();
        return text.length() > 40 ? text.substring(0, 40) : text;
    }

    public record NotifyResult(String outTradeNo, String transactionId, String tradeState, String mchid) {}
}
