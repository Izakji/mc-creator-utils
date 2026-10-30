package com.izakji.agent.util;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Removes configured secrets and common authorization patterns from text.
 *
 * @author Izakji
 */
public final class Redactor {
    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)\\S+");
    private static final Pattern HEADER = Pattern.compile(
            "(?i)((?:authorization|api[_-]?key|x-api-key)\\s*[:=]\\s*)[^\\s,;]+");
    private final List<String> secrets;

    public Redactor(Set<String> secrets) {
        this.secrets = Objects.requireNonNullElse(secrets, Set.<String>of()).stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    public String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        String result = text;
        for (String secret : secrets) {
            result = result.replace(secret, "[REDACTED]");
        }
        result = BEARER.matcher(result).replaceAll("$1[REDACTED]");
        result = HEADER.matcher(result).replaceAll("$1[REDACTED]");
        return result;
    }
}
