package top.sama.haode.order.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.OrderApplicationService;
import top.sama.haode.order.domain.Checkout;
import top.sama.haode.order.domain.Order;
import top.sama.haode.order.domain.User;
import top.sama.haode.order.repository.UserRepository;
import top.sama.haode.order.service.WechatPayService;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders/checkouts")
public class CheckoutPaymentController {
    private final OrderApplicationService applicationService;
    private final WechatPayService wechatPayService;
    private final UserRepository users;

    public CheckoutPaymentController(
            OrderApplicationService applicationService,
            WechatPayService wechatPayService,
            UserRepository users
    ) {
        this.applicationService = applicationService;
        this.wechatPayService = wechatPayService;
        this.users = users;
    }

    @PostMapping("/{checkoutId}/payments/wechat/prepay")
    public PaymentController.WechatPaymentResponse createPrepay(
            @RequestAttribute("userId") String userId,
            @PathVariable UUID checkoutId
    ) {
        try {
            Checkout checkout = applicationService.markCheckoutPaying(checkoutId, userId);
            User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
            String description = applicationService.getCheckout(checkoutId, userId).orders().stream()
                    .map(Order::getProductName)
                    .findFirst()
                    .orElse("红日大家纺");
            return wechatPayService.prepay(checkout, user.getWechatOpenid(), description);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
        }
    }
}
