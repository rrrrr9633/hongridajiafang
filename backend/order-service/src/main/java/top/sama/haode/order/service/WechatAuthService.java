package top.sama.haode.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import top.sama.haode.order.domain.InviteCodes;
import top.sama.haode.order.domain.Session;
import top.sama.haode.order.domain.User;
import top.sama.haode.order.repository.SessionRepository;
import top.sama.haode.order.repository.UserRepository;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class WechatAuthService {
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private static final String DEV_OPENID = "dev-local-openid";
    private static final String DEV_PHONE = "13800000000";

    private final String appId;
    private final String secret;
    private final String appEnv;
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile CachedToken cachedToken;

    public WechatAuthService(
            UserRepository userRepository,
            SessionRepository sessionRepository,
            @Value("${wechat.app-id:}") String appId,
            @Value("${wechat.secret:}") String secret,
            @Value("${app.env:production}") String appEnv,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(20_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.objectMapper = objectMapper;
        this.appId = appId;
        this.secret = secret;
        this.appEnv = appEnv;
    }

    @Transactional
    public AuthSession login(String code, String phoneCode) {
        if (appId.isBlank() || secret.isBlank()) {
            throw new IllegalStateException("微信登录配置未完成");
        }

        WechatSession session = requestWechatSession(code);
        String phone = requestPhoneNumber(accessToken(), phoneCode);
        User user = upsertUser(session.openid(), phone);
        return issueSession(user);
    }

    @Transactional
    public AuthSession devLogin() {
        if (!"local".equalsIgnoreCase(appEnv)) {
            throw new IllegalStateException("当前环境不允许开发登录");
        }
        User user = upsertUser(DEV_OPENID, DEV_PHONE);
        return issueSession(user);
    }

    private User upsertUser(String openid, String phone) {
        return userRepository.findByPhone(phone)
                .map(existing -> existing.bindWechatOpenid(openid))
                .orElseGet(() -> userRepository.save(new User(UUID.randomUUID().toString(), openid, phone, InviteCodes.random())));
    }

    private AuthSession issueSession(User user) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String accessToken = HexFormat.of().formatHex(tokenBytes);
        sessionRepository.save(new Session(sha256(accessToken), user.getId(), Instant.now().plus(Duration.ofDays(7))));
        return new AuthSession(accessToken);
    }

    private WechatSession requestWechatSession(String code) {
        JsonNode node = wechatCall("GET", "/sns/jscode2session", Map.of(
                "appid", appId,
                "secret", secret,
                "js_code", code,
                "grant_type", "authorization_code"
        ), null);
        String openid = node.path("openid").asText("");
        int errcode = node.path("errcode").asInt(0);
        String errmsg = node.path("errmsg").asText("");
        if (errcode != 0 || openid.isBlank()) {
            throw new IllegalArgumentException(errmsg.isBlank() ? "微信登录凭证无效" : "微信登录凭证无效：" + errmsg);
        }
        return new WechatSession(openid, errcode, errmsg);
    }

    public String accessToken() {
        CachedToken cached = cachedToken;
        if (cached != null && cached.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return cached.value();
        }
        synchronized (this) {
            cached = cachedToken;
            if (cached != null && cached.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
                return cached.value();
            }
            JsonNode node = wechatCall("GET", "/cgi-bin/token", Map.of(
                    "appid", appId,
                    "secret", secret,
                    "grant_type", "client_credential"
            ), null);
            String token = node.path("access_token").asText("");
            int errcode = node.path("errcode").asInt(0);
            String errmsg = node.path("errmsg").asText("");
            if (errcode != 0 || token.isBlank()) {
                throw new IllegalStateException(errmsg.isBlank() ? "微信服务端令牌获取失败" : "微信服务端令牌获取失败：" + errmsg);
            }
            int ttl = node.path("expires_in").asInt(7200);
            if (ttl < 60) ttl = 7000;
            cachedToken = new CachedToken(token, Instant.now().plusSeconds(ttl));
            return token;
        }
    }

    private String requestPhoneNumber(String accessToken, String phoneCode) {
        JsonNode node = wechatCall(
                "POST",
                "/wxa/business/getuserphonenumber",
                Map.of("access_token", accessToken),
                new PhoneCodeRequest(phoneCode)
        );
        int errcode = node.path("errcode").asInt(0);
        String errmsg = node.path("errmsg").asText("");
        String phone = node.path("phone_info").path("phoneNumber").asText("");
        if (errcode != 0 || phone.isBlank()) {
            throw new IllegalArgumentException(errmsg.isBlank() ? "微信手机号授权无效" : "微信手机号授权无效：" + errmsg);
        }
        return phone;
    }

    private JsonNode wechatCall(String method, String path, Map<String, String> query, Object body) {
        try {
            var uriBuilder = UriComponentsBuilder.fromUriString("https://api.weixin.qq.com" + path);
            query.forEach(uriBuilder::queryParam);
            URI uri = uriBuilder.encode().build().toUri();
            var spec = restClient.method(HttpMethod.valueOf(method))
                    .uri(uri)
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept", "*/*");
            if (body != null) {
                spec = spec.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            return spec.exchange((request, response) -> {
                String text = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                int status = response.getStatusCode().value();
                if (text == null || text.isBlank()) {
                    throw new IllegalStateException("微信接口无响应（HTTP " + status + "）");
                }
                return objectMapper.readTree(text);
            });
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("微信接口请求失败：" + exception.getMessage(), exception);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成会话摘要", exception);
        }
    }

    public record AuthSession(String accessToken) {
    }

    private record PhoneCodeRequest(String code) {
    }

    private record WechatSession(String openid, Integer errcode, String errmsg) {
    }

    private record CachedToken(String value, Instant expiresAt) {}
}
