package top.sama.haode.order.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import top.sama.haode.order.domain.MediaAsset;
import top.sama.haode.order.repository.MediaAssetRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class MediaStorage {
    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private final Path root;
    private final MediaAssetRepository assets;

    public MediaStorage(@Value("${media.storage-dir:./.runtime/media}") String storageDir, MediaAssetRepository assets) {
        this.root = Path.of(storageDir).toAbsolutePath().normalize();
        this.assets = assets;
    }

    public MediaAsset store(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片为空或超过 8MB");
        }
        String contentType = file.getContentType();
        if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType) && !MediaType.IMAGE_PNG_VALUE.equals(contentType)
                && !MediaType.IMAGE_GIF_VALUE.equals(contentType) && !"image/webp".equals(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 JPEG、PNG、GIF、WebP 图片");
        }
        String extension = switch (contentType) {
            case MediaType.IMAGE_JPEG_VALUE -> ".jpg";
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            case MediaType.IMAGE_GIF_VALUE -> ".gif";
            default -> ".webp";
        };
        Path destination = root.resolve(UUID.randomUUID() + extension).normalize();
        if (!destination.startsWith(root)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片路径无效");
        try {
            Files.createDirectories(root);
            Path temporary = Files.createTempFile(root, "upload-", ".tmp");
            try {
                file.transferTo(temporary);
                BufferedImage image = ImageIO.read(temporary.toFile());
                if (image == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "上传内容不是有效图片");
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception exception) {
                Files.deleteIfExists(temporary);
                throw exception;
            }
            String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
            if (originalName.length() > 255) originalName = originalName.substring(originalName.length() - 255);
            MediaAsset asset = new MediaAsset(destination.getFileName().toString(), contentType, originalName);
            return assets.save(asset);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片存储失败", exception);
        }
    }

    public MediaAsset findOwned(UUID id, String ownerId) {
        return assets.findById(id)
                .filter(asset -> ownerId != null && ownerId.equals(asset.getOwnerId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片资源无效或不属于当前用户"));
    }

    public Path resolve(MediaAsset asset) {
        Path path = root.resolve(asset.getStoragePath()).normalize();
        if (!path.startsWith(root)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return path;
    }

    public String publicUrl(UUID assetId) { return "/api/media/" + assetId; }
}
