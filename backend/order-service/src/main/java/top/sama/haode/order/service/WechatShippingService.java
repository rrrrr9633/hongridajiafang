package top.sama.haode.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WechatShippingService {
    private final WechatAuthService wechatAuthService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public WechatShippingService(WechatAuthService wechatAuthService, ObjectMapper objectMapper) {
        this.wechatAuthService = wechatAuthService;
        this.objectMapper = objectMapper;
    }

    public void upload(ShippingUpload upload) {
        try {
            String token = wechatAuthService.accessToken();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("order_key", Map.of(
                    "order_number_type", 2,
                    "transaction_id", upload.transactionId()
            ));
            body.put("logistics_type", upload.logisticsType());
            body.put("delivery_mode", 1);
            body.put("shipping_list", List.of(shippingItem(upload)));
            body.put("upload_time", OffsetDateTime.now(ZoneId.of("Asia/Shanghai"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")));
            body.put("payer", Map.of("openid", upload.openid()));
            String json = objectMapper.writeValueAsString(body);
            String response = restClient.post()
                    .uri("https://api.weixin.qq.com/wxa/sec/order/upload_shipping_info?access_token={token}", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(response == null ? "{}" : response);
            int errcode = node.path("errcode").asInt(0);
            String errmsg = node.path("errmsg").asText("");
            if (errcode != 0 && !errmsg.contains("已发货") && !errmsg.contains("repeat")) {
                throw new IllegalStateException("微信发货上报失败：" + (errmsg.isBlank() ? "未知错误" : errmsg));
            }
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("微信发货上报失败：" + exception.getMessage(), exception);
        }
    }

    private static Map<String, Object> shippingItem(ShippingUpload upload) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("item_desc", upload.itemDesc());
        if (upload.logisticsType() == 1) {
            item.put("tracking_no", upload.trackingNo());
            item.put("express_company", upload.expressCompany());
            if (upload.receiverPhone() != null && upload.receiverPhone().length() >= 4) {
                item.put("contact", Map.of("receiver_contact", upload.receiverPhone().substring(upload.receiverPhone().length() - 4)));
            }
        }
        return item;
    }

    public record ShippingUpload(
            String transactionId,
            String openid,
            String itemDesc,
            int logisticsType,
            String trackingNo,
            String expressCompany,
            String receiverPhone
    ) {}
}
