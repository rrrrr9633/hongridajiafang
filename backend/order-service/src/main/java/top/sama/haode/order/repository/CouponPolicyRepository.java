package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.CouponPolicy;

public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, String> {
}
