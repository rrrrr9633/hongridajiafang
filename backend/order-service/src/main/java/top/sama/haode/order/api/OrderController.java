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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.OrderApplicationService;
import top.sama.haode.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderApplicationService applicationService;

    public OrderController(OrderApplicationService applicationService) { this.applicationService = applicationService; }

    @PostMapping
    public OrderResponse create(@RequestAttribute("userId") String userId, @Valid @RequestBody CreateOrderRequest request) {
        return OrderResponse.from(applicationService.createOrder(userId, request.productId(), request.quantity()));
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@RequestAttribute("userId") String userId, @Valid @RequestBody CreateCheckoutRequest request) {
        try {
            var result = applicationService.createCheckout(userId, request.items().stream()
                    .map(item -> new OrderApplicationService.CheckoutItem(item.productId(), item.quantity())).toList());
            return CheckoutResponse.from(result.checkout(), result.orders());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @GetMapping("/checkouts/{checkoutId}")
    public CheckoutResponse getCheckout(@RequestAttribute("userId") String userId, @PathVariable UUID checkoutId) {
        try {
            var result = applicationService.getCheckout(checkoutId, userId);
            return CheckoutResponse.from(result.checkout(), result.orders());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @PostMapping("/checkouts/{checkoutId}/cancel")
    public CheckoutResponse cancelCheckout(@RequestAttribute("userId") String userId, @PathVariable UUID checkoutId) {
        try {
            var checkout = applicationService.cancelCheckout(checkoutId, userId);
            var result = applicationService.listCheckoutOrders(checkoutId, userId);
            return CheckoutResponse.from(checkout, result.orders());
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

    public record CreateOrderRequest(@NotBlank String productId, @Min(1) Integer quantity) {}
    public record CreateCheckoutRequest(@NotEmpty List<@Valid CheckoutItemRequest> items) {}
    public record CheckoutItemRequest(@NotBlank String productId, @Min(1) Integer quantity) {}
    public record CheckoutResponse(UUID id, BigDecimal amount, String status, List<OrderResponse> orders) {
        static CheckoutResponse from(top.sama.haode.order.domain.Checkout checkout, List<Order> orders) {
            return new CheckoutResponse(checkout.getId(), checkout.getAmount(), checkout.getStatus().name(),
                    orders.stream().map(OrderResponse::from).toList());
        }
    }

    public record OrderResponse(UUID id, String productId, String title, String subtitle, BigDecimal amount,
                                String status, Instant createdAt, Integer quantity, UUID checkoutId) {
        static OrderResponse from(Order order) {
            return new OrderResponse(order.getId(), order.getProductId(), order.getProductName(),
                    order.getProductSubtitle(), order.getAmount(), order.getStatus().name(),
                    order.getCreatedAt(), order.getQuantity(), order.getCheckoutId());
        }
    }
}
