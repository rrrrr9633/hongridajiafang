package top.sama.haode.order.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sama.haode.order.domain.Product;
import top.sama.haode.order.domain.ProductType;
import top.sama.haode.order.repository.ProductRepository;
import top.sama.haode.order.repository.ProductTypeRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;

    public ProductController(ProductRepository productRepository, ProductTypeRepository productTypeRepository) {
        this.productRepository = productRepository;
        this.productTypeRepository = productTypeRepository;
    }

    @GetMapping
    public List<ProductResponse> list() {
        List<Product> products = productRepository.findByActiveTrueOrderBySortOrderAscIdAsc();
        if (products.isEmpty()) return List.of();
        Map<String, List<ProductType>> types = productTypeRepository.findByProductIdInOrderBySortOrderAscIdAsc(
                products.stream().map(Product::getId).toList()
        ).stream().collect(Collectors.groupingBy(ProductType::getProductId));
        return products.stream()
                .map(product -> ProductResponse.from(product, types.getOrDefault(product.getId(), List.of())))
                .toList();
    }

    public record TypeResponse(String id, String name, String intro, String imageUrl, Integer sortOrder) {
        static TypeResponse from(ProductType type) {
            return new TypeResponse(
                    type.getId(),
                    type.getName(),
                    type.getIntro(),
                    imageOf(type.getImageAssetId(), type.getImageUrl()),
                    type.getSortOrder()
            );
        }
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
            String imageUploadUrl,
            List<TypeResponse> types
    ) {
        static ProductResponse from(Product product, List<ProductType> types) {
            List<ProductType> visible = types.stream().filter(type -> Boolean.TRUE.equals(type.getActive())).toList();
            String cover = imageOf(product.getImageAssetId(), product.getImageUrl());
            if (cover == null && !visible.isEmpty()) {
                ProductType first = visible.get(0);
                cover = imageOf(first.getImageAssetId(), first.getImageUrl());
            }
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
                    cover,
                    product.getImageAssetId() == null ? null : "/api/media/" + product.getImageAssetId(),
                    visible.stream().map(TypeResponse::from).toList()
            );
        }
    }

    private static String imageOf(UUID assetId, String imageUrl) {
        return assetId == null ? imageUrl : "/api/media/" + assetId;
    }
}
