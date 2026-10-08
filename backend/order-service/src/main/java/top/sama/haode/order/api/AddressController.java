package top.sama.haode.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.AddressService;
import top.sama.haode.order.domain.UserAddress;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public List<AddressResponse> list(@RequestAttribute("userId") String userId) {
        return addressService.list(userId).stream().map(AddressResponse::from).toList();
    }

    @PostMapping
    public AddressResponse create(@RequestAttribute("userId") String userId, @Valid @RequestBody AddressRequest request) {
        try {
            return AddressResponse.from(addressService.save(
                    userId, null, request.receiverName(), request.phone(),
                    request.province(), request.city(), request.district(), request.detail(),
                    Boolean.TRUE.equals(request.makeDefault())
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @PutMapping("/{id}")
    public AddressResponse update(
            @RequestAttribute("userId") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request
    ) {
        try {
            return AddressResponse.from(addressService.save(
                    userId, id, request.receiverName(), request.phone(),
                    request.province(), request.city(), request.district(), request.detail(),
                    Boolean.TRUE.equals(request.makeDefault())
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @PutMapping("/{id}/default")
    public AddressResponse markDefault(@RequestAttribute("userId") String userId, @PathVariable UUID id) {
        try {
            return AddressResponse.from(addressService.markDefault(userId, id));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestAttribute("userId") String userId, @PathVariable UUID id) {
        try {
            addressService.delete(userId, id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    public record AddressRequest(
            @NotBlank String receiverName,
            @NotBlank String phone,
            @NotBlank String province,
            @NotBlank String city,
            @NotBlank String district,
            @NotBlank String detail,
            Boolean makeDefault
    ) {}

    public record AddressResponse(
            UUID id,
            String receiverName,
            String phone,
            String province,
            String city,
            String district,
            String detail,
            boolean isDefault,
            String line
    ) {
        static AddressResponse from(UserAddress address) {
            return new AddressResponse(
                    address.getId(),
                    address.getReceiverName(),
                    address.getPhone(),
                    address.getProvince(),
                    address.getCity(),
                    address.getDistrict(),
                    address.getDetail(),
                    address.isDefaultAddress(),
                    address.line()
            );
        }
    }
}
