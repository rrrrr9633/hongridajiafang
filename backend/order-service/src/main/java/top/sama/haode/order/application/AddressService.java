package top.sama.haode.order.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sama.haode.order.domain.UserAddress;
import top.sama.haode.order.repository.UserAddressRepository;

import java.util.List;
import java.util.UUID;

@Service
public class AddressService {
    private final UserAddressRepository addresses;

    public AddressService(UserAddressRepository addresses) {
        this.addresses = addresses;
    }

    @Transactional(readOnly = true)
    public List<UserAddress> list(String userId) {
        return addresses.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public UserAddress require(String userId, UUID id) {
        return addresses.findByIdAndUserId(id, userId).orElseThrow(() -> new IllegalArgumentException("收货地址不存在"));
    }

    @Transactional(readOnly = true)
    public UserAddress defaultAddress(String userId) {
        return addresses.findFirstByUserIdAndDefaultAddressTrue(userId).orElse(null);
    }

    @Transactional
    public UserAddress save(
            String userId,
            UUID id,
            String receiverName,
            String phone,
            String province,
            String city,
            String district,
            String detail,
            boolean makeDefault
    ) {
        boolean first = addresses.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).isEmpty();
        boolean asDefault = makeDefault || first;
        UserAddress address = id == null
                ? new UserAddress(userId, receiverName, phone, province, city, district, detail, asDefault)
                : require(userId, id);
        if (id != null) address.replace(receiverName, phone, province, city, district, detail, asDefault);
        if (asDefault) {
            addresses.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).forEach(item -> {
                if (!item.getId().equals(address.getId())) item.markDefault(false);
            });
            address.markDefault(true);
        }
        return addresses.save(address);
    }

    @Transactional
    public UserAddress markDefault(String userId, UUID id) {
        UserAddress address = require(userId, id);
        addresses.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).forEach(item -> item.markDefault(item.getId().equals(id)));
        return address;
    }

    @Transactional
    public void delete(String userId, UUID id) {
        UserAddress address = require(userId, id);
        addresses.delete(address);
        if (address.isDefaultAddress()) {
            addresses.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream().findFirst()
                    .ifPresent(item -> item.markDefault(true));
        }
    }
}
