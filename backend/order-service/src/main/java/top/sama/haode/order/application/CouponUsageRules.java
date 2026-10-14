package top.sama.haode.order.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sama.haode.order.domain.CouponTemplate;
import top.sama.haode.order.domain.CouponUsageRule;
import top.sama.haode.order.domain.Order;
import top.sama.haode.order.domain.Product;
import top.sama.haode.order.repository.CouponTemplateRepository;
import top.sama.haode.order.repository.CouponUsageRuleRepository;
import top.sama.haode.order.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CouponUsageRules {
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};
    private static final TypeReference<List<OptionDraft>> OPTIONS = new TypeReference<>() {};

    private final CouponUsageRuleRepository rules;
    private final CouponTemplateRepository templates;
    private final ProductRepository products;
    private final ObjectMapper json;

    public CouponUsageRules(
            CouponUsageRuleRepository rules,
            CouponTemplateRepository templates,
            ProductRepository products,
            ObjectMapper json
    ) {
        this.rules = rules;
        this.templates = templates;
        this.products = products;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<RuleView> list() {
        return rules.findAllByOrderByPriorityAscCreatedAtAsc().stream().map(this::view).toList();
    }

    @Transactional
    public RuleView save(UUID id, String name, Integer priority, List<String> zones, List<String> productIds, List<OptionDraft> options) {
        Draft draft = normalize(name, priority, zones, productIds, options);
        CouponUsageRule rule = id == null
                ? new CouponUsageRule(draft.name(), draft.priority(), write(draft.zones()), write(draft.productIds()), write(draft.options()))
                : rules.findById(id).orElseThrow(() -> new IllegalArgumentException("用券规则不存在"));
        if (id != null) rule.replace(draft.name(), draft.priority(), write(draft.zones()), write(draft.productIds()), write(draft.options()));
        return view(rules.save(rule));
    }

    @Transactional
    public void delete(UUID id) {
        if (!rules.existsById(id)) throw new IllegalArgumentException("用券规则不存在");
        rules.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Decision decide(List<Order> lines) {
        List<Order> cart = lines == null ? List.of() : lines;
        Map<String, String> zones = zonesOf(cart);
        List<CouponRuleMatch.CartItem> items = cart.stream()
                .map(order -> new CouponRuleMatch.CartItem(order.getProductId(), zones.get(order.getProductId())))
                .toList();
        List<CouponRuleMatch.Rule> parsed = rules.findAllByOrderByPriorityAscCreatedAtAsc().stream().map(this::parsed).toList();
        List<CouponRuleMatch.Rule> active = CouponRuleMatch.active(parsed, items);
        BigDecimal goods = cart.stream().map(Order::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Decision(active, goods, names());
    }

    @Transactional(readOnly = true)
    public String templateHint(UUID templateId) {
        if (templateId == null) return "尚未加入用券规则";
        List<String> names = rules.findAllByOrderByPriorityAscCreatedAtAsc().stream()
                .map(this::parsed)
                .filter(rule -> rule.options().stream().anyMatch(option -> option.quantityOf(templateId) > 0))
                .map(CouponRuleMatch.Rule::name)
                .toList();
        if (names.isEmpty()) return "尚未加入用券规则";
        if (names.size() <= 2) return "用于 " + String.join("、", names);
        return "用于 " + names.size() + " 条用券规则";
    }

    private Draft normalize(String name, Integer priority, List<String> zones, List<String> productIds, List<OptionDraft> options) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("请填写规则名称");
        if (priority == null || priority < 1 || priority > 999) throw new IllegalArgumentException("优先级需在 1 到 999 之间，数字越小越优先");
        List<String> zoneList = clean(zones);
        List<String> productList = clean(productIds);
        if (zoneList.isEmpty() && productList.isEmpty()) throw new IllegalArgumentException("请至少选择一个分区或商品");
        if (!productList.isEmpty()) {
            List<Product> found = products.findAllById(productList);
            if (found.size() != productList.size()) throw new IllegalArgumentException("包含不存在的商品");
        }
        if (options == null || options.isEmpty()) throw new IllegalArgumentException("请至少添加一种用法");
        List<OptionDraft> normalized = new ArrayList<>();
        for (OptionDraft option : options) {
            if (option == null || option.items() == null || option.items().isEmpty()) {
                throw new IllegalArgumentException("每种用法至少要选一张券");
            }
            Map<UUID, Integer> counts = new LinkedHashMap<>();
            for (ItemDraft item : option.items()) {
                if (item == null || item.templateId() == null) throw new IllegalArgumentException("请选择优惠券");
                if (item.quantity() < 1 || item.quantity() > 99) throw new IllegalArgumentException("每种券的张数需在 1 到 99 之间");
                CouponTemplate template = templates.findById(item.templateId()).orElseThrow(() -> new IllegalArgumentException("优惠券模板不存在"));
                if (!template.isActive()) throw new IllegalArgumentException(template.getName() + " 已停用，不能写入规则");
                counts.merge(item.templateId(), item.quantity(), Integer::sum);
            }
            List<ItemDraft> items = counts.entrySet().stream().map(entry -> new ItemDraft(entry.getKey(), entry.getValue())).toList();
            if (items.stream().anyMatch(item -> item.quantity() > 99)) throw new IllegalArgumentException("每种券的张数需在 1 到 99 之间");
            normalized.add(new OptionDraft(items));
        }
        return new Draft(name.trim(), priority, zoneList, productList, normalized);
    }

    private RuleView view(CouponUsageRule rule) {
        CouponRuleMatch.Rule parsed = parsed(rule);
        return new RuleView(rule.getId(), rule.getName(), rule.getPriority(), List.copyOf(parsed.zones()), List.copyOf(parsed.productIds()), drafts(parsed));
    }

    private CouponRuleMatch.Rule parsed(CouponUsageRule rule) {
        return new CouponRuleMatch.Rule(
                rule.getId(),
                rule.getName(),
                rule.getPriority(),
                new LinkedHashSet<>(read(rule.getZonesJson(), STRINGS)),
                new LinkedHashSet<>(read(rule.getProductIdsJson(), STRINGS)),
                read(rule.getOptionsJson(), OPTIONS).stream().map(option -> new CouponRuleMatch.Option(
                        option.items().stream().map(item -> new CouponRuleMatch.Item(item.templateId(), item.quantity())).toList()
                )).toList()
        );
    }

    private List<OptionDraft> drafts(CouponRuleMatch.Rule rule) {
        return rule.options().stream()
                .map(option -> new OptionDraft(option.items().stream().map(item -> new ItemDraft(item.templateId(), item.quantity())).toList()))
                .toList();
    }

    private Map<String, String> zonesOf(List<Order> cart) {
        List<String> ids = cart.stream().map(Order::getProductId).distinct().toList();
        Map<String, String> zones = new LinkedHashMap<>();
        if (ids.isEmpty()) return zones;
        products.findAllById(ids).forEach(product -> zones.put(product.getId(), product.getZone()));
        return zones;
    }

    private Map<UUID, String> names() {
        Map<UUID, String> names = new LinkedHashMap<>();
        templates.findAll().forEach(template -> names.put(template.getId(), template.getName()));
        return names;
    }

    private List<String> clean(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).distinct().toList();
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("规则无法保存");
        }
    }

    private <T> T read(String raw, TypeReference<T> type) {
        try {
            return json.readValue(raw == null || raw.isBlank() ? "[]" : raw, type);
        } catch (Exception exception) {
            throw new IllegalStateException("用券规则数据损坏");
        }
    }

    private record Draft(String name, int priority, List<String> zones, List<String> productIds, List<OptionDraft> options) {}

    public record ItemDraft(UUID templateId, int quantity) {}
    public record OptionDraft(List<ItemDraft> items) {}
    public record RuleView(UUID id, String name, int priority, List<String> zones, List<String> productIds, List<OptionDraft> options) {}

    public record Decision(List<CouponRuleMatch.Rule> active, BigDecimal goodsAmount, Map<UUID, String> names) {
        public boolean fits(Map<UUID, Integer> selection) {
            return CouponRuleMatch.fits(active, selection);
        }

        public int maxOf(UUID templateId) {
            return CouponRuleMatch.maxOf(active, templateId);
        }

        public String summary() {
            return CouponRuleMatch.summary(active, names);
        }

        public String rejection() {
            if (active.isEmpty()) return summary();
            return "当前勾选不满足用券规则。" + summary();
        }
    }
}
