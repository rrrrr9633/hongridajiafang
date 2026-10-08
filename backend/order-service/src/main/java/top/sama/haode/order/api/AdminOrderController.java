package top.sama.haode.order.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.OrderApplicationService;
import top.sama.haode.order.domain.Order;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final OrderApplicationService orderService;

    public AdminOrderController(OrderApplicationService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable UUID orderId) {
        try {
            return OrderResponse.from(orderService.cancelOrderAsAdmin(orderId));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    public record OrderResponse(UUID id, String status, String title) {
        static OrderResponse from(Order order) {
            return new OrderResponse(order.getId(), order.getStatus().name(), order.getProductName());
        }
    }
}
