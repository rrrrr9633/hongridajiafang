package top.sama.haode.order.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sama.haode.order.application.OrderApplicationService;
import top.sama.haode.order.service.WechatPayService;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/wechat")
public class WechatPayNotifyController {
    private final WechatPayService wechatPayService;
    private final OrderApplicationService applicationService;

    public WechatPayNotifyController(WechatPayService wechatPayService, OrderApplicationService applicationService) {
        this.wechatPayService = wechatPayService;
        this.applicationService = applicationService;
    }

    @PostMapping("/notify")
    public ResponseEntity<Map<String, String>> notify(
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
            @RequestBody String body
    ) {
        try {
            wechatPayService.verifyNotify(timestamp, nonce, body, signature, serial);
            WechatPayService.NotifyResult result = wechatPayService.parseNotify(body);
            if ("SUCCESS".equals(result.tradeState())) {
                applicationService.confirmPaidByOutTradeNo(result.outTradeNo(), result.transactionId());
            }
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "code", "FAIL",
                    "message", exception.getMessage() == null ? "处理失败" : exception.getMessage()
            ));
        }
    }

    @PostMapping("/refund-notify")
    public ResponseEntity<Map<String, String>> refundNotify(
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
            @RequestBody String body
    ) {
        try {
            wechatPayService.verifyNotify(timestamp, nonce, body, signature, serial);
            WechatPayService.RefundNotifyResult result = wechatPayService.parseRefundNotify(body);
            applicationService.confirmRefund(result.outRefundNo(), result.refundId(), result.refundStatus());
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "code", "FAIL",
                    "message", exception.getMessage() == null ? "处理失败" : exception.getMessage()
            ));
        }
    }
}
