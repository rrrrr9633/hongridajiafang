package top.sama.haode.order.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sama.haode.order.application.OrderApplicationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class PaymentController {
    private final OrderApplicationService applicationService;

    public PaymentController(OrderApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/{orderId}/payments/wechat/prepay")
    public WechatPaymentResponse createWechatPrepay(
            @RequestAttribute("userId") String userId,
            @PathVariable UUID orderId
    ) {
        applicationService.markPaying(orderId, userId);
        throw new UnsupportedOperationException("微信预支付服务尚未配置");
    }

    @PostMapping("/{orderId}/payments/wechat/notify")
    public ResponseEntity<Void> receiveWechatNotify(@PathVariable UUID orderId) {
        throw new UnsupportedOperationException("微信支付回调验签服务尚未配置");
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
