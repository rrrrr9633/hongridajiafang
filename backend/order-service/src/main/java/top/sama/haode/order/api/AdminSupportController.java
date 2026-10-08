package top.sama.haode.order.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.SupportService;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/support")
public class AdminSupportController {
    private final SupportService supportService;

    public AdminSupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/settings")
    public SupportController.SettingsResponse settings() {
        return new SupportController.SettingsResponse(supportService.settings().getWechatId());
    }

    @PutMapping("/settings")
    public SupportController.SettingsResponse updateSettings(@RequestBody SupportController.SettingsResponse request) {
        return new SupportController.SettingsResponse(supportService.updateWechatId(request == null ? null : request.wechatId()).getWechatId());
    }

    @GetMapping("/threads")
    public List<SupportService.ThreadView> threads(@RequestParam(defaultValue = "") String q) {
        return supportService.listThreads(q);
    }

    @GetMapping("/threads/{userId}/messages")
    public List<SupportService.MessageView> messages(
            @PathVariable String userId,
            @RequestParam(required = false) Instant after
    ) {
        try {
            return supportService.listAdminMessages(userId, after).stream().map(SupportService.MessageView::from).toList();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @PostMapping("/threads/{userId}/messages")
    public SupportService.MessageView send(@PathVariable String userId, @RequestBody SupportController.SendRequest request) {
        try {
            return SupportService.MessageView.from(supportService.sendFromAdmin(userId, request == null ? null : request.content()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
