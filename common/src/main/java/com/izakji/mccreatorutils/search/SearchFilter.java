package com.izakji.mccreatorutils.search;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Case-insensitive substring filtering shared by searchable selection controls.
 *
 * @author Izakji
 */
public final class SearchFilter {
    private SearchFilter() {}

    public static <T> List<T> filter(List<T> values, String query, Function<T, String> text) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(text, "text");
        if (query == null || query.isBlank()) {
            return List.copyOf(values);
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> {
                    String candidate = text.apply(value);
                    return candidate != null
                            && candidate.toLowerCase(Locale.ROOT).contains(needle);
                })
                .toList();
    }
}
