package top.sama.haode.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
import top.sama.haode.order.application.CouponService;
import top.sama.haode.order.application.OrderApplicationService;
import top.sama.haode.order.domain.Checkout;
import top.sama.haode.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderApplicationService applicationService;
    private final CouponService couponService;

    public OrderController(OrderApplicationService applicationService, CouponService couponService) {
        this.applicationService = applicationService;
        this.couponService = couponService;
    }

    @PostMapping
    public OrderResponse create(@RequestAttribute("userId") String userId, @Valid @RequestBody CreateOrderRequest request) {
        return OrderResponse.from(applicationService.createOrder(userId, request.productId(), request.typeId(), request.quantity()));
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@RequestAttribute("userId") String userId, @Valid @RequestBody CreateCheckoutRequest request) {
        try {
            var result = applicationService.createCheckout(userId, request.items().stream()
                    .map(item -> new OrderApplicationService.CheckoutItem(item.productId(), item.typeId(), item.quantity())).toList(),
                    request.couponId());
            return toCheckout(userId, result.checkout(), result.orders());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @GetMapping("/checkouts/{checkoutId}")
    public CheckoutResponse getCheckout(@RequestAttribute("userId") String userId, @PathVariable UUID checkoutId) {
        try {
            var result = applicationService.getCheckout(checkoutId, userId);
            return toCheckout(userId, result.checkout(), result.orders());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @PostMapping("/checkouts/{checkoutId}/cancel")
    public CheckoutResponse cancelCheckout(@RequestAttribute("userId") String userId, @PathVariable UUID checkoutId) {
        try {
            var checkout = applicationService.cancelCheckout(checkoutId, userId);
            var result = applicationService.listCheckoutOrders(checkoutId, userId);
            return toCheckout(userId, checkout, result.orders());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @PutMapping("/checkouts/{checkoutId}/coupon")
    public CheckoutResponse applyCoupon(
            @RequestAttribute("userId") String userId,
            @PathVariable UUID checkoutId,
            @RequestBody(required = false) ApplyCouponRequest request
    ) {
        try {
            var result = applicationService.applyCheckoutCoupon(
                    checkoutId,
                    userId,
                    request == null ? null : request.couponId(),
                    request == null || request.selections() == null
                            ? null
                            : request.selections().stream()
                            .map(item -> new CouponService.Selection(item.templateId(), item.quantity()))
                            .toList()
            );
            return toCheckout(userId, result.checkout(), result.orders());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @PutMapping("/checkouts/{checkoutId}/address")
    public CheckoutResponse bindAddress(
            @RequestAttribute("userId") String userId,
            @PathVariable UUID checkoutId,
            @RequestBody AddressBindRequest request
    ) {
        try {
            var result = applicationService.bindCheckoutAddress(checkoutId, userId, request == null ? null : request.addressId());
            return toCheckout(userId, result.checkout(), result.orders());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @GetMapping
    public List<OrderResponse> list(@RequestAttribute("userId") String userId) {
        return applicationService.listOrders(userId).stream().map(OrderResponse::from).toList();
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@RequestAttribute("userId") String userId, @PathVariable UUID orderId) {
        try {
            return OrderResponse.from(applicationService.cancelOrder(orderId, userId));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @DeleteMapping("/{orderId}")
    public void delete(@RequestAttribute("userId") String userId, @PathVariable UUID orderId) {
        try {
            applicationService.deleteOrder(orderId, userId);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@RequestAttribute("userId") String userId, @PathVariable UUID orderId) {
        try {
            return OrderResponse.from(applicationService.getOrder(orderId, userId));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    public record CreateOrderRequest(@NotBlank String productId, String typeId, @Min(1) Integer quantity) {}
    public record CreateCheckoutRequest(@NotEmpty List<@Valid CheckoutItemRequest> items, UUID couponId) {}
    public record ApplyCouponRequest(UUID couponId, List<CouponSelectionRequest> selections) {}
    public record AddressBindRequest(UUID addressId) {}
    public record CouponSelectionRequest(UUID templateId, int quantity) {}
    public record CheckoutItemRequest(@NotBlank String productId, String typeId, @Min(1) Integer quantity) {}
    public record CouponOption(
            UUID templateId,
            String name,
            BigDecimal thresholdAmount,
            BigDecimal discountAmount,
            int perOrderLimit,
            int availableCount,
            int selectedCount
    ) {
        static CouponOption from(CouponService.CouponGroup group) {
            return new CouponOption(
                    group.templateId(),
                    group.name(),
                    group.thresholdAmount(),
                    group.discountAmount(),
                    group.perOrderLimit(),
                    group.availableCount(),
                    group.selectedCount()
            );
        }
    }
    public record CheckoutResponse(
            UUID id,
            BigDecimal amount,
            BigDecimal goodsAmount,
            BigDecimal discountAmount,
            UUID couponId,
            String couponName,
            String status,
            AddressView address,
            List<OrderResponse> orders,
            List<CouponOption> availableCoupons
    ) {}

    public record AddressView(
            UUID id,
            String receiverName,
            String phone,
            String province,
            String city,
            String district,
            String detail,
            String line
    ) {
        static AddressView from(Checkout checkout) {
            if (checkout == null || !checkout.hasReceiver()) return null;
            String line = (checkout.getReceiverProvince() == null ? "" : checkout.getReceiverProvince())
                    + (checkout.getReceiverCity() == null ? "" : checkout.getReceiverCity())
                    + (checkout.getReceiverDistrict() == null ? "" : checkout.getReceiverDistrict())
                    + (checkout.getReceiverDetail() == null ? "" : checkout.getReceiverDetail());
            return new AddressView(
                    checkout.getAddressId(),
                    checkout.getReceiverName(),
                    checkout.getReceiverPhone(),
                    checkout.getReceiverProvince(),
                    checkout.getReceiverCity(),
                    checkout.getReceiverDistrict(),
                    checkout.getReceiverDetail(),
                    line
            );
        }
    }

    private CheckoutResponse toCheckout(String userId, Checkout checkout, List<Order> orders) {
        return new CheckoutResponse(
                checkout.getId(),
                checkout.getAmount(),
                checkout.getGoodsAmount(),
                checkout.getDiscountAmount(),
                checkout.getCouponId(),
                couponService.couponSummary(checkout.getId()),
                checkout.getStatus().name(),
                AddressView.from(checkout),
                orders.stream().map(OrderResponse::from).toList(),
                couponService.availableGroups(userId, checkout.getGoodsAmount(), checkout.getId()).stream().map(CouponOption::from).toList()
        );
    }

    public record OrderResponse(UUID id, String productId, String typeId, String typeName, String title, String subtitle, BigDecimal amount,
                                String status, Instant createdAt, Integer quantity, UUID checkoutId) {
        static OrderResponse from(Order order) {
            return new OrderResponse(order.getId(), order.getProductId(), order.getProductTypeId(), order.getProductTypeName(),
                    order.getProductName(), order.getProductSubtitle(), order.getAmount(), order.getStatus().name(),
                    order.getCreatedAt(), order.getQuantity(), order.getCheckoutId());
        }
    }
}
