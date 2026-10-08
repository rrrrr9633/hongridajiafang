package top.sama.haode.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import top.sama.haode.order.domain.Session;
import top.sama.haode.order.domain.User;
import top.sama.haode.order.repository.SessionRepository;
import top.sama.haode.order.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
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
        this.restClient = RestClient.create();
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
        String phone = requestPhoneNumber(requestAccessToken(), phoneCode);
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
                .orElseGet(() -> userRepository.save(new User(UUID.randomUUID().toString(), openid, phone)));
    }

    private AuthSession issueSession(User user) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String accessToken = HexFormat.of().formatHex(tokenBytes);
        sessionRepository.save(new Session(sha256(accessToken), user.getId(), Instant.now().plus(Duration.ofDays(7))));
        return new AuthSession(accessToken);
    }

    private WechatSession requestWechatSession(String code) {
        String responseBody = restClient.get()
                .uri(uriBuilder -> uriBuilder.scheme("https").host("api.weixin.qq.com")
                        .path("/sns/jscode2session").queryParam("appid", appId)
                        .queryParam("secret", secret).queryParam("js_code", code)
                        .queryParam("grant_type", "authorization_code").build())
                .retrieve().body(String.class);
        try {
            WechatSession response = objectMapper.readValue(responseBody, WechatSession.class);
            if (response.errcode() != null || response.openid() == null || response.openid().isBlank()) {
                throw new IllegalArgumentException("微信登录凭证无效");
            }
            return response;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("微信登录响应格式无效", exception);
        }
    }

    private String requestAccessToken() {
        String responseBody = restClient.get()
                .uri(uriBuilder -> uriBuilder.scheme("https").host("api.weixin.qq.com")
                        .path("/cgi-bin/token").queryParam("appid", appId)
                        .queryParam("secret", secret).queryParam("grant_type", "client_credential").build())
                .retrieve().body(String.class);
        try {
            WechatAccessToken response = objectMapper.readValue(responseBody, WechatAccessToken.class);
            if (response.errcode() != null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new IllegalStateException("微信服务端令牌获取失败");
            }
            return response.accessToken();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("微信服务端令牌响应格式无效", exception);
        }
    }

    private String requestPhoneNumber(String accessToken, String phoneCode) {
        String responseBody = restClient.post()
                .uri("https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token={accessToken}", accessToken)
                .body(new PhoneCodeRequest(phoneCode)).retrieve().body(String.class);
        try {
            WechatPhoneResponse response = objectMapper.readValue(responseBody, WechatPhoneResponse.class);
            if (response.errcode() != null || response.phoneInfo() == null
                    || response.phoneInfo().phoneNumber() == null || response.phoneInfo().phoneNumber().isBlank()) {
                throw new IllegalArgumentException("微信手机号授权无效");
            }
            return response.phoneInfo().phoneNumber();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("微信手机号响应格式无效", exception);
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

    private record WechatAccessToken(
            @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken,
            Integer errcode,
            String errmsg
    ) {
    }

    private record WechatPhoneResponse(
            Integer errcode,
            String errmsg,
            @com.fasterxml.jackson.annotation.JsonProperty("phone_info") PhoneInfo phoneInfo
    ) {
    }

    private record PhoneInfo(
            @com.fasterxml.jackson.annotation.JsonProperty("phoneNumber") String phoneNumber
    ) {
    }
}
