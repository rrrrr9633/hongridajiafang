package top.sama.haode.order.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sama.haode.order.domain.Announcement;
import top.sama.haode.order.domain.AnnouncementRead;
import top.sama.haode.order.repository.AnnouncementReadRepository;
import top.sama.haode.order.repository.AnnouncementRepository;

import java.util.List;
import java.util.UUID;

@Service
public class AnnouncementService {
    private final AnnouncementRepository announcements;
    private final AnnouncementReadRepository reads;

    public AnnouncementService(AnnouncementRepository announcements, AnnouncementReadRepository reads) {
        this.announcements = announcements;
        this.reads = reads;
    }

    @Transactional(readOnly = true)
    public List<Announcement> listAll() {
        return announcements.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Announcement> listPublished() {
        return announcements.findByPublishedTrueOrderByCreatedAtDesc();
    }

    @Transactional
    public Announcement create(String title, String content, boolean published) {
        return announcements.save(new Announcement(title, content, published));
    }

    @Transactional
    public Announcement update(UUID id, String title, String content, boolean published) {
        Announcement announcement = announcements.findById(id).orElseThrow(() -> new IllegalArgumentException("公告不存在"));
        announcement.replace(title, content, published);
        return announcement;
    }

    @Transactional
    public void delete(UUID id) {
        if (!announcements.existsById(id)) throw new IllegalArgumentException("公告不存在");
        reads.deleteByAnnouncementId(id);
        announcements.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        long published = announcements.countByPublishedTrue();
        long read = reads.countByUserId(userId);
        return Math.max(0, published - read);
    }

    @Transactional(readOnly = true)
    public java.util.Set<java.util.UUID> readIds(String userId) {
        return new java.util.HashSet<>(reads.findReadIdsByUserId(userId));
    }

    @Transactional
    public void markRead(String userId, UUID announcementId) {
        Announcement announcement = announcements.findById(announcementId)
                .orElseThrow(() -> new IllegalArgumentException("公告不存在"));
        if (!announcement.isPublished()) return;
        AnnouncementRead.Id id = new AnnouncementRead.Id(announcementId, userId);
        if (reads.existsById(id)) return;
        reads.save(new AnnouncementRead(announcementId, userId));
    }

    @Transactional
    public void markAllRead(String userId) {
        for (Announcement announcement : listPublished()) {
            markRead(userId, announcement.getId());
        }
    }
}
