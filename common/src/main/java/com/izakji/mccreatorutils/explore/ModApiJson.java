package com.izakji.mccreatorutils.explore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Optional;

/**
 * Extracts exact mod pages from CurseForge and Modrinth API responses.
 *
 * @author Izakji
 */
public final class ModApiJson {
    private ModApiJson() {}

    /**
     * Extracts an exact CurseForge match from a mod search response.
     *
     * @param json API response body
     * @param mod installed mod metadata
     * @return exact page match, or empty
     */
    public static Optional<ResolvedModPage> curseForgeResult(String json, InstalledMod mod) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray data = root.getAsJsonArray("data");
            if (data == null) {
                return Optional.empty();
            }
            List<String> candidates = ModPageUrls.slugCandidates(mod);
            for (JsonElement element : data) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject project = element.getAsJsonObject();
                String slug = string(project, "slug");
                String name = string(project, "name");
                if (!matches(slug, name, candidates, mod.displayName())) {
                    continue;
                }
                Optional<ResolvedModPage> link = pageFromLinks(project, ModPlatform.CURSEFORGE);
                if (link.isPresent()) {
                    return link;
                }
                Optional<ResolvedModPage> generated =
                        ModPageUrls.parse("https://www.curseforge.com/minecraft/mc-mods/" + slug);
                if (generated.isPresent()) {
                    return generated;
                }
            }
        } catch (RuntimeException malformed) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * Extracts a Modrinth project page from a project response.
     *
     * @param json API response body
     * @return project page, or empty
     */
    public static Optional<ResolvedModPage> modrinthProjectResult(String json) {
        try {
            JsonObject project = JsonParser.parseString(json).getAsJsonObject();
            String slug = string(project, "slug");
            if (slug == null || slug.isBlank()) {
                return Optional.empty();
            }
            return ModPageUrls.parse("https://modrinth.com/mod/" + slug);
        } catch (RuntimeException malformed) {
            return Optional.empty();
        }
    }

    /**
     * Extracts an exact Modrinth search hit.
     *
     * @param json search response body
     * @param mod installed mod metadata
     * @return exact page match, or empty
     */
    public static Optional<ResolvedModPage> modrinthSearchResult(String json, InstalledMod mod) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray hits = root.getAsJsonArray("hits");
            if (hits == null) {
                return Optional.empty();
            }
            List<String> candidates = ModPageUrls.slugCandidates(mod);
            for (JsonElement element : hits) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject project = element.getAsJsonObject();
                String slug = string(project, "slug");
                String title = string(project, "title");
                if (!matches(slug, title, candidates, mod.displayName())) {
                    continue;
                }
                Optional<ResolvedModPage> page = ModPageUrls.parse("https://modrinth.com/mod/" + slug);
                if (page.isPresent()) {
                    return page;
                }
            }
        } catch (RuntimeException malformed) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static Optional<ResolvedModPage> pageFromLinks(JsonObject project, ModPlatform platform) {
        JsonObject links = project.getAsJsonObject("links");
        if (links == null) {
            return Optional.empty();
        }
        String url = string(links, "websiteUrl");
        if (url == null) {
            return Optional.empty();
        }
        return ModPageUrls.parse(url).filter(page -> page.platform() == platform);
    }

    private static boolean matches(String slug, String title, List<String> candidates, String displayName) {
        return (slug != null && candidates.stream().anyMatch(candidate -> candidate.equalsIgnoreCase(slug)))
                || (title != null && displayName != null && title.equalsIgnoreCase(displayName));
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }
}
