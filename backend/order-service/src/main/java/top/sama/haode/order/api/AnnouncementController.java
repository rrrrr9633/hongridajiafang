package top.sama.haode.order.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.AnnouncementService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public List<Item> list(@RequestAttribute("userId") String userId) {
        var readIds = announcementService.readIds(userId);
        return announcementService.listPublished().stream()
                .map(item -> new Item(item.getId(), item.getTitle(), item.getContent(), item.getCreatedAt(), !readIds.contains(item.getId())))
                .toList();
    }

    @GetMapping("/unread-count")
    public Unread unread(@RequestAttribute("userId") String userId) {
        return new Unread(announcementService.unreadCount(userId));
    }

    @PostMapping("/{id}/read")
    public Unread read(@RequestAttribute("userId") String userId, @PathVariable UUID id) {
        try {
            announcementService.markRead(userId, id);
            return new Unread(announcementService.unreadCount(userId));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @PostMapping("/read-all")
    public Unread readAll(@RequestAttribute("userId") String userId) {
        announcementService.markAllRead(userId);
        return new Unread(0);
    }

    public record Item(UUID id, String title, String content, Instant createdAt, boolean unread) {}
    public record Unread(long count) {}
}
