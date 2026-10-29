package com.izakji.mccreatorutils.explore;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Recognizes and canonicalizes CurseForge and Modrinth project URLs.
 *
 * @author Izakji
 */
public final class ModPageUrls {
    private static final String CURSEFORGE_HOST = "curseforge.com";
    private static final String MODRINTH_HOST = "modrinth.com";

    private ModPageUrls() {}

    /**
     * Parses a supported platform project URL.
     *
     * @param rawUrl source URL
     * @return canonical project page, or empty when unsupported or malformed
     */
    public static Optional<ResolvedModPage> parse(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException exception) {
            return Optional.empty();
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            return Optional.empty();
        }

        List<String> segments = pathSegments(uri.getPath());
        host = host.toLowerCase(Locale.ROOT);
        if (isHost(host, CURSEFORGE_HOST)) {
            int marker = segments.indexOf("mc-mods");
            if (marker < 0 || marker + 1 >= segments.size()) {
                return Optional.empty();
            }
            return page(
                    "https://www.curseforge.com/minecraft/mc-mods/", segments.get(marker + 1), ModPlatform.CURSEFORGE);
        }
        if (isHost(host, MODRINTH_HOST)) {
            int marker = segments.indexOf("mod");
            if (marker < 0 || marker + 1 >= segments.size()) {
                return Optional.empty();
            }
            return page("https://modrinth.com/mod/", segments.get(marker + 1), ModPlatform.MODRINTH);
        }
        return Optional.empty();
    }

    /**
     * Returns deterministic lookup slugs for a mod, mod ID first.
     *
     * @param mod installed mod metadata
     * @return distinct non-empty slug candidates
     */
    public static List<String> slugCandidates(InstalledMod mod) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, mod.modId());
        addCandidate(candidates, mod.displayName());
        return List.copyOf(candidates);
    }

    static String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("'", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private static void addCandidate(LinkedHashSet<String> candidates, String value) {
        String slug = slugify(value);
        if (!slug.isEmpty()) {
            candidates.add(slug);
        }
    }

    private static Optional<ResolvedModPage> page(String prefix, String slug, ModPlatform platform) {
        if (!slug.matches("[A-Za-z0-9._-]+")) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedModPage(platform, URI.create(prefix + slug)));
    }

    private static boolean isHost(String host, String expected) {
        return host.equals(expected) || host.endsWith("." + expected);
    }

    private static List<String> pathSegments(String path) {
        List<String> segments = new ArrayList<>();
        if (path == null) {
            return segments;
        }
        for (String segment : path.split("/")) {
            if (!segment.isEmpty()) {
                segments.add(segment);
            }
        }
        return segments;
    }
}
