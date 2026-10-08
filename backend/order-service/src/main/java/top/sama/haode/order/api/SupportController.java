package top.sama.haode.order.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.SupportService;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/support")
public class SupportController {
    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/settings")
    public SettingsResponse settings() {
        return new SettingsResponse(supportService.settings().getWechatId());
    }

    @GetMapping("/messages")
    public List<SupportService.MessageView> messages(
            @RequestAttribute("userId") String userId,
            @RequestParam(required = false) Instant after
    ) {
        return supportService.listUserMessages(userId, after).stream().map(SupportService.MessageView::from).toList();
    }

    @PostMapping("/messages")
    public SupportService.MessageView send(
            @RequestAttribute("userId") String userId,
            @RequestBody SendRequest request
    ) {
        try {
            return SupportService.MessageView.from(supportService.sendFromUser(userId, request == null ? null : request.content()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    public record SettingsResponse(String wechatId) {}
    public record SendRequest(String content) {}
}
