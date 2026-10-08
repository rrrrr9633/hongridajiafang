package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
public class MediaAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "storage_path", nullable = false, length = 1024)
    private String storagePath;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "owner_id", length = 64)
    private String ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MediaAsset() {}

    public MediaAsset(String storagePath, String contentType, String originalName) {
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.originalName = originalName;
        this.createdAt = Instant.now();
    }

    public void assignOwner(String ownerId) { this.ownerId = ownerId; }

    public UUID getId() { return id; }
    public String getStoragePath() { return storagePath; }
    public String getContentType() { return contentType; }
    public String getOriginalName() { return originalName; }
    public String getOwnerId() { return ownerId; }
}
