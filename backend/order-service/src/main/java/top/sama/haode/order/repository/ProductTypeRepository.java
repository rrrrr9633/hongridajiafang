package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.ProductType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductTypeRepository extends JpaRepository<ProductType, String> {
    List<ProductType> findByProductIdOrderBySortOrderAscIdAsc(String productId);

    List<ProductType> findByProductIdInOrderBySortOrderAscIdAsc(Collection<String> productIds);

    List<ProductType> findByProductIdAndActiveTrueOrderBySortOrderAscIdAsc(String productId);

    Optional<ProductType> findByIdAndProductIdAndActiveTrue(String id, String productId);
}
