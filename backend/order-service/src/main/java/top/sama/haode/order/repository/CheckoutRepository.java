package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.Checkout;

import java.util.Optional;
import java.util.UUID;

public interface CheckoutRepository extends JpaRepository<Checkout, UUID> {
    Optional<Checkout> findByIdAndUserId(UUID id, String userId);
    java.util.List<Checkout> findByStatusAndCreatedAtBefore(top.sama.haode.order.domain.OrderStatus status, java.time.Instant createdAt);
}
