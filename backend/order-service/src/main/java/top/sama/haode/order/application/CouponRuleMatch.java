package top.sama.haode.order.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 一单只执行命中商品里优先级数字最小的规则。
 * 同优先级多条规则、同一条规则里的多种用法，都是二选一，不能把额度加在一起。
 * 用法是上限：张数可以更少，不能出现该用法里没有的券，也不能超过该用法的张数。
 */
public final class CouponRuleMatch {
    private CouponRuleMatch() {
    }

    public record Item(UUID templateId, int quantity) {}

    public record Option(List<Item> items) {
        public int quantityOf(UUID templateId) {
            int total = 0;
            for (Item item : items) {
                if (templateId.equals(item.templateId())) total += item.quantity();
            }
            return total;
        }

        public boolean covers(Map<UUID, Integer> selection) {
            for (Map.Entry<UUID, Integer> entry : selection.entrySet()) {
                int wanted = entry.getValue() == null ? 0 : entry.getValue();
                if (wanted <= 0) continue;
                if (wanted > quantityOf(entry.getKey())) return false;
            }
            return true;
        }
    }

    public record Rule(
            UUID id,
            String name,
            int priority,
            Set<String> zones,
            Set<String> productIds,
            List<Option> options
    ) {}

    public record CartItem(String productId, String zone) {}

    public static List<Rule> active(List<Rule> rules, List<CartItem> cart) {
        List<Rule> matched = new ArrayList<>();
        for (Rule rule : rules) {
            if (matches(rule, cart)) matched.add(rule);
        }
        if (matched.isEmpty()) return List.of();
        int best = matched.stream().mapToInt(Rule::priority).min().orElseThrow();
        return matched.stream().filter(rule -> rule.priority() == best).toList();
    }

    public static boolean fits(List<Rule> active, Map<UUID, Integer> selection) {
        Map<UUID, Integer> used = used(selection);
        if (used.isEmpty()) return true;
        for (Rule rule : active) {
            for (Option option : rule.options()) {
                if (option.covers(used)) return true;
            }
        }
        return false;
    }

    public static int maxOf(List<Rule> active, UUID templateId) {
        int max = 0;
        for (Rule rule : active) {
            for (Option option : rule.options()) max = Math.max(max, option.quantityOf(templateId));
        }
        return max;
    }

    public static String summary(List<Rule> active, Map<UUID, String> names) {
        if (active.isEmpty()) return "当前商品没有命中用券规则，不能使用优惠券";
        int priority = active.get(0).priority();
        if (active.size() == 1) {
            Rule rule = active.get(0);
            return "本单只执行优先级 " + priority + "「" + rule.name() + "」：" + optionsText(rule, names) + "。用得更少可以，不能叠加";
        }
        List<String> parts = new ArrayList<>();
        for (Rule rule : active) parts.add("「" + rule.name() + "」" + optionsText(rule, names));
        return "本单优先级 " + priority + " 有 " + active.size() + " 条规则，只能满足其中一条，不能叠加。" + String.join("；", parts);
    }

    private static boolean matches(Rule rule, List<CartItem> cart) {
        for (CartItem item : cart) {
            if (item.productId() != null && rule.productIds().contains(item.productId())) return true;
            if (item.zone() != null && !item.zone().isBlank() && rule.zones().contains(item.zone())) return true;
        }
        return false;
    }

    private static Map<UUID, Integer> used(Map<UUID, Integer> selection) {
        Map<UUID, Integer> used = new LinkedHashMap<>();
        if (selection == null) return used;
        selection.forEach((id, quantity) -> {
            if (id != null && quantity != null && quantity > 0) used.put(id, quantity);
        });
        return used;
    }

    private static String optionsText(Rule rule, Map<UUID, String> names) {
        List<String> parts = new ArrayList<>();
        for (Option option : rule.options()) parts.add(optionText(option, names));
        return String.join("，或", parts);
    }

    private static String optionText(Option option, Map<UUID, String> names) {
        List<String> parts = new ArrayList<>();
        for (Item item : option.items()) {
            parts.add(item.quantity() + " 张" + names.getOrDefault(item.templateId(), "优惠券"));
        }
        return String.join(" + ", parts);
    }
}
