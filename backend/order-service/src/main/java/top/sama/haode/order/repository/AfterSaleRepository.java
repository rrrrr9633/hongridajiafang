package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.AfterSale;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AfterSaleRepository extends JpaRepository<AfterSale, UUID> {
    List<AfterSale> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<AfterSale> findByOrderIdAndUserId(UUID orderId, String userId);
    boolean existsByOrderIdAndUserId(UUID orderId, String userId);
}
