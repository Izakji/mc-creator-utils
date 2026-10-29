package com.izakji.mccreatorutils.browser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

/**
 * Normalizes supported browser address-bar input.
 *
 * @author Izakji
 */
public final class BrowserUrlInput {
    private BrowserUrlInput() {}

    /**
     * Normalizes a supported URL, adding https when the scheme is missing.
     *
     * @param raw address-bar input
     * @return normalized URL, or empty when unsupported or malformed
     */
    public static Optional<String> normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = raw.trim();
        if (value.equalsIgnoreCase("about:blank")) {
            return Optional.of("about:blank");
        }
        if (!value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            value = "https://" + value;
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || host.isBlank()) {
                return Optional.empty();
            }
            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return Optional.empty();
            }
            return Optional.of(uri.toString());
        } catch (URISyntaxException malformed) {
            return Optional.empty();
        }
    }
}
