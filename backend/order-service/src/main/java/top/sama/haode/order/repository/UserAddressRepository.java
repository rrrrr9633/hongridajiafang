package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.UserAddress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {
    List<UserAddress> findByUserIdOrderByDefaultAddressDescCreatedAtDesc(String userId);

    Optional<UserAddress> findByIdAndUserId(UUID id, String userId);

    Optional<UserAddress> findFirstByUserIdAndDefaultAddressTrue(String userId);
}
