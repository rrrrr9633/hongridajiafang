package top.sama.haode.order.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sama.haode.order.domain.AfterSale;
import top.sama.haode.order.domain.AfterSaleStatus;
import top.sama.haode.order.domain.Checkout;
import top.sama.haode.order.domain.Order;
import top.sama.haode.order.domain.OrderStatus;
import top.sama.haode.order.domain.Payment;
import top.sama.haode.order.domain.PaymentStatus;
import top.sama.haode.order.domain.Product;
import top.sama.haode.order.domain.ProductType;
import top.sama.haode.order.domain.Review;
import top.sama.haode.order.domain.ReviewStatus;
import top.sama.haode.order.domain.User;
import top.sama.haode.order.repository.AfterSaleRepository;
import top.sama.haode.order.repository.CheckoutRepository;
import top.sama.haode.order.repository.ReviewImageRepository;
import top.sama.haode.order.repository.ReviewRepository;
import top.sama.haode.order.repository.OrderRepository;
import top.sama.haode.order.repository.PaymentRepository;
import top.sama.haode.order.repository.ProductRepository;
import top.sama.haode.order.repository.ProductTypeRepository;
import top.sama.haode.order.repository.UserRepository;
import top.sama.haode.order.service.WechatPayService;
import top.sama.haode.order.service.WechatShippingService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderApplicationService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final AfterSaleRepository afterSaleRepository;
    private final CheckoutRepository checkoutRepository;
    private final CouponService couponService;
    private final AddressService addressService;
    private final WechatPayService wechatPayService;
    private final WechatShippingService wechatShippingService;
    private final UserRepository userRepository;

    public OrderApplicationService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ProductRepository productRepository,
            ProductTypeRepository productTypeRepository,
            ReviewRepository reviewRepository,
            ReviewImageRepository reviewImageRepository,
            AfterSaleRepository afterSaleRepository,
            CheckoutRepository checkoutRepository,
            CouponService couponService,
            AddressService addressService,
            WechatPayService wechatPayService,
            WechatShippingService wechatShippingService,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
        this.productTypeRepository = productTypeRepository;
        this.reviewRepository = reviewRepository;
        this.reviewImageRepository = reviewImageRepository;
        this.afterSaleRepository = afterSaleRepository;
        this.checkoutRepository = checkoutRepository;
        this.couponService = couponService;
        this.addressService = addressService;
        this.wechatPayService = wechatPayService;
        this.wechatShippingService = wechatShippingService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Review> listProductReviews(String productId) {
        List<UUID> orderIds = orderRepository.findByProductIdAndStatusInOrderByCreatedAtDesc(
                productId,
                List.of(OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.COMPLETED,
                        OrderStatus.REFUNDING, OrderStatus.REFUNDED)
        ).stream().map(Order::getId).toList();
        if (orderIds.isEmpty()) return List.of();
        return reviewRepository.findByOrderIdInAndStatusOrderByCreatedAtDesc(
                orderIds.stream().limit(500).toList(), ReviewStatus.APPROVED);
    }
    @Transactional
    public List<Review> listReviews(String userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Review createReview(UUID orderId, String userId, int rating, String content) {
        Order order = getOrder(orderId, userId);
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("仅已完成订单可以评价");
        }
        if (reviewRepository.existsByOrderIdAndUserId(orderId, userId)) {
            throw new IllegalStateException("该订单已经评价过");
        }
        return reviewRepository.save(new Review(orderId, userId, rating, content));
    }

    @Transactional
    public List<AfterSale> listAfterSales(String userId) {
        return afterSaleRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public AfterSale createAfterSale(UUID orderId, String userId, String reason) {
        Order order = getOrder(orderId, userId);
        if (!order.canRequestAfterSale()) {
            throw new IllegalStateException("当前订单状态不支持退款/售后");
        }
        if (afterSaleRepository.existsByOrderIdAndUserId(orderId, userId)) {
            throw new IllegalStateException("该订单已经提交过售后申请");
        }
        OrderStatus previous = order.getStatus();
        order.markRefunding();
        return afterSaleRepository.save(new AfterSale(orderId, userId, reason, previous));
    }


    @Transactional
    public CheckoutResult createCheckout(String userId, List<CheckoutItem> items, UUID couponId) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("购物车为空");
        UUID checkoutId = UUID.randomUUID();
        Checkout checkout = checkoutRepository.save(new Checkout(checkoutId, userId, java.math.BigDecimal.ONE));
        List<Order> orders = new java.util.ArrayList<>();
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        java.util.Set<String> typeKeys = new java.util.HashSet<>();
        for (CheckoutItem item : items) {
            if (item.productId() == null || item.productId().isBlank()) {
                throw new IllegalArgumentException("购物车商品无效或重复");
            }
            if (item.quantity() < 1 || item.quantity() > 99) throw new IllegalArgumentException("商品数量无效");
            Product product = productRepository.findByIdAndActiveTrue(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("商品不存在或已下架"));
            ProductType type = resolveType(product.getId(), item.typeId());
            if (!typeKeys.add(type.getId())) {
                throw new IllegalArgumentException("购物车商品无效或重复");
            }
            if (product.getPrice() == null || product.getPrice().signum() <= 0) {
                throw new IllegalStateException("商品价格无效，暂不可结算");
            }
            Order order = new Order(userId, checkoutId, product, type, item.quantity());
            orders.add(order);
            total = total.add(order.getAmount());
        }
        checkout.updateAmount(total);
        checkoutRepository.save(checkout);
        List<Order> savedOrders = orderRepository.saveAll(orders);
        paymentRepository.save(new Payment(checkoutId, total, true));
        if (couponId != null) couponService.applyToCheckout(checkout, userId, couponId);
        return new CheckoutResult(checkout, savedOrders);
    }

    public record CheckoutItem(String productId, String typeId, int quantity) {}
    public record CheckoutResult(Checkout checkout, List<Order> orders) {}

    private void expireCheckoutIfNeeded(Checkout checkout) {
        if (checkout.getStatus() != OrderStatus.PENDING_PAYMENT
                || checkout.getCreatedAt().isAfter(Instant.now().minus(Duration.ofMinutes(5)))) return;
        if (confirmCheckoutFromWechat(checkout.getId())) return;
        couponService.releaseCheckout(checkout);
        checkout.cancel();
        paymentRepository.findByCheckoutId(checkout.getId()).ifPresent(Payment::close);
        orderRepository.findByCheckoutIdOrderByCreatedAtAsc(checkout.getId()).forEach(order -> {
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) order.cancel();
        });
    }

    @Transactional
    public CheckoutResult getCheckout(UUID checkoutId, String userId) {
        Checkout checkout = checkoutRepository.findByIdAndUserId(checkoutId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Checkout not found: " + checkoutId));
        confirmCheckoutFromWechat(checkoutId);
        checkout = checkoutRepository.findByIdAndUserId(checkoutId, userId).orElse(checkout);
        expireCheckoutIfNeeded(checkout);
        if (checkout.getStatus() == OrderStatus.PENDING_PAYMENT && !checkout.hasReceiver()) {
            var fallback = addressService.defaultAddress(userId);
            if (fallback != null) checkout.bindAddress(fallback);
        }
        return new CheckoutResult(checkout, orderRepository.findByCheckoutIdOrderByCreatedAtAsc(checkoutId));
    }

    @Transactional
    public CheckoutResult listCheckoutOrders(UUID checkoutId, String userId) {
        return getCheckout(checkoutId, userId);
    }

    @Transactional
    public CheckoutResult applyCheckoutCoupon(
            UUID checkoutId,
            String userId,
            UUID couponId,
            List<CouponService.Selection> selections
    ) {
        CheckoutResult result = getCheckout(checkoutId, userId);
        if (selections != null) couponService.applyToCheckout(result.checkout(), userId, selections);
        else couponService.applyToCheckout(result.checkout(), userId, couponId);
        return result;
    }

    @Transactional
    public CheckoutResult bindCheckoutAddress(UUID checkoutId, String userId, UUID addressId) {
        CheckoutResult result = getCheckout(checkoutId, userId);
        result.checkout().bindAddress(addressService.require(userId, addressId));
        return result;
    }

    @Transactional
    public Checkout markCheckoutPaying(UUID checkoutId, String userId) {
        Checkout checkout = checkoutRepository.findByIdAndUserId(checkoutId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Checkout not found: " + checkoutId));
        expireCheckoutIfNeeded(checkout);
        if (checkout.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("结算单已关闭或不可支付");
        }
        if (!checkout.hasReceiver()) throw new IllegalStateException("请先选择收货地址");
        paymentRepository.findByCheckoutId(checkoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for checkout: " + checkoutId))
                .beginPaying();
        return checkout;
    }

    @Transactional
    public Order createOrder(String userId, String productId, String typeId, Integer requestedQuantity) {
        int quantity = requestedQuantity == null ? 1 : requestedQuantity;
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or inactive: " + productId));
        ProductType type = resolveType(product.getId(), typeId);
        Order order = orderRepository.save(new Order(userId, product, type, quantity));
        paymentRepository.save(new Payment(order.getId(), order.getAmount()));
        return order;
    }

    private ProductType resolveType(String productId, String typeId) {
        if (typeId != null && !typeId.isBlank()) {
            return productTypeRepository.findByIdAndProductIdAndActiveTrue(typeId, productId)
                    .orElseThrow(() -> new IllegalArgumentException("商品类型不存在或已下架"));
        }
        return productTypeRepository.findByProductIdAndActiveTrueOrderBySortOrderAscIdAsc(productId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("商品尚未配置可售类型"));
    }

    @Transactional
    public List<Order> listOrders(String userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING_PAYMENT)
                .forEach(this::confirmOrderFromWechat);
        orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        orders.forEach(this::expireIfNeeded);
        return orders;
    }

    @Transactional
    public Order getOrder(UUID orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        confirmOrderFromWechat(order);
        order = orderRepository.findByIdAndUserId(orderId, userId).orElse(order);
        expireIfNeeded(order);
        return order;
    }

    @Transactional
    public Order getOrderForPayment(UUID orderId, String userId) {
        return getOrder(orderId, userId);
    }

    @Transactional
    public void markPaying(UUID orderId, String userId) {
        Order order = getOrder(orderId, userId);
        paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for order: " + orderId))
                .beginPaying();
    }

    private void expireIfNeeded(Order order) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                || order.getCreatedAt().isAfter(Instant.now().minus(Duration.ofMinutes(5)))) {
            return;
        }
        if (confirmOrderFromWechat(order)) return;
        if (order.getCheckoutId() != null) return;
        order.cancel();
        paymentRepository.findByOrderId(order.getId()).ifPresent(Payment::close);
    }

    @Transactional
    public Order cancelOrder(UUID orderId, String userId) {
        Order order = getOrder(orderId, userId);
        if (order.getCheckoutId() != null) {
            cancelCheckout(order.getCheckoutId(), userId);
            return orderRepository.findByIdAndUserId(orderId, userId).orElse(order);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        order.cancel();
        paymentRepository.findByOrderId(orderId).ifPresent(Payment::close);
        return order;
    }

    @Transactional
    public Checkout cancelCheckout(UUID checkoutId, String userId) {
        Checkout checkout = checkoutRepository.findByIdAndUserId(checkoutId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Checkout not found: " + checkoutId));
        if (checkout.getStatus() == OrderStatus.CANCELLED) return checkout;
        couponService.releaseCheckout(checkout);
        checkout.cancel();
        paymentRepository.findByCheckoutId(checkoutId).ifPresent(Payment::close);
        orderRepository.findByCheckoutIdOrderByCreatedAtAsc(checkoutId).forEach(order -> {
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) order.cancel();
        });
        return checkout;
    }

    @Transactional
    public void deleteOrder(UUID orderId, String userId) {
        Order order = getOrder(orderId, userId);
        if (order.getCheckoutId() != null) {
            Checkout checkout = checkoutRepository.findByIdAndUserId(order.getCheckoutId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("Checkout not found: " + order.getCheckoutId()));
            if (checkout.getStatus() != OrderStatus.CANCELLED) {
                throw new IllegalStateException("仅已取消结算单可以删除");
            }
            couponService.releaseCheckout(checkout);
            paymentRepository.deleteByCheckoutId(checkout.getId());
            orderRepository.deleteByCheckoutId(checkout.getId());
            checkoutRepository.delete(checkout);
            return;
        }
        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CREATED
                && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("已支付或售后订单不能删除");
        }
        paymentRepository.deleteByOrderId(orderId);
        orderRepository.delete(order);
    }

    @Transactional
    public Order cancelOrderAsAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getCheckoutId() != null) {
            Checkout checkout = checkoutRepository.findById(order.getCheckoutId())
                    .orElseThrow(() -> new IllegalArgumentException("Checkout not found: " + order.getCheckoutId()));
            cancelCheckout(checkout.getId(), checkout.getUserId());
            return orderRepository.findById(orderId).orElse(order);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        order.cancel();
        paymentRepository.findByOrderId(orderId).ifPresent(Payment::close);
        return order;
    }

    @Transactional
    public void deleteOrderAsAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getCheckoutId() != null) {
            Checkout checkout = checkoutRepository.findById(order.getCheckoutId())
                    .orElseThrow(() -> new IllegalArgumentException("Checkout not found: " + order.getCheckoutId()));
            if (!canAdminDelete(checkout.getStatus())) {
                throw new IllegalStateException("已支付或售后订单不能删除，请走售后退款");
            }
            couponService.releaseCheckout(checkout);
            paymentRepository.deleteByCheckoutId(checkout.getId());
            orderRepository.deleteByCheckoutId(checkout.getId());
            checkoutRepository.delete(checkout);
            return;
        }
        if (!canAdminDelete(order.getStatus())) {
            throw new IllegalStateException("已支付或售后订单不能删除，请走售后退款");
        }
        paymentRepository.deleteByOrderId(orderId);
        orderRepository.delete(order);
    }

    private static boolean canAdminDelete(OrderStatus status) {
        return status == OrderStatus.CANCELLED
                || status == OrderStatus.CREATED
                || status == OrderStatus.PENDING_PAYMENT;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelExpiredOrders() {
        Instant deadline = Instant.now().minus(Duration.ofMinutes(5));
        orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, deadline).forEach(order -> {
            if (order.getCheckoutId() == null) {
                order.cancel();
                paymentRepository.findByOrderId(order.getId()).ifPresent(Payment::close);
            }
        });
        checkoutRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, deadline)
                .forEach(this::expireCheckoutIfNeeded);
    }

    @Transactional
    public void markCheckoutPaid(UUID checkoutId, String transactionId) {
        Checkout checkout = checkoutRepository.findById(checkoutId)
                .orElseThrow(() -> new IllegalArgumentException("Checkout not found: " + checkoutId));
        Payment payment = paymentRepository.findByCheckoutId(checkoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for checkout: " + checkoutId));
        if (checkout.getStatus() == OrderStatus.PAID && payment.getStatus() == PaymentStatus.SUCCESS) {
            if (!transactionId.equals(payment.getTransactionId())) throw new IllegalStateException("Payment transaction does not match existing payment");
            return;
        }
        if (checkout.getStatus() != OrderStatus.PENDING_PAYMENT && checkout.getStatus() != OrderStatus.CANCELLED) {
            throw new IllegalStateException("结算单状态与支付结果不一致");
        }
        payment.succeed(transactionId);
        checkout.markPaid();
        List<Order> orders = orderRepository.findByCheckoutIdOrderByCreatedAtAsc(checkoutId);
        for (Order order : orders) {
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.CANCELLED) {
                throw new IllegalStateException("商品订单状态与结算单不一致");
            }
        }
        orders.forEach(Order::markPaid);
        couponService.markCheckoutUsed(checkoutId);
        couponService.grantPurchaseReward(checkout.getUserId());
    }

    @Transactional
    public Order markPaid(UUID orderId, String transactionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for order: " + orderId));
        if (order.getStatus().name().equals("PAID") && payment.getStatus().name().equals("SUCCESS")) {
            if (!transactionId.equals(payment.getTransactionId())) {
                throw new IllegalStateException("Payment transaction does not match existing payment");
            }
            return order;
        }
        payment.succeed(transactionId);
        order.markPaid();
        couponService.grantPurchaseReward(order.getUserId());
        return order;
    }

    @Transactional
    public Order markPaid(UUID orderId, String userId, String transactionId) {
        Order order = getOrder(orderId, userId);
        return markPaid(order.getId(), transactionId);
    }

    @Transactional
    public boolean confirmPaidByOutTradeNo(String outTradeNo, String transactionId) {
        UUID id = WechatPayService.idFromOutTradeNo(outTradeNo);
        if (checkoutRepository.findById(id).isPresent()) {
            markCheckoutPaid(id, transactionId);
            return true;
        }
        if (orderRepository.findById(id).isPresent()) {
            markPaid(id, transactionId);
            return true;
        }
        throw new IllegalArgumentException("找不到对应支付单");
    }

    @Transactional
    public boolean confirmCheckoutFromWechat(UUID checkoutId) {
        return confirmWechatTrade(checkoutId, true);
    }

    @Transactional
    public boolean confirmOrderFromWechat(Order order) {
        UUID tradeId = order.getCheckoutId() != null ? order.getCheckoutId() : order.getId();
        return confirmWechatTrade(tradeId, order.getCheckoutId() != null);
    }

    @Transactional
    public void syncCheckoutPayment(UUID checkoutId, String userId) {
        getCheckout(checkoutId, userId);
        confirmCheckoutFromWechat(checkoutId);
    }

    @Transactional
    public void syncOrderPayment(UUID orderId, String userId) {
        Order order = getOrder(orderId, userId);
        confirmOrderFromWechat(order);
    }

    private boolean confirmWechatTrade(UUID tradeId, boolean checkout) {
        if (!wechatPayService.configured()) return false;
        try {
            return wechatPayService.queryByOutTradeNo(tradeId)
                    .filter(result -> "SUCCESS".equals(result.tradeState()) && result.transactionId() != null && !result.transactionId().isBlank())
                    .map(result -> {
                        if (checkout) markCheckoutPaid(tradeId, result.transactionId());
                        else markPaid(tradeId, result.transactionId());
                        return true;
                    })
                    .orElse(false);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void reconcilePayingWechatOrders() {
        if (!wechatPayService.configured()) return;
        paymentRepository.findByStatus(PaymentStatus.PAYING).forEach(payment -> {
            if (payment.getCheckoutId() != null) confirmCheckoutFromWechat(payment.getCheckoutId());
            else if (payment.getOrderId() != null) {
                orderRepository.findById(payment.getOrderId()).ifPresent(this::confirmOrderFromWechat);
            }
        });
    }

    @Transactional
    public Order shipOrder(UUID orderId, String mode, String trackingNo, String expressCompany) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PROCESSING) {
            throw new IllegalStateException("当前订单状态不可发货");
        }
        UUID checkoutId = order.getCheckoutId();
        List<Order> group = checkoutId == null
                ? List.of(order)
                : orderRepository.findByCheckoutIdOrderByCreatedAtAsc(checkoutId);
        Payment payment = checkoutId != null
                ? paymentRepository.findByCheckoutId(checkoutId).orElseThrow(() -> new IllegalArgumentException("Payment not found"))
                : paymentRepository.findByOrderId(orderId).orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
            throw new IllegalStateException("订单尚未完成微信支付，不能上报发货");
        }
        boolean express = isExpressShip(mode, trackingNo);
        String company = expressCompany == null ? "" : expressCompany.trim();
        String tracking = trackingNo == null ? "" : trackingNo.trim();
        if (express) {
            if (tracking.isBlank()) throw new IllegalArgumentException("快递发货需要物流单号");
            if (company.isBlank()) throw new IllegalArgumentException("请填写物流公司编码，例如 SF");
        }
        User user = userRepository.findById(order.getUserId()).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        String itemDesc = group.stream().map(Order::getProductName).reduce((a, b) -> a + "、" + b).orElse("红日大家纺");
        Checkout checkout = checkoutId == null ? null : checkoutRepository.findById(checkoutId).orElse(null);
        String phone = checkout == null ? "" : checkout.getReceiverPhone();
        boolean correcting = group.stream().anyMatch(item -> item.getShippedAt() != null);
        try {
            wechatShippingService.upload(new WechatShippingService.ShippingUpload(
                    payment.getTransactionId(),
                    user.getWechatOpenid(),
                    itemDesc,
                    express ? 1 : 4,
                    express ? tracking : "",
                    company,
                    phone
            ));
        } catch (IllegalStateException exception) {
            if (!correcting) throw exception;
        }
        for (Order item : group) {
            item.ship(express ? tracking : "NONE", express ? company : "NONE");
            item.markShippingUploaded();
        }
        return order;
    }

    private static boolean isExpressShip(String mode, String trackingNo) {
        String shippingMode = mode == null ? "" : mode.trim().toUpperCase();
        if (shippingMode.equals("NONE") || shippingMode.equals("PICKUP")) return false;
        if (shippingMode.equals("EXPRESS")) return true;
        return trackingNo != null && !trackingNo.isBlank();
    }

    @Transactional
    public Order completeOrder(UUID orderId, String userId) {
        return completeShippedOrder(getOrder(orderId, userId));
    }

    @Transactional
    public Order completeOrderAsAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return completeShippedOrder(order);
    }

    private Order completeShippedOrder(Order order) {
        UUID checkoutId = order.getCheckoutId();
        List<Order> group = checkoutId == null
                ? List.of(order)
                : orderRepository.findByCheckoutIdOrderByCreatedAtAsc(checkoutId);
        for (Order item : group) {
            if (item.getStatus() != OrderStatus.PROCESSING && item.getStatus() != OrderStatus.COMPLETED) {
                throw new IllegalStateException("只有已发货订单可以完成");
            }
        }
        Payment payment = checkoutId != null
                ? paymentRepository.findByCheckoutId(checkoutId).orElse(null)
                : paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (payment != null && payment.getTransactionId() != null && !payment.getTransactionId().isBlank()) {
            try {
                wechatShippingService.notifyConfirm(payment.getTransactionId());
            } catch (IllegalStateException ignored) {
            }
        }
        for (Order item : group) {
            if (item.getStatus() == OrderStatus.PROCESSING) item.complete();
        }
        return order;
    }

    @Transactional
    public AfterSale approveAfterSale(UUID afterSaleId) {
        AfterSale afterSale = afterSaleRepository.findById(afterSaleId)
                .orElseThrow(() -> new IllegalArgumentException("售后单不存在"));
        if (afterSale.getStatus() == AfterSaleStatus.REFUNDED) return afterSale;
        if (afterSale.getStatus() == AfterSaleStatus.APPROVED) return afterSale;
        Order order = orderRepository.findById(afterSale.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        Payment payment = resolvePayment(order);
        afterSale.approve(WechatPayService.outTradeNo(afterSale.getId()));
        payment.markRefunding();
        WechatPayService.RefundResult result = wechatPayService.refund(
                payment.getTransactionId(),
                afterSale.getId(),
                order.getAmount(),
                payment.getAmount(),
                afterSale.getReason()
        );
        if ("SUCCESS".equals(result.status())) {
            afterSale.markRefunded(result.refundId());
            order.markRefunded();
            if (allCheckoutOrdersRefunded(order)) payment.markRefunded();
        }
        return afterSale;
    }

    @Transactional
    public AfterSale rejectAfterSale(UUID afterSaleId) {
        AfterSale afterSale = afterSaleRepository.findById(afterSaleId)
                .orElseThrow(() -> new IllegalArgumentException("售后单不存在"));
        afterSale.reject();
        Order order = orderRepository.findById(afterSale.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        order.restoreFromRefunding();
        return afterSale;
    }

    @Transactional
    public void confirmRefund(String outRefundNo, String refundId, String refundStatus) {
        if (!"SUCCESS".equals(refundStatus)) return;
        AfterSale afterSale = afterSaleRepository.findByOutRefundNo(outRefundNo)
                .orElseThrow(() -> new IllegalArgumentException("退款单不存在"));
        afterSale.markRefunded(refundId);
        Order order = orderRepository.findById(afterSale.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        order.markRefunded();
        Payment payment = resolvePayment(order);
        if (allCheckoutOrdersRefunded(order)) payment.markRefunded();
    }

    public List<AfterSale> listAllAfterSales() {
        return afterSaleRepository.findAll();
    }

    private Payment resolvePayment(Order order) {
        if (order.getCheckoutId() != null) {
            return paymentRepository.findByCheckoutId(order.getCheckoutId())
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        }
        return paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    }

    private boolean allCheckoutOrdersRefunded(Order order) {
        if (order.getCheckoutId() == null) return true;
        return orderRepository.findByCheckoutIdOrderByCreatedAtAsc(order.getCheckoutId()).stream()
                .allMatch(item -> item.getStatus() == OrderStatus.REFUNDED);
    }
}
