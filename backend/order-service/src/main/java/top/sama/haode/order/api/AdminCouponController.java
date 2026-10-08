package top.sama.haode.order.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.CouponService;
import top.sama.haode.order.domain.CouponPolicy;
import top.sama.haode.order.domain.CouponTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {
    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/templates")
    public List<TemplateResponse> templates() {
        return couponService.listTemplates().stream().map(TemplateResponse::from).toList();
    }

    @PostMapping("/templates")
    public TemplateResponse create(@RequestBody TemplateRequest request) {
        try {
            return TemplateResponse.from(couponService.createTemplate(
                    request == null ? null : request.name(),
                    request == null ? null : request.thresholdAmount(),
                    request == null ? null : request.discountAmount(),
                    request == null || request.active() == null || request.active(),
                    request == null || request.perOrderLimit() == null ? 1 : request.perOrderLimit()
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @PutMapping("/templates/{id}")
    public TemplateResponse update(@PathVariable UUID id, @RequestBody TemplateRequest request) {
        try {
            return TemplateResponse.from(couponService.updateTemplate(
                    id,
                    request == null ? null : request.name(),
                    request == null ? null : request.thresholdAmount(),
                    request == null ? null : request.discountAmount(),
                    request == null || request.active() == null || request.active(),
                    request == null || request.perOrderLimit() == null ? 1 : request.perOrderLimit()
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/policy")
    public PolicyResponse policy() {
        return PolicyResponse.from(couponService.policy());
    }

    @PutMapping("/policy")
    public PolicyResponse updatePolicy(@RequestBody PolicyRequest request) {
        try {
            return PolicyResponse.from(couponService.updatePolicy(
                    request != null && Boolean.TRUE.equals(request.enabled()),
                    request == null ? null : request.inviteTemplateId(),
                    request == null ? null : request.purchaseTemplateId(),
                    request == null || request.purchaseThreshold() == null ? BigDecimal.ZERO : request.purchaseThreshold()
            ));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    public record TemplateRequest(String name, BigDecimal thresholdAmount, BigDecimal discountAmount, Boolean active, Integer perOrderLimit) {}
    public record PolicyRequest(Boolean enabled, UUID inviteTemplateId, UUID purchaseTemplateId, BigDecimal purchaseThreshold) {}

    public record TemplateResponse(
            UUID id,
            String name,
            BigDecimal thresholdAmount,
            BigDecimal discountAmount,
            int perOrderLimit,
            boolean active,
            Instant createdAt
    ) {
        static TemplateResponse from(CouponTemplate template) {
            return new TemplateResponse(
                    template.getId(),
                    template.getName(),
                    template.getThresholdAmount(),
                    template.getDiscountAmount(),
                    template.getPerOrderLimit(),
                    template.isActive(),
                    template.getCreatedAt()
            );
        }
    }

    public record PolicyResponse(boolean enabled, UUID inviteTemplateId, UUID purchaseTemplateId, BigDecimal purchaseThreshold) {
        static PolicyResponse from(CouponPolicy policy) {
            return new PolicyResponse(policy.isEnabled(), policy.getInviteTemplateId(), policy.getPurchaseTemplateId(), policy.getPurchaseThreshold());
        }
    }
}
