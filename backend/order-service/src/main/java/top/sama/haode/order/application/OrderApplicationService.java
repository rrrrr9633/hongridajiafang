package top.sama.haode.order.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sama.haode.order.domain.Checkout;
import top.sama.haode.order.domain.AfterSale;
import top.sama.haode.order.domain.Order;
import top.sama.haode.order.domain.OrderStatus;
import top.sama.haode.order.domain.Payment;
import top.sama.haode.order.domain.Product;
import top.sama.haode.order.domain.Review;
import top.sama.haode.order.domain.ReviewStatus;
import top.sama.haode.order.repository.AfterSaleRepository;
import top.sama.haode.order.repository.CheckoutRepository;
import top.sama.haode.order.repository.ReviewRepository;
import top.sama.haode.order.repository.OrderRepository;
import top.sama.haode.order.repository.PaymentRepository;
import top.sama.haode.order.repository.ProductRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderApplicationService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final AfterSaleRepository afterSaleRepository;
    private final CheckoutRepository checkoutRepository;

    public OrderApplicationService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ProductRepository productRepository,
            ReviewRepository reviewRepository,
            AfterSaleRepository afterSaleRepository,
            CheckoutRepository checkoutRepository
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.afterSaleRepository = afterSaleRepository;
        this.checkoutRepository = checkoutRepository;
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
        order.markRefunding();
        return afterSaleRepository.save(new AfterSale(orderId, userId, reason));
    }


    @Transactional
    public CheckoutResult createCheckout(String userId, List<CheckoutItem> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("购物车为空");
        UUID checkoutId = UUID.randomUUID();
        Checkout checkout = checkoutRepository.save(new Checkout(checkoutId, userId, java.math.BigDecimal.ONE));
        List<Order> orders = new java.util.ArrayList<>();
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        java.util.Set<String> productIds = new java.util.HashSet<>();
        for (CheckoutItem item : items) {
            if (item.productId() == null || item.productId().isBlank() || !productIds.add(item.productId())) {
                throw new IllegalArgumentException("购物车商品无效或重复");
            }
            if (item.quantity() < 1 || item.quantity() > 99) throw new IllegalArgumentException("商品数量无效");
            Product product = productRepository.findByIdAndActiveTrue(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("商品不存在或已下架"));
            if (product.getPrice() == null || product.getPrice().signum() <= 0) {
                throw new IllegalStateException("商品价格无效，暂不可结算");
            }
            Order order = new Order(userId, checkoutId, product, item.quantity());
            orders.add(order);
            total = total.add(order.getAmount());
        }
        checkout.updateAmount(total);
        checkoutRepository.save(checkout);
        List<Order> savedOrders = orderRepository.saveAll(orders);
        paymentRepository.save(new Payment(checkoutId, total, true));
        return new CheckoutResult(checkout, savedOrders);
    }

    public record CheckoutItem(String productId, int quantity) {}
    public record CheckoutResult(Checkout checkout, List<Order> orders) {}

    private void expireCheckoutIfNeeded(Checkout checkout) {
        if (checkout.getStatus() != OrderStatus.PENDING_PAYMENT
                || checkout.getCreatedAt().isAfter(Instant.now().minus(Duration.ofMinutes(5)))) return;
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
        expireCheckoutIfNeeded(checkout);
        return new CheckoutResult(checkout, orderRepository.findByCheckoutIdOrderByCreatedAtAsc(checkoutId));
    }

    @Transactional
    public CheckoutResult listCheckoutOrders(UUID checkoutId, String userId) {
        return getCheckout(checkoutId, userId);
    }

    @Transactional
    public void markCheckoutPaying(UUID checkoutId, String userId) {
        Checkout checkout = checkoutRepository.findByIdAndUserId(checkoutId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Checkout not found: " + checkoutId));
        expireCheckoutIfNeeded(checkout);
        if (checkout.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("结算单已关闭或不可支付");
        }
        paymentRepository.findByCheckoutId(checkoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for checkout: " + checkoutId))
                .beginPaying();
    }

    @Transactional
    public Order createOrder(String userId, String productId, Integer requestedQuantity) {
        int quantity = requestedQuantity == null ? 1 : requestedQuantity;
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or inactive: " + productId));
        Order order = orderRepository.save(new Order(userId, product, quantity));
        paymentRepository.save(new Payment(order.getId(), order.getAmount()));
        return order;
    }

    @Transactional
    public List<Order> listOrders(String userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        orders.forEach(this::expireIfNeeded);
        return orders;
    }

    @Transactional
    public Order getOrder(UUID orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
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
        order.cancel();
        paymentRepository.findByOrderId(order.getId()).ifPresent(Payment::close);
    }

    @Transactional
    public Order cancelOrder(UUID orderId, String userId) {
        Order order = getOrder(orderId, userId);
        if (order.getCheckoutId() != null) {
            throw new IllegalStateException("结算单中的商品订单需通过整笔结算单取消");
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
        if (order.getCheckoutId() != null) throw new IllegalStateException("结算单中的商品订单需通过结算单取消");
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        order.cancel();
        paymentRepository.findByOrderId(orderId).ifPresent(Payment::close);
        return order;
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
        if (checkout.getStatus() == OrderStatus.PAID && payment.getStatus() == top.sama.haode.order.domain.PaymentStatus.SUCCESS) {
            if (!transactionId.equals(payment.getTransactionId())) throw new IllegalStateException("Payment transaction does not match existing payment");
            return;
        }
        if (checkout.getStatus() != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("结算单状态与支付结果不一致");
        payment.succeed(transactionId);
        checkout.markPaid();
        List<Order> orders = orderRepository.findByCheckoutIdOrderByCreatedAtAsc(checkoutId);
        for (Order order : orders) {
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("商品订单状态与结算单不一致");
        }
        orders.forEach(Order::markPaid);
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
        return order;
    }

    @Transactional
    public Order markPaid(UUID orderId, String userId, String transactionId) {
        Order order = getOrder(orderId, userId);
        return markPaid(order.getId(), transactionId);
    }
}
