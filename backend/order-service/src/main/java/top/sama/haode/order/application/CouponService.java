package top.sama.haode.order.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sama.haode.order.domain.Checkout;
import top.sama.haode.order.domain.CouponPolicy;
import top.sama.haode.order.domain.CouponTemplate;
import top.sama.haode.order.domain.InviteCodes;
import top.sama.haode.order.domain.InvitePurchaseReward;
import top.sama.haode.order.domain.OrderStatus;
import top.sama.haode.order.domain.Payment;
import top.sama.haode.order.domain.User;
import top.sama.haode.order.domain.UserCoupon;
import top.sama.haode.order.repository.CheckoutRepository;
import top.sama.haode.order.repository.CouponPolicyRepository;
import top.sama.haode.order.repository.CouponTemplateRepository;
import top.sama.haode.order.repository.InvitePurchaseRewardRepository;
import top.sama.haode.order.repository.OrderRepository;
import top.sama.haode.order.repository.PaymentRepository;
import top.sama.haode.order.repository.UserCouponRepository;
import top.sama.haode.order.repository.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CouponService {
    private static final List<OrderStatus> PAID_ORDER_STATUSES = List.of(
            OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.COMPLETED
    );

    private final CouponTemplateRepository templates;
    private final CouponPolicyRepository policies;
    private final UserCouponRepository userCoupons;
    private final InvitePurchaseRewardRepository purchaseRewards;
    private final UserRepository users;
    private final CheckoutRepository checkouts;
    private final PaymentRepository payments;
    private final OrderRepository orders;

    public CouponService(
            CouponTemplateRepository templates,
            CouponPolicyRepository policies,
            UserCouponRepository userCoupons,
            InvitePurchaseRewardRepository purchaseRewards,
            UserRepository users,
            CheckoutRepository checkouts,
            PaymentRepository payments,
            OrderRepository orders
    ) {
        this.templates = templates;
        this.policies = policies;
        this.userCoupons = userCoupons;
        this.purchaseRewards = purchaseRewards;
        this.users = users;
        this.checkouts = checkouts;
        this.payments = payments;
        this.orders = orders;
    }

    @Transactional
    public CouponPolicy policy() {
        return policies.findById(CouponPolicy.DEFAULT_ID).orElseGet(() -> policies.save(new CouponPolicy(CouponPolicy.DEFAULT_ID)));
    }

    @Transactional(readOnly = true)
    public List<CouponTemplate> listTemplates() {
        return templates.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public CouponTemplate createTemplate(String name, BigDecimal threshold, BigDecimal discount, boolean active, int perOrderLimit) {
        return templates.save(new CouponTemplate(name, threshold, discount, active, perOrderLimit));
    }

    @Transactional
    public CouponTemplate updateTemplate(UUID id, String name, BigDecimal threshold, BigDecimal discount, boolean active, int perOrderLimit) {
        CouponTemplate template = templates.findById(id).orElseThrow(() -> new IllegalArgumentException("优惠券模板不存在"));
        template.replace(name, threshold, discount, active, perOrderLimit);
        return template;
    }

    @Transactional
    public CouponPolicy updatePolicy(boolean enabled, UUID inviteTemplateId, UUID purchaseTemplateId, BigDecimal purchaseThreshold) {
        if (inviteTemplateId != null) requireTemplate(inviteTemplateId);
        if (purchaseTemplateId != null) requireTemplate(purchaseTemplateId);
        CouponPolicy policy = policy();
        policy.replace(enabled, inviteTemplateId, purchaseTemplateId, purchaseThreshold);
        return policy;
    }

    @Transactional
    public User ensureInviteCode(User user) {
        if (user.getInviteCode() != null && !user.getInviteCode().isBlank()) return user;
        for (int i = 0; i < 12; i++) {
            String code = InviteCodes.random();
            if (users.findByInviteCode(code).isEmpty()) {
                user.assignInviteCode(code);
                return users.save(user);
            }
        }
        throw new IllegalStateException("无法生成邀请码");
    }

    @Transactional
    public Wallet wallet(String userId) {
        User user = ensureInviteCode(requireUser(userId));
        User inviter = user.hasInviter() ? users.findById(user.getInviterId()).orElse(null) : null;
        return new Wallet(user, inviter, policy(), userCoupons.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Transactional
    public Wallet bindInvite(String userId, String rawCode) {
        String code = InviteCodes.normalize(rawCode);
        if (code.isBlank()) throw new IllegalArgumentException("请填写邀请码");
        CouponPolicy policy = policy();
        if (!policy.isEnabled()) throw new IllegalStateException("邀请活动未开启");
        User invitee = ensureInviteCode(requireUser(userId));
        if (invitee.hasInviter()) throw new IllegalStateException("已经绑定过邀请人");
        User inviter = users.findByInviteCode(code).orElseThrow(() -> new IllegalArgumentException("邀请码无效"));
        if (inviter.getId().equals(invitee.getId())) throw new IllegalArgumentException("不能填写自己的邀请码");
        if (invitee.getId().equals(inviter.getInviterId())) throw new IllegalArgumentException("不能互相绑定邀请关系");
        invitee.bindInviter(inviter.getId());
        users.save(invitee);
        grantFromTemplate(inviter.getId(), policy.getInviteTemplateId(), UserCoupon.Source.INVITE);
        grantFromTemplate(invitee.getId(), policy.getInviteTemplateId(), UserCoupon.Source.INVITE);
        return wallet(userId);
    }

    @Transactional
    public void applyToCheckout(Checkout checkout, String userId, UUID couponId) {
        if (couponId == null) {
            applyToCheckout(checkout, userId, List.of());
            return;
        }
        UserCoupon coupon = userCoupons.findByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new IllegalArgumentException("优惠券不存在"));
        applyToCheckout(checkout, userId, List.of(new Selection(coupon.getTemplateId(), 1)));
    }

    @Transactional
    public void applyToCheckout(Checkout checkout, String userId, List<Selection> selections) {
        if (checkout.getStatus() != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("结算单当前状态不可用券");
        releaseCheckout(checkout);
        if (selections == null || selections.isEmpty()) return;
        List<UserCoupon> locked = new ArrayList<>();
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (Selection selection : selections) {
            if (selection == null || selection.templateId() == null || selection.quantity() <= 0) continue;
            CouponTemplate template = requireTemplate(selection.templateId());
            if (selection.quantity() > template.getPerOrderLimit()) {
                throw new IllegalStateException(template.getName() + " 每单最多使用 " + template.getPerOrderLimit() + " 张");
            }
            List<UserCoupon> unused = userCoupons
                    .findByUserIdAndTemplateIdAndStatusOrderByCreatedAtAsc(userId, template.getId(), UserCoupon.Status.UNUSED)
                    .stream()
                    .filter(coupon -> coupon.usableFor(checkout.getGoodsAmount()))
                    .toList();
            if (selection.quantity() > unused.size()) {
                throw new IllegalStateException(template.getName() + " 可用数量不足");
            }
            for (int i = 0; i < selection.quantity(); i++) {
                UserCoupon coupon = unused.get(i);
                coupon.lock(checkout.getId());
                locked.add(coupon);
                totalDiscount = totalDiscount.add(coupon.getDiscountAmount());
            }
        }
        checkout.applyDiscount(totalDiscount);
        checkout.rememberCoupon(locked.isEmpty() ? null : locked.get(0).getId());
        Payment payment = payments.findByCheckoutId(checkout.getId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for checkout: " + checkout.getId()));
        payment.updateAmount(checkout.getAmount());
    }

    @Transactional
    public void releaseCheckout(Checkout checkout) {
        userCoupons.findByCheckoutId(checkout.getId()).forEach(coupon -> {
            if (coupon.getStatus() == UserCoupon.Status.LOCKED) coupon.release();
        });
        if (checkout.getStatus() != OrderStatus.PENDING_PAYMENT) return;
        if (checkout.getCouponId() != null || checkout.getDiscountAmount().signum() > 0) checkout.clearCoupon();
        payments.findByCheckoutId(checkout.getId()).ifPresent(payment -> {
            if (payment.getStatus() != top.sama.haode.order.domain.PaymentStatus.SUCCESS
                    && payment.getStatus() != top.sama.haode.order.domain.PaymentStatus.CLOSED) {
                payment.updateAmount(checkout.getAmount());
            }
        });
    }

    @Transactional
    public void markCheckoutUsed(UUID checkoutId) {
        userCoupons.findByCheckoutId(checkoutId).forEach(UserCoupon::markUsed);
    }

    @Transactional
    public void grantPurchaseReward(String inviteeId) {
        User invitee = users.findById(inviteeId).orElse(null);
        if (invitee == null || !invitee.hasInviter()) return;
        if (purchaseRewards.existsById(inviteeId)) return;
        CouponPolicy policy = policy();
        if (!policy.isEnabled() || policy.getPurchaseTemplateId() == null) return;
        BigDecimal spent = paidAmount(inviteeId);
        if (spent.compareTo(policy.getPurchaseThreshold()) < 0) return;
        UserCoupon granted = grantFromTemplate(invitee.getInviterId(), policy.getPurchaseTemplateId(), UserCoupon.Source.INVITE_PURCHASE);
        if (granted == null) return;
        purchaseRewards.save(new InvitePurchaseReward(inviteeId, invitee.getInviterId(), granted.getId()));
    }

    public List<CouponGroup> availableGroups(String userId, BigDecimal goodsAmount, UUID checkoutId) {
        Map<UUID, Integer> selected = new LinkedHashMap<>();
        if (checkoutId != null) {
            userCoupons.findByCheckoutId(checkoutId).stream()
                    .filter(coupon -> coupon.getStatus() == UserCoupon.Status.LOCKED || coupon.getStatus() == UserCoupon.Status.USED)
                    .forEach(coupon -> selected.merge(coupon.getTemplateId(), 1, Integer::sum));
        }
        Map<UUID, CouponGroup> groups = new LinkedHashMap<>();
        for (UserCoupon coupon : userCoupons.findByUserIdOrderByCreatedAtDesc(userId)) {
            UUID templateId = coupon.getTemplateId();
            if (templateId == null) continue;
            boolean counted = coupon.usableFor(goodsAmount)
                    || (checkoutId != null && checkoutId.equals(coupon.getCheckoutId())
                    && (coupon.getStatus() == UserCoupon.Status.LOCKED || coupon.getStatus() == UserCoupon.Status.USED));
            if (!counted) continue;
            CouponGroup current = groups.get(templateId);
            if (current == null) {
                int limit = templates.findById(templateId).map(CouponTemplate::getPerOrderLimit).orElse(1);
                groups.put(templateId, new CouponGroup(
                        templateId,
                        coupon.getName(),
                        coupon.getThresholdAmount(),
                        coupon.getDiscountAmount(),
                        limit,
                        1,
                        selected.getOrDefault(templateId, 0)
                ));
            } else {
                groups.put(templateId, current.plusAvailable());
            }
        }
        return List.copyOf(groups.values());
    }

    public String couponSummary(UUID checkoutId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        userCoupons.findByCheckoutId(checkoutId).stream()
                .filter(coupon -> coupon.getStatus() == UserCoupon.Status.LOCKED || coupon.getStatus() == UserCoupon.Status.USED)
                .forEach(coupon -> counts.merge(coupon.getName(), 1, Integer::sum));
        if (counts.isEmpty()) return null;
        List<String> parts = new ArrayList<>();
        counts.forEach((name, count) -> parts.add(count > 1 ? name + " ×" + count : name));
        return String.join("、", parts);
    }

    public CouponTemplate requireTemplate(UUID id) {
        return templates.findById(id).orElseThrow(() -> new IllegalArgumentException("优惠券模板不存在"));
    }

    public String couponName(UUID couponId) {
        if (couponId == null) return null;
        return userCoupons.findById(couponId).map(UserCoupon::getName).orElse(null);
    }

    private UserCoupon grantFromTemplate(String userId, UUID templateId, UserCoupon.Source source) {
        if (templateId == null) return null;
        CouponTemplate template = templates.findById(templateId).orElse(null);
        if (template == null || !template.isActive()) return null;
        return userCoupons.save(new UserCoupon(userId, template, source));
    }

    private BigDecimal paidAmount(String userId) {
        BigDecimal checkoutPaid = checkouts.sumAmountByUserIdAndStatus(userId, OrderStatus.PAID);
        BigDecimal standalone = orders.sumStandaloneAmountByUserIdAndStatusIn(userId, PAID_ORDER_STATUSES);
        return (checkoutPaid == null ? BigDecimal.ZERO : checkoutPaid)
                .add(standalone == null ? BigDecimal.ZERO : standalone);
    }

    private User requireUser(String userId) {
        return users.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    public record Wallet(User user, User inviter, CouponPolicy policy, List<UserCoupon> coupons) {}
    public record Selection(UUID templateId, int quantity) {}
    public record CouponGroup(
            UUID templateId,
            String name,
            BigDecimal thresholdAmount,
            BigDecimal discountAmount,
            int perOrderLimit,
            int availableCount,
            int selectedCount
    ) {
        CouponGroup plusAvailable() {
            return new CouponGroup(templateId, name, thresholdAmount, discountAmount, perOrderLimit, availableCount + 1, selectedCount);
        }
    }
}
