package top.sama.haode.order.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sama.haode.order.domain.CouponTemplateProduct;
import top.sama.haode.order.domain.Order;
import top.sama.haode.order.domain.Product;
import top.sama.haode.order.repository.CouponTemplateProductRepository;
import top.sama.haode.order.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CouponProductScope {
    private final CouponTemplateProductRepository scopes;
    private final ProductRepository products;

    public CouponProductScope(CouponTemplateProductRepository scopes, ProductRepository products) {
        this.scopes = scopes;
        this.products = products;
    }

    @Transactional
    public void replace(UUID templateId, List<String> productIds) {
        List<String> ids = normalize(productIds);
        if (ids.isEmpty()) throw new IllegalArgumentException("请至少指定一个可用商品");
        List<Product> found = products.findAllById(ids);
        if (found.size() != ids.size()) throw new IllegalArgumentException("包含不存在的商品");
        scopes.deleteByTemplateId(templateId);
        scopes.flush();
        ids.forEach(id -> scopes.save(new CouponTemplateProduct(templateId, id)));
    }

    @Transactional(readOnly = true)
    public List<String> productIds(UUID templateId) {
        return scopes.findByTemplateId(templateId).stream().map(CouponTemplateProduct::getProductId).toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<String>> productIds(Collection<UUID> templateIds) {
        Map<UUID, List<String>> grouped = new LinkedHashMap<>();
        if (templateIds == null || templateIds.isEmpty()) return grouped;
        scopes.findByTemplateIds(templateIds).forEach(row -> grouped
                .computeIfAbsent(row.getTemplateId(), ignored -> new java.util.ArrayList<>())
                .add(row.getProductId()));
        return grouped;
    }

    public BigDecimal eligibleAmount(Collection<String> productIds, List<Order> orders) {
        if (productIds == null || productIds.isEmpty() || orders == null || orders.isEmpty()) return BigDecimal.ZERO;
        Set<String> allowed = Set.copyOf(productIds);
        return orders.stream()
                .filter(order -> allowed.contains(order.getProductId()))
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal unionAmount(Collection<String> productIds, List<Order> orders) {
        return eligibleAmount(productIds, orders);
    }

    @Transactional(readOnly = true)
    public String label(Collection<String> productIds) {
        List<String> ids = productIds == null ? List.of() : List.copyOf(new LinkedHashSet<>(productIds));
        if (ids.isEmpty()) return "未指定可用商品";
        Map<String, String> names = new LinkedHashMap<>();
        products.findAllById(ids).forEach(product -> names.put(product.getId(), product.getName()));
        if (ids.size() <= 3) {
            return "仅限 " + ids.stream().map(id -> names.getOrDefault(id, id)).reduce((left, right) -> left + "、" + right).orElse("指定商品");
        }
        return "仅限 " + ids.size() + " 个指定商品";
    }

    private static List<String> normalize(List<String> productIds) {
        if (productIds == null) return List.of();
        return productIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
