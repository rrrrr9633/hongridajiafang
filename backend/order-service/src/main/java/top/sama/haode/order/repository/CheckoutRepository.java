package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.sama.haode.order.domain.Checkout;
import top.sama.haode.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckoutRepository extends JpaRepository<Checkout, UUID> {
    Optional<Checkout> findByIdAndUserId(UUID id, String userId);

    List<Checkout> findByStatusAndCreatedAtBefore(OrderStatus status, Instant createdAt);

    @Query("select coalesce(sum(c.amount), 0) from Checkout c where c.userId = :userId and c.status = :status")
    BigDecimal sumAmountByUserIdAndStatus(@Param("userId") String userId, @Param("status") OrderStatus status);
}
