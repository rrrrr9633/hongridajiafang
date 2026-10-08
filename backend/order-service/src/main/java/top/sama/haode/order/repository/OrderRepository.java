package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import top.sama.haode.order.domain.Order;
import top.sama.haode.order.domain.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Order> findByCheckoutIdOrderByCreatedAtAsc(UUID checkoutId);
    void deleteByCheckoutId(UUID checkoutId);

    List<Order> findByProductIdAndStatusInOrderByCreatedAtDesc(String productId, List<OrderStatus> statuses);

    Optional<Order> findByIdAndUserId(UUID id, String userId);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant createdAt);
}
