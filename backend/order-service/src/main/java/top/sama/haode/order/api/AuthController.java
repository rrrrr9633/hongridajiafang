package top.sama.haode.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sama.haode.order.service.WechatAuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final WechatAuthService wechatAuthService;

    public AuthController(WechatAuthService wechatAuthService) {
        this.wechatAuthService = wechatAuthService;
    }

    @PostMapping("/wechat")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(SessionResponse.from(wechatAuthService.login(request.code(), request.phoneCode())));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            int status = exception instanceof IllegalArgumentException ? 400 : 503;
            return ResponseEntity.status(status).body(Map.of("message", text(exception)));
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body(Map.of("message", text(exception)));
        }
    }

    @PostMapping("/dev")
    public ResponseEntity<?> devLogin() {
        try {
            return ResponseEntity.ok(SessionResponse.from(wechatAuthService.devLogin()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(404).body(Map.of("message", text(exception)));
        }
    }

    private static String text(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "登录失败" : message;
    }

    public record LoginRequest(
            @NotBlank String code,
            @NotBlank String phoneCode
    ) {
    }

    public record SessionResponse(String accessToken) {
        static SessionResponse from(WechatAuthService.AuthSession session) {
            return new SessionResponse(session.accessToken());
        }
    }
}
