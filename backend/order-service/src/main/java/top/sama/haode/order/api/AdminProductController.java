package top.sama.haode.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.domain.Product;
import top.sama.haode.order.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductRepository productRepository;
    private final MediaStorage mediaStorage;

    public AdminProductController(ProductRepository productRepository, MediaStorage mediaStorage) {
        this.productRepository = productRepository;
        this.mediaStorage = mediaStorage;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    @PostMapping
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        Product product = new Product(
                UUID.randomUUID().toString(), request.name(), request.subtitle(), request.price(), request.originalPrice(),
                request.color(), request.accent(), request.pattern(), request.tag(), request.zone(),
                Boolean.TRUE.equals(request.homepage()), request.sortOrder() == null ? 0 : request.sortOrder(),
                request.imageUrl(), request.active());
        return ProductResponse.from(productRepository.save(product));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        product.update(request.name(), request.subtitle(), request.price(), request.originalPrice(),
                request.color(), request.accent(), request.pattern(), request.tag(), request.zone(),
                Boolean.TRUE.equals(request.homepage()), request.sortOrder() == null ? 0 : request.sortOrder(),
                request.imageUrl(), request.active());
        return ProductResponse.from(productRepository.save(product));
    }

    @PostMapping("/{id}/image")
    public ProductResponse uploadImage(@PathVariable String id, @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        product.setImageAssetId(mediaStorage.store(file).getId());
        return ProductResponse.from(productRepository.save(product));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        product.deactivate();
        productRepository.save(product);
    }

    public record ProductRequest(
            @NotBlank String name,
            @NotBlank String subtitle,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            BigDecimal originalPrice,
            String color,
            String accent,
            String pattern,
            String tag,
            String zone,
            Boolean homepage,
            Integer sortOrder,
            String imageUrl,
            @NotNull Boolean active
    ) {}

    public record ProductResponse(String id, String name, String subtitle, BigDecimal price, BigDecimal originalPrice,
                                   String color, String accent, String pattern, String tag, String zone, Boolean homepage,
                                   Integer sortOrder, String imageUrl, String imageUploadUrl, Boolean active) {
        static ProductResponse from(Product p) {
            String mediaUrl = p.getImageAssetId() == null ? null : "/api/media/" + p.getImageAssetId();
            return new ProductResponse(p.getId(), p.getName(), p.getSubtitle(), p.getPrice(), p.getOriginalPrice(),
                    p.getColor(), p.getAccent(), p.getPattern(), p.getTag(), p.getZone(), p.getHomepage(), p.getSortOrder(),
                    mediaUrl != null ? mediaUrl : p.getImageUrl(),
                    mediaUrl, p.getActive());
        }
    }
}
