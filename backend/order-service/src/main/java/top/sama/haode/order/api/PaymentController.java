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
@RequestMapping("/api/orders")
public class PaymentController {
    private final OrderApplicationService applicationService;
    private final WechatPayService wechatPayService;
    private final UserRepository users;

    public PaymentController(
            OrderApplicationService applicationService,
            WechatPayService wechatPayService,
            UserRepository users
    ) {
        this.applicationService = applicationService;
        this.wechatPayService = wechatPayService;
        this.users = users;
    }

    @PostMapping("/{orderId}/payments/wechat/prepay")
    public WechatPaymentResponse createWechatPrepay(
            @RequestAttribute("userId") String userId,
            @PathVariable UUID orderId
    ) {
        try {
            Order order = applicationService.getOrderForPayment(orderId, userId);
            User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
            if (order.getCheckoutId() != null) {
                Checkout checkout = applicationService.markCheckoutPaying(order.getCheckoutId(), userId);
                return wechatPayService.prepay(checkout.getId(), checkout.getAmount(), user.getWechatOpenid(), description(order));
            }
            applicationService.markPaying(orderId, userId);
            return wechatPayService.prepay(order.getId(), order.getAmount(), user.getWechatOpenid(), description(order));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
        }
    }

    @PostMapping("/{orderId}/payments/wechat/sync")
    public void syncWechatPayment(
            @RequestAttribute("userId") String userId,
            @PathVariable UUID orderId
    ) {
        try {
            applicationService.syncOrderPayment(orderId, userId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    private static String description(Order order) {
        return order.getProductTypeName() == null || order.getProductTypeName().isBlank()
                ? order.getProductName()
                : order.getProductName() + " · " + order.getProductTypeName();
    }

    public record WechatPaymentResponse(
            String timeStamp,
            String nonceStr,
            String packageValue,
            String signType,
            String paySign
    ) {
    }
}
