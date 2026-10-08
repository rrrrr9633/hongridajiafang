package top.sama.haode.order.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.AnnouncementService;
import top.sama.haode.order.domain.Announcement;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/announcements")
public class AdminAnnouncementController {
    private final AnnouncementService announcementService;

    public AdminAnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public List<Item> list() {
        return announcementService.listAll().stream().map(Item::from).toList();
    }

    @PostMapping
    public Item create(@RequestBody Request request) {
        try {
            return Item.from(announcementService.create(
                    request == null ? null : request.title(),
                    request == null ? null : request.content(),
                    request == null || request.published() == null || request.published()
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @PutMapping("/{id}")
    public Item update(@PathVariable UUID id, @RequestBody Request request) {
        try {
            return Item.from(announcementService.update(
                    id,
                    request == null ? null : request.title(),
                    request == null ? null : request.content(),
                    request == null || request.published() == null || request.published()
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        try {
            announcementService.delete(id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    public record Request(String title, String content, Boolean published) {}

    public record Item(UUID id, String title, String content, boolean published, Instant createdAt, Instant updatedAt) {
        static Item from(Announcement announcement) {
            return new Item(
                    announcement.getId(),
                    announcement.getTitle(),
                    announcement.getContent(),
                    announcement.isPublished(),
                    announcement.getCreatedAt(),
                    announcement.getUpdatedAt()
            );
        }
    }
}
