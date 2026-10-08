package top.sama.haode.order.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sama.haode.order.domain.Product;
import top.sama.haode.order.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public record ProductResponse(
            String id,
            String name,
            String subtitle,
            BigDecimal price,
            BigDecimal originalPrice,
            String color,
            String accent,
            String pattern,
            String tag,
            String zone,
            Boolean homepage,
            Integer sortOrder,
            String imageUrl,
            String imageUploadUrl
    ) {
        static ProductResponse from(Product product) {
            return new ProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getSubtitle(),
                    product.getPrice(),
                    product.getOriginalPrice(),
                    product.getColor(),
                    product.getAccent(),
                    product.getPattern(),
                    product.getTag(),
                    product.getZone(),
                    product.getHomepage(),
                    product.getSortOrder(),
                    product.getImageAssetId() == null ? product.getImageUrl() : "/api/media/" + product.getImageAssetId(),
                    product.getImageAssetId() == null ? null : "/api/media/" + product.getImageAssetId()
            );
        }
    }
}
