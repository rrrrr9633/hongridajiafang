package top.sama.haode.order.api;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import top.sama.haode.order.domain.MediaAsset;
import top.sama.haode.order.repository.MediaAssetRepository;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    private final MediaStorage storage;
    private final MediaAssetRepository assets;

    public MediaController(MediaStorage storage, MediaAssetRepository assets) {
        this.storage = storage;
        this.assets = assets;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestAttribute("userId") String userId, @RequestPart("file") MultipartFile file) {
        MediaAsset asset = storage.store(file);
        asset.assignOwner(userId);
        assets.save(asset);
        return Map.of("id", asset.getId().toString(), "url", storage.publicUrl(asset.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> read(@PathVariable UUID id) throws IOException {
        MediaAsset asset = assets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        byte[] bytes = java.nio.file.Files.readAllBytes(storage.resolve(asset));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.getContentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic()).body(bytes);
    }
}
