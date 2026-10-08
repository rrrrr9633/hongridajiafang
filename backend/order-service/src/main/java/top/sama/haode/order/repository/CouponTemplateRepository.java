package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.CouponTemplate;

import java.util.List;
import java.util.UUID;

public interface CouponTemplateRepository extends JpaRepository<CouponTemplate, UUID> {
    List<CouponTemplate> findAllByOrderByCreatedAtDesc();
}
