package top.sama.haode.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "coupon_template_products")
public class CouponTemplateProduct {
    @EmbeddedId
    private Key id;

    protected CouponTemplateProduct() {
    }

    public CouponTemplateProduct(UUID templateId, String productId) {
        this.id = new Key(templateId, productId);
    }

    public UUID getTemplateId() {
        return id.templateId;
    }

    public String getProductId() {
        return id.productId;
    }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "template_id", nullable = false)
        private UUID templateId;

        @Column(name = "product_id", nullable = false, length = 64)
        private String productId;

        protected Key() {
        }

        public Key(UUID templateId, String productId) {
            this.templateId = templateId;
            this.productId = productId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return templateId.equals(key.templateId) && productId.equals(key.productId);
        }

        @Override
        public int hashCode() {
            return templateId.hashCode() * 31 + productId.hashCode();
        }
    }
}
