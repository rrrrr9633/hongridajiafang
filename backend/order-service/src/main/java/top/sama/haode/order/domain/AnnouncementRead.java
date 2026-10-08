package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "announcement_reads")
public class AnnouncementRead {
    @EmbeddedId
    private Id id;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    protected AnnouncementRead() {
    }

    public AnnouncementRead(UUID announcementId, String userId) {
        this.id = new Id(announcementId, userId);
        this.readAt = Instant.now();
    }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "announcement_id")
        private UUID announcementId;

        @Column(name = "user_id", length = 64)
        private String userId;

        protected Id() {
        }

        public Id(UUID announcementId, String userId) {
            this.announcementId = announcementId;
            this.userId = userId;
        }

        public UUID getAnnouncementId() { return announcementId; }
        public String getUserId() { return userId; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Id that)) return false;
            return Objects.equals(announcementId, that.announcementId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(announcementId, userId);
        }
    }
}
