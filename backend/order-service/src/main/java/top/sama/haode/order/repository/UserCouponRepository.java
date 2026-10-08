package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.UserCoupon;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCouponRepository extends JpaRepository<UserCoupon, UUID> {
    List<UserCoupon> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<UserCoupon> findByIdAndUserId(UUID id, String userId);

    List<UserCoupon> findByCheckoutId(UUID checkoutId);

    List<UserCoupon> findByUserIdAndTemplateIdAndStatusOrderByCreatedAtAsc(String userId, UUID templateId, UserCoupon.Status status);
}
