package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "checkout_id", unique = true)
    private UUID checkoutId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "transaction_id", unique = true, length = 128)
    private String transactionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(UUID orderId, BigDecimal amount) {
        this.orderId = orderId;
        this.amount = amount;
        initialize();
    }

    public Payment(UUID checkoutId, BigDecimal amount, boolean checkoutPayment) {
        this.checkoutId = checkoutId;
        this.amount = amount;
        initialize();
    }

    private void initialize() {
        this.status = PaymentStatus.INITIATED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getCheckoutId() {
        return checkoutId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void beginPaying() {
        if (status == PaymentStatus.INITIATED) {
            status = PaymentStatus.PAYING;
            updatedAt = Instant.now();
            return;
        }
        if (status != PaymentStatus.PAYING) {
            throw new IllegalStateException("Payment cannot start from status: " + status);
        }
    }

    public void succeed(String transactionId) {
        if (status == PaymentStatus.SUCCESS) {
            if (!transactionId.equals(this.transactionId)) {
                throw new IllegalStateException("Payment transaction does not match existing payment");
            }
            return;
        }
        if (status != PaymentStatus.INITIATED && status != PaymentStatus.PAYING) {
            throw new IllegalStateException("Payment cannot transition to success");
        }
        this.status = PaymentStatus.SUCCESS;
        this.transactionId = transactionId;
        this.updatedAt = Instant.now();
    }

    public void close() {
        if (status == PaymentStatus.CLOSED) {
            return;
        }
        if (status != PaymentStatus.INITIATED && status != PaymentStatus.PAYING) {
            throw new IllegalStateException("Payment cannot be closed from status: " + status);
        }
        status = PaymentStatus.CLOSED;
        updatedAt = Instant.now();
    }
}
