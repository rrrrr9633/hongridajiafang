package top.sama.haode.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
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
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;
    private final MediaStorage mediaStorage;

    public AdminProductController(
            ProductRepository productRepository,
            ProductTypeRepository productTypeRepository,
            MediaStorage mediaStorage
    ) {
        this.productRepository = productRepository;
        this.productTypeRepository = productTypeRepository;
        this.mediaStorage = mediaStorage;
    }

    @GetMapping
    public List<ProductResponse> list() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) return List.of();
        Map<String, List<ProductType>> types = productTypeRepository.findByProductIdInOrderBySortOrderAscIdAsc(
                products.stream().map(Product::getId).toList()
        ).stream().collect(Collectors.groupingBy(ProductType::getProductId));
        return products.stream()
                .map(product -> ProductResponse.from(product, types.getOrDefault(product.getId(), List.of())))
                .toList();
    }

    @PostMapping
    @Transactional
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        Product product = new Product(
                UUID.randomUUID().toString(), request.name(), request.subtitle(), request.price(), request.originalPrice(),
                request.color(), request.accent(), request.pattern(), request.tag(), request.zone(),
                Boolean.TRUE.equals(request.homepage()), request.sortOrder() == null ? 0 : request.sortOrder(),
                request.imageUrl(), request.active());
        productRepository.save(product);
        return ProductResponse.from(product, productTypeRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()));
    }

    @PutMapping("/{id}")
    @Transactional
    public ProductResponse update(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        product.update(request.name(), request.subtitle(), request.price(), request.originalPrice(),
                request.color(), request.accent(), request.pattern(), request.tag(), request.zone(),
                Boolean.TRUE.equals(request.homepage()), request.sortOrder() == null ? 0 : request.sortOrder(),
                request.imageUrl(), request.active());
        productRepository.save(product);
        return ProductResponse.from(product, productTypeRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()));
    }

    @PostMapping("/{id}/image")
    public ProductResponse uploadImage(@PathVariable String id, @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        product.setImageAssetId(mediaStorage.store(file).getId());
        productRepository.save(product);
        return ProductResponse.from(product, productTypeRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()));
    }

    @PostMapping("/{id}/types")
    @Transactional
    public TypeResponse createType(@PathVariable String id, @Valid @RequestBody TypeRequest request) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        int sortOrder = request.sortOrder() == null
                ? productTypeRepository.findByProductIdOrderBySortOrderAscIdAsc(id).size()
                : request.sortOrder();
        ProductType type = new ProductType(
                UUID.randomUUID().toString(),
                product.getId(),
                request.name(),
                request.intro(),
                request.imageUrl(),
                sortOrder,
                request.active() == null || request.active()
        );
        return TypeResponse.from(productTypeRepository.save(type));
    }

    @PutMapping("/{id}/types/{typeId}")
    @Transactional
    public TypeResponse updateType(@PathVariable String id, @PathVariable String typeId, @Valid @RequestBody TypeRequest request) {
        ProductType type = productTypeRepository.findById(typeId)
                .filter(item -> id.equals(item.getProductId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        int sortOrder = request.sortOrder() == null ? type.getSortOrder() : request.sortOrder();
        type.update(request.name(), request.intro(), request.imageUrl() == null ? type.getImageUrl() : request.imageUrl(),
                sortOrder, request.active() == null || request.active());
        return TypeResponse.from(productTypeRepository.save(type));
    }

    @DeleteMapping("/{id}/types/{typeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteType(@PathVariable String id, @PathVariable String typeId) {
        ProductType type = productTypeRepository.findById(typeId)
                .filter(item -> id.equals(item.getProductId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        productTypeRepository.delete(type);
    }

    @PostMapping("/{id}/types/{typeId}/image")
    public ProductResponse uploadTypeImage(
            @PathVariable String id,
            @PathVariable String typeId,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file
    ) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ProductType type = productTypeRepository.findById(typeId)
                .filter(item -> id.equals(item.getProductId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        type.setImageAssetId(mediaStorage.store(file).getId());
        productTypeRepository.save(type);
        return ProductResponse.from(product, productTypeRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        product.deactivate();
        productRepository.save(product);
    }

    public record TypeRequest(@NotBlank String name, String intro, String imageUrl, Integer sortOrder, Boolean active) {}

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

    public record TypeResponse(String id, String name, String intro, String imageUrl, String imageUploadUrl, Integer sortOrder, Boolean active) {
        static TypeResponse from(ProductType type) {
            String mediaUrl = type.getImageAssetId() == null ? null : "/api/media/" + type.getImageAssetId();
            return new TypeResponse(
                    type.getId(),
                    type.getName(),
                    type.getIntro(),
                    mediaUrl != null ? mediaUrl : type.getImageUrl(),
                    mediaUrl,
                    type.getSortOrder(),
                    type.getActive()
            );
        }
    }

    public record ProductResponse(String id, String name, String subtitle, BigDecimal price, BigDecimal originalPrice,
                                   String color, String accent, String pattern, String tag, String zone, Boolean homepage,
                                   Integer sortOrder, String imageUrl, String imageUploadUrl, Boolean active,
                                   List<TypeResponse> types) {
        static ProductResponse from(Product p, List<ProductType> types) {
            String mediaUrl = p.getImageAssetId() == null ? null : "/api/media/" + p.getImageAssetId();
            String cover = mediaUrl != null ? mediaUrl : p.getImageUrl();
            List<TypeResponse> typeResponses = types.stream().map(TypeResponse::from).toList();
            if (cover == null && !typeResponses.isEmpty()) cover = typeResponses.get(0).imageUrl();
            return new ProductResponse(p.getId(), p.getName(), p.getSubtitle(), p.getPrice(), p.getOriginalPrice(),
                    p.getColor(), p.getAccent(), p.getPattern(), p.getTag(), p.getZone(), p.getHomepage(), p.getSortOrder(),
                    cover, mediaUrl, p.getActive(), typeResponses);
        }
    }
}
