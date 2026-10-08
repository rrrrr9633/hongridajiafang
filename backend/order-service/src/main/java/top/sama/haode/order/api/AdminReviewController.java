package top.sama.haode.order.api;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.domain.Review;
import top.sama.haode.order.domain.ReviewStatus;
import top.sama.haode.order.repository.ReviewRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {
    private final ReviewRepository reviews;

    public AdminReviewController(ReviewRepository reviews) {
        this.reviews = reviews;
    }

    @GetMapping
    public AdminPage<ReviewResponse> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String query = q == null ? "" : q.trim();
        Specification<Review> spec = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!query.isEmpty()) {
                String like = "%" + query.toLowerCase() + "%";
                List<Predicate> matches = new ArrayList<>();
                matches.add(cb.like(cb.lower(root.get("userId")), like));
                matches.add(cb.like(cb.lower(root.get("content")), like));
                try {
                    UUID id = UUID.fromString(query);
                    matches.add(cb.equal(root.get("id"), id));
                    matches.add(cb.equal(root.get("orderId"), id));
                } catch (IllegalArgumentException ignoredUuid) {
                    // 非 UUID 只搜用户和内容
                }
                predicates.add(cb.or(matches.toArray(Predicate[]::new)));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), ReviewStatus.valueOf(status)));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
        int bound = Math.min(Math.max(size, 1), 50);
        return AdminPage.from(
                reviews.findAll(spec, PageRequest.of(Math.max(page, 0), bound, Sort.by(Sort.Direction.DESC, "createdAt"))),
                ReviewResponse::from
        );
    }

    @PostMapping("/{id}/approve")
    public ReviewResponse approve(@PathVariable UUID id) {
        Review review = reviews.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        review.approve();
        return ReviewResponse.from(reviews.save(review));
    }

    @PostMapping("/{id}/reject")
    public ReviewResponse reject(@PathVariable UUID id) {
        Review review = reviews.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        review.reject();
        return ReviewResponse.from(reviews.save(review));
    }

    public record ReviewResponse(UUID id, UUID orderId, String userId, int rating, String content,
                                 String status, Instant createdAt) {
        static ReviewResponse from(Review review) {
            return new ReviewResponse(review.getId(), review.getOrderId(), review.getUserId(), review.getRating(),
                    review.getContent(), review.getStatus().name(), review.getCreatedAt());
        }
    }
}
