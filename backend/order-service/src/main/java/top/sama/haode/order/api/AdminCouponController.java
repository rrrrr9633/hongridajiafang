package top.sama.haode.order.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.CouponService;
import top.sama.haode.order.application.CouponUsageRules;
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
    private final CouponUsageRules usageRules;

    public AdminCouponController(CouponService couponService, CouponUsageRules usageRules) {
        this.couponService = couponService;
        this.usageRules = usageRules;
    }

    @GetMapping("/templates")
    public List<TemplateResponse> templates() {
        return couponService.listTemplates().stream().map(template -> TemplateResponse.from(template, couponService)).toList();
    }

    @PostMapping("/templates")
    public TemplateResponse create(@RequestBody TemplateRequest request) {
        try {
            return TemplateResponse.from(couponService.createTemplate(
                    request == null ? null : request.name(),
                    request == null ? null : request.thresholdAmount(),
                    request == null ? null : request.discountAmount(),
                    request == null || request.active() == null || request.active()
            ), couponService);
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
                    request == null || request.active() == null || request.active()
            ), couponService);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/rules")
    public List<CouponUsageRules.RuleView> rules() {
        return usageRules.list();
    }

    @PostMapping("/rules")
    public CouponUsageRules.RuleView createRule(@RequestBody RuleRequest request) {
        return saveRule(null, request);
    }

    @PutMapping("/rules/{id}")
    public CouponUsageRules.RuleView updateRule(@PathVariable UUID id, @RequestBody RuleRequest request) {
        return saveRule(id, request);
    }

    @DeleteMapping("/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable UUID id) {
        try {
            usageRules.delete(id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private CouponUsageRules.RuleView saveRule(UUID id, RuleRequest request) {
        try {
            return usageRules.save(
                    id,
                    request == null ? null : request.name(),
                    request == null ? null : request.priority(),
                    request == null ? List.of() : request.zones(),
                    request == null ? List.of() : request.productIds(),
                    request == null || request.options() == null ? List.of() : request.options().stream()
                            .map(option -> new CouponUsageRules.OptionDraft(
                                    option == null || option.items() == null ? List.of() : option.items().stream()
                                            .map(item -> new CouponUsageRules.ItemDraft(item == null ? null : item.templateId(), item == null ? 0 : item.quantity()))
                                            .toList()
                            ))
                            .toList()
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    public record RuleItemRequest(UUID templateId, int quantity) {}
    public record RuleOptionRequest(List<RuleItemRequest> items) {}
    public record RuleRequest(String name, Integer priority, List<String> zones, List<String> productIds, List<RuleOptionRequest> options) {}

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

    public record TemplateRequest(String name, BigDecimal thresholdAmount, BigDecimal discountAmount, Boolean active, Integer perOrderLimit, List<String> productIds) {}
    public record PolicyRequest(Boolean enabled, UUID inviteTemplateId, UUID purchaseTemplateId, BigDecimal purchaseThreshold) {}

    public record TemplateResponse(
            UUID id,
            String name,
            BigDecimal thresholdAmount,
            BigDecimal discountAmount,
            int perOrderLimit,
            boolean active,
            List<String> productIds,
            String scopeText,
            Instant createdAt
    ) {
        static TemplateResponse from(CouponTemplate template, CouponService couponService) {
            List<String> productIds = couponService.productIds(template.getId());
            return new TemplateResponse(
                    template.getId(),
                    template.getName(),
                    template.getThresholdAmount(),
                    template.getDiscountAmount(),
                    template.getPerOrderLimit(),
                    template.isActive(),
                    productIds,
                    couponService.scopeText(template.getId()),
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
