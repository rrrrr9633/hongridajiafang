package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcements")
public class Announcement {
    @Id
    private UUID id;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Announcement() {
    }

    public Announcement(String title, String content, boolean published) {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        replace(title, content, published);
    }

    public void replace(String title, String content, boolean published) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("请填写公告标题");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("请填写公告内容");
        if (title.trim().length() > 128) throw new IllegalArgumentException("公告标题过长");
        if (content.length() > 4000) throw new IllegalArgumentException("公告内容过长");
        this.title = title.trim();
        this.content = content.trim();
        this.published = published;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isPublished() { return published; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
