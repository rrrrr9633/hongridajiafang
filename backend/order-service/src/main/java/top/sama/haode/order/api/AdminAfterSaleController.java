package top.sama.haode.order.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.OrderApplicationService;
import top.sama.haode.order.domain.AfterSale;
import top.sama.haode.order.repository.AfterSaleRepository;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/after-sales")
public class AdminAfterSaleController {
    private final AfterSaleRepository afterSales;
    private final OrderApplicationService orderService;

    public AdminAfterSaleController(AfterSaleRepository afterSales, OrderApplicationService orderService) {
        this.afterSales = afterSales;
        this.orderService = orderService;
    }

    @GetMapping
    public AdminPage<AfterSaleResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int bound = Math.min(Math.max(size, 1), 50);
        return AdminPage.from(
                afterSales.findAll(PageRequest.of(Math.max(page, 0), bound, Sort.by(Sort.Direction.DESC, "createdAt"))),
                AfterSaleResponse::from
        );
    }

    @PostMapping("/{id}/approve")
    public AfterSaleResponse approve(@PathVariable UUID id) {
        try {
            return AfterSaleResponse.from(orderService.approveAfterSale(id));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @PostMapping("/{id}/reject")
    public AfterSaleResponse reject(@PathVariable UUID id) {
        try {
            return AfterSaleResponse.from(orderService.rejectAfterSale(id));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    public record AfterSaleResponse(
            UUID id,
            UUID orderId,
            String userId,
            String reason,
            String status,
            Instant createdAt
    ) {
        static AfterSaleResponse from(AfterSale item) {
            return new AfterSaleResponse(item.getId(), item.getOrderId(), item.getUserId(), item.getReason(),
                    item.getStatus().name(), item.getCreatedAt());
        }
    }
}
