package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.CouponUsageRule;

import java.util.List;
import java.util.UUID;

public interface CouponUsageRuleRepository extends JpaRepository<CouponUsageRule, UUID> {
    List<CouponUsageRule> findAllByOrderByPriorityAscCreatedAtAsc();
}
