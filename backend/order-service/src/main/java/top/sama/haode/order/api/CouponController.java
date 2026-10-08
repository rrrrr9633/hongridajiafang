package top.sama.haode.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.CouponService;
import top.sama.haode.order.domain.CouponPolicy;
import top.sama.haode.order.domain.CouponTemplate;
import top.sama.haode.order.domain.UserCoupon;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {
    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    public WalletResponse mine(@RequestAttribute("userId") String userId) {
        return WalletResponse.from(couponService.wallet(userId), couponService);
    }

    @PostMapping("/invite")
    public WalletResponse invite(@RequestAttribute("userId") String userId, @Valid @RequestBody InviteRequest request) {
        try {
            return WalletResponse.from(couponService.bindInvite(userId, request.code()), couponService);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    public record InviteRequest(@NotBlank String code) {}

    public record CouponView(
            UUID id,
            UUID templateId,
            String name,
            BigDecimal thresholdAmount,
            BigDecimal discountAmount,
            String source,
            String status,
            Instant createdAt
    ) {
        static CouponView from(UserCoupon coupon) {
            return new CouponView(
                    coupon.getId(),
                    coupon.getTemplateId(),
                    coupon.getName(),
                    coupon.getThresholdAmount(),
                    coupon.getDiscountAmount(),
                    coupon.getSource().name(),
                    coupon.getStatus().name(),
                    coupon.getCreatedAt()
            );
        }
    }

    public record TemplateView(UUID id, String name, BigDecimal thresholdAmount, BigDecimal discountAmount) {
        static TemplateView from(CouponTemplate template) {
            if (template == null) return null;
            return new TemplateView(template.getId(), template.getName(), template.getThresholdAmount(), template.getDiscountAmount());
        }
    }

    public record PolicyView(boolean enabled, TemplateView inviteCoupon, TemplateView purchaseCoupon, BigDecimal purchaseThreshold) {
        static PolicyView from(CouponPolicy policy, CouponService couponService) {
            return new PolicyView(
                    policy.isEnabled(),
                    templateOrNull(couponService, policy.getInviteTemplateId()),
                    templateOrNull(couponService, policy.getPurchaseTemplateId()),
                    policy.getPurchaseThreshold()
            );
        }

        private static TemplateView templateOrNull(CouponService couponService, UUID id) {
            if (id == null) return null;
            try {
                return TemplateView.from(couponService.requireTemplate(id));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    public record WalletResponse(
            String inviteCode,
            boolean inviterBound,
            String inviterCode,
            PolicyView policy,
            List<CouponView> coupons
    ) {
        static WalletResponse from(CouponService.Wallet wallet, CouponService couponService) {
            return new WalletResponse(
                    wallet.user().getInviteCode(),
                    wallet.user().hasInviter(),
                    wallet.inviter() == null ? null : wallet.inviter().getInviteCode(),
                    PolicyView.from(wallet.policy(), couponService),
                    wallet.coupons().stream().map(CouponView::from).toList()
            );
        }
    }
}
