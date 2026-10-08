package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByCheckoutId(UUID checkoutId);
    void deleteByOrderId(UUID orderId);
    void deleteByCheckoutId(UUID checkoutId);
}
