package top.sama.haode.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.OrderApplicationService;
import top.sama.haode.order.domain.AfterSale;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/after-sales")
public class AfterSaleController {
    private final OrderApplicationService orderService;

    public AfterSaleController(OrderApplicationService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<AfterSaleResponse> list(@RequestAttribute("userId") String userId) {
        return orderService.listAfterSales(userId).stream().map(AfterSaleResponse::from).toList();
    }

    @PostMapping("/orders/{orderId}")
    public AfterSaleResponse create(
            @RequestAttribute("userId") String userId,
            @PathVariable UUID orderId,
            @Valid @RequestBody AfterSaleRequest request
    ) {
        try {
            return AfterSaleResponse.from(orderService.createAfterSale(orderId, userId, request.reason()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    public record AfterSaleRequest(@NotBlank @Size(max = 500) String reason) {}
    public record AfterSaleResponse(UUID id, UUID orderId, String reason, String status, Instant createdAt) {
        static AfterSaleResponse from(AfterSale item) {
            return new AfterSaleResponse(item.getId(), item.getOrderId(), item.getReason(), item.getStatus().name(), item.getCreatedAt());
        }
    }
}
