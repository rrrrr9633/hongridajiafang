package top.sama.haode.order.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.OrderApplicationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders/checkouts")
public class CheckoutPaymentController {
    private final OrderApplicationService applicationService;

    public CheckoutPaymentController(OrderApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/{checkoutId}/payments/wechat/prepay")
    public void createPrepay(@RequestAttribute("userId") String userId, @PathVariable UUID checkoutId) {
        try {
            applicationService.markCheckoutPaying(checkoutId, userId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "微信预支付服务尚未配置");
    }
}
