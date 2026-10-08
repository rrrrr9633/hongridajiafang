package top.sama.haode.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class AdminAuthService {
    private final String username;
    private final String password;
    private final byte[] secret;

    public AdminAuthService(
            @Value("${admin.username:}") String username,
            @Value("${admin.password:}") String password,
            @Value("${admin.token-secret:}") String tokenSecret
    ) {
        this.username = username;
        this.password = password;
        this.secret = tokenSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String login(String candidateUsername, String candidatePassword) {
        if (username.isBlank() || password.isBlank() || secret.length == 0
                || !MessageDigest.isEqual(username.getBytes(StandardCharsets.UTF_8), candidateUsername.getBytes(StandardCharsets.UTF_8))
                || !MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8), candidatePassword.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("管理员账号或密码错误");
        }
        long expiresAt = Instant.now().plusSeconds(8 * 3600).getEpochSecond();
        String payload = username + "." + expiresAt;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + sign(payload);
    }

    public boolean isValid(String token) {
        try {
            String[] parts = token.split("\\.", 2);
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] values = payload.split("\\.", 2);
            return parts.length == 2 && values.length == 2
                    && values[0].equals(username)
                    && Long.parseLong(values[1]) > Instant.now().getEpochSecond()
                    && MessageDigest.isEqual(parts[1].getBytes(StandardCharsets.UTF_8), sign(payload).getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("管理员令牌签名失败", exception);
        }
    }
}
