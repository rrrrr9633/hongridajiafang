package top.sama.haode.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.OrderApplicationService;
import top.sama.haode.order.domain.MediaAsset;
import top.sama.haode.order.domain.Review;
import top.sama.haode.order.domain.ReviewImage;
import top.sama.haode.order.repository.MediaAssetRepository;
import top.sama.haode.order.repository.ReviewImageRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final OrderApplicationService orderService;
    private final ReviewImageRepository reviewImageRepository;
    private final MediaAssetRepository mediaAssetRepository;

    public ReviewController(OrderApplicationService orderService, ReviewImageRepository reviewImageRepository,
                            MediaAssetRepository mediaAssetRepository) {
        this.orderService = orderService;
        this.reviewImageRepository = reviewImageRepository;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    @GetMapping("/products/{productId}")
    public List<ReviewResponse> listProductReviews(@PathVariable String productId) {
        return response(orderService.listProductReviews(productId));
    }

    @GetMapping
    public List<ReviewResponse> list(@RequestAttribute("userId") String userId) {
        return response(orderService.listReviews(userId));
    }

    @PostMapping("/orders/{orderId}")
    public ReviewResponse create(@RequestAttribute("userId") String userId, @PathVariable UUID orderId,
                                 @Valid @RequestBody ReviewRequest request) {
        try {
            Review review = orderService.createReview(orderId, userId, request.rating(), request.content());
            List<UUID> imageIds = request.imageAssetIds() == null ? List.of() : request.imageAssetIds();
            if (imageIds.size() > 6 || imageIds.stream().distinct().count() != imageIds.size()) {
                throw new IllegalStateException("评价图片最多 6 张，且不能重复");
            }
            for (int index = 0; index < imageIds.size(); index++) {
                MediaAsset asset = mediaAssetRepository.findById(imageIds.get(index))
                        .filter(value -> userId.equals(value.getOwnerId()))
                        .orElseThrow(() -> new IllegalArgumentException("评价图片无效"));
                reviewImageRepository.save(new ReviewImage(review.getId(), asset.getId(), index));
            }
            return response(List.of(review)).getFirst();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    private List<ReviewResponse> response(List<Review> reviews) {
        if (reviews.isEmpty()) return List.of();
        return reviews.stream().map(review -> new ReviewResponse(review.getId(), review.getOrderId(), review.getRating(),
                review.getContent(), review.getStatus().name(), review.getCreatedAt(),
                reviewImageRepository.findByReviewIdOrderBySortOrderAsc(review.getId()).stream()
                        .map(image -> "/api/media/" + image.getAssetId()).toList())).toList();
    }

    public record ReviewRequest(@Min(1) @Max(5) int rating, @NotBlank @Size(max = 1000) String content,
                                List<UUID> imageAssetIds) {}
    public record ReviewResponse(UUID id, UUID orderId, int rating, String content, String status,
                                 Instant createdAt, List<String> images) {}
}
