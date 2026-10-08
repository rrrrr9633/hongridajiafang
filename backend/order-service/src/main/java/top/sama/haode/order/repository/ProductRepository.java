package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByActiveTrueOrderBySortOrderAscIdAsc();

    Optional<Product> findByIdAndActiveTrue(String id);
}
