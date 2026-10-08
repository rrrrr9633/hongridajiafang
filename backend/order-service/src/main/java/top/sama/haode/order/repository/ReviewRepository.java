package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import top.sama.haode.order.domain.Review;
import top.sama.haode.order.domain.ReviewStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID>, JpaSpecificationExecutor<Review> {
    List<Review> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Review> findByOrderIdAndUserId(UUID orderId, String userId);
    boolean existsByOrderIdAndUserId(UUID orderId, String userId);
    List<Review> findByOrderIdInAndStatusOrderByCreatedAtDesc(List<UUID> orderIds, ReviewStatus status);
    List<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status);
}
