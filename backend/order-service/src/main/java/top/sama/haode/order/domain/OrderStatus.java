package top.sama.haode.order.domain;

public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    PAID,
    PROCESSING,
    COMPLETED,
    CANCELLED,
    REFUNDING,
    REFUNDED
}
