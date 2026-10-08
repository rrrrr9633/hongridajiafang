package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.ReviewImage;

import java.util.List;
import java.util.UUID;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, UUID> {
    List<ReviewImage> findByReviewIdInOrderByReviewIdAscSortOrderAsc(List<UUID> reviewIds);
    List<ReviewImage> findByReviewIdOrderBySortOrderAsc(UUID reviewId);
}
