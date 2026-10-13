package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.sama.haode.order.domain.CouponTemplateProduct;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CouponTemplateProductRepository extends JpaRepository<CouponTemplateProduct, CouponTemplateProduct.Key> {
    @Query("select row from CouponTemplateProduct row where row.id.templateId = :templateId")
    List<CouponTemplateProduct> findByTemplateId(@Param("templateId") UUID templateId);

    @Query("select row from CouponTemplateProduct row where row.id.templateId in :templateIds")
    List<CouponTemplateProduct> findByTemplateIds(@Param("templateIds") Collection<UUID> templateIds);

    @Modifying
    @Query("delete from CouponTemplateProduct row where row.id.templateId = :templateId")
    void deleteByTemplateId(@Param("templateId") UUID templateId);
}
