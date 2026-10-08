package top.sama.haode.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.service.WechatAuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final WechatAuthService wechatAuthService;

    public AuthController(WechatAuthService wechatAuthService) {
        this.wechatAuthService = wechatAuthService;
    }

    @PostMapping("/wechat")
    public SessionResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            return SessionResponse.from(wechatAuthService.login(request.code(), request.phoneCode()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
        }
    }

    @PostMapping("/dev")
    public SessionResponse devLogin() {
        try {
            return SessionResponse.from(wechatAuthService.devLogin());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
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
