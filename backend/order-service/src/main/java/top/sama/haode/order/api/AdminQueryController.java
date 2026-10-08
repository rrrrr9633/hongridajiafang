package top.sama.haode.order.api;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.sama.haode.order.domain.Order;
import top.sama.haode.order.domain.OrderStatus;
import top.sama.haode.order.domain.Payment;
import top.sama.haode.order.domain.User;
import top.sama.haode.order.repository.OrderRepository;
import top.sama.haode.order.repository.PaymentRepository;
import top.sama.haode.order.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminQueryController {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public AdminQueryController(UserRepository userRepository, OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/users")
    public AdminPage<UserResponse> users(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String query = q == null ? "" : q.trim();
        Specification<User> spec = (root, ignored, cb) -> {
            if (query.isEmpty()) return cb.conjunction();
            String like = "%" + query.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("id")), like),
                    cb.like(cb.lower(root.get("phone")), like)
            );
        };
        return AdminPage.from(
                userRepository.findAll(spec, PageRequest.of(Math.max(page, 0), bound(size), Sort.by(Sort.Direction.DESC, "createdAt"))),
                UserResponse::from
        );
    }

    @GetMapping("/orders")
    public AdminPage<OrderResponse> orders(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String query = q == null ? "" : q.trim();
        Specification<Order> spec = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!query.isEmpty()) {
                String like = "%" + query.toLowerCase() + "%";
                List<Predicate> matches = new ArrayList<>();
                matches.add(cb.like(cb.lower(root.get("userId")), like));
                matches.add(cb.like(cb.lower(root.get("productName")), like));
                matches.add(cb.like(cb.lower(root.get("productId")), like));
                try {
                    matches.add(cb.equal(root.get("id"), UUID.fromString(query)));
                } catch (IllegalArgumentException ignoredUuid) {
                    matches.add(cb.like(cb.lower(root.get("id").as(String.class)), like));
                }
                predicates.add(cb.or(matches.toArray(Predicate[]::new)));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), OrderStatus.valueOf(status)));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
        return AdminPage.from(
                orderRepository.findAll(spec, PageRequest.of(Math.max(page, 0), bound(size), Sort.by(Sort.Direction.DESC, "createdAt"))),
                OrderResponse::from
        );
    }

    @GetMapping("/summary")
    public Summary summary() {
        List<Payment> payments = paymentRepository.findAll();
        BigDecimal paidAmount = payments.stream()
                .filter(payment -> "SUCCESS".equals(payment.getStatus().name()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Summary(userRepository.count(), orderRepository.count(), paidAmount, Instant.now());
    }

    private static int bound(int size) {
        return Math.min(Math.max(size, 1), 50);
    }

    public record UserResponse(String id, String phone, Instant createdAt, Instant updatedAt) {
        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getPhone(), user.getCreatedAt(), user.getUpdatedAt());
        }
    }

    public record OrderResponse(String id, String userId, String productId, String title, BigDecimal amount, String status, Instant createdAt) {
        static OrderResponse from(Order order) {
            return new OrderResponse(order.getId().toString(), order.getUserId(), order.getProductId(),
                    order.getProductTypeName() == null || order.getProductTypeName().isBlank()
                            ? order.getProductName()
                            : order.getProductName() + " · " + order.getProductTypeName(),
                    order.getAmount(), order.getStatus().name(), order.getCreatedAt());
        }
    }

    public record Summary(long userCount, long orderCount, BigDecimal paidAmount, Instant calculatedAt) {}
}
