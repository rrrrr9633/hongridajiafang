package top.sama.haode.order.api;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record AdminPage<T>(List<T> items, long total, int page, int size) {
    static <E, T> AdminPage<T> from(Page<E> result, Function<E, T> mapper) {
        return new AdminPage<>(
                result.getContent().stream().map(mapper).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize()
        );
    }
}
