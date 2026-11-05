package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formats CurseForge API responses for the agent.
 *
 * @author Izakji
 */
final class CurseForgeReport {
    private static final int MAX_OUTPUT = 24 * 1024;
    private static final int MAX_SUMMARY = 240;
    private static final int MAX_DESCRIPTION = 8 * 1024;
    private static final int MAX_LATEST_FILES = 5;

    private CurseForgeReport() {}

    static String search(JsonObject root) {
        JsonArray data = array(root, "data");
        JsonObject pagination = object(root, "pagination");
        long total = number(pagination, "totalCount");
        if (data == null || data.isEmpty()) {
            return "No CurseForge mods matched.";
        }
        StringBuilder report = new StringBuilder();
        report.append("CurseForge mods (")
                .append(data.size())
                .append(" of ")
                .append(total > 0 ? total : data.size())
                .append("):\n");
        for (JsonElement element : data) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject mod = element.getAsJsonObject();
            report.append("- ")
                    .append(text(mod, "name"))
                    .append(" | id=")
                    .append(number(mod, "id"))
                    .append(" | slug=")
                    .append(text(mod, "slug"))
                    .append(" | authors=")
                    .append(authors(array(mod, "authors")))
                    .append(" | downloads=")
                    .append(number(mod, "downloadCount"))
                    .append('\n');
            String summary = cut(text(mod, "summary"), MAX_SUMMARY);
            if (!summary.isBlank()) {
                report.append("  ").append(summary).append('\n');
            }
            String url = pageUrl(mod);
            if (!url.isBlank()) {
                report.append("  ").append(url).append('\n');
            }
            JsonArray indexes = array(mod, "latestFilesIndexes");
            if (indexes != null && !indexes.isEmpty() && indexes.get(0).isJsonObject()) {
                JsonObject latest = indexes.get(0).getAsJsonObject();
                report.append("  latest: fileId=")
                        .append(number(latest, "fileId"))
                        .append(" | ")
                        .append(text(latest, "filename"))
                        .append(" | ")
                        .append(text(latest, "gameVersion"))
                        .append(" | ")
                        .append(text(latest, "modLoader"))
                        .append('\n');
            }
        }
        return bounded(report.toString());
    }

    static String mod(JsonObject root, boolean includeDescription, String description) {
        JsonObject mod = object(root, "data");
        if (mod == null) {
            return "CurseForge mod not found.";
        }
        StringBuilder report = new StringBuilder();
        report.append("CurseForge mod ")
                .append(number(mod, "id"))
                .append(": ")
                .append(text(mod, "name"))
                .append(" (")
                .append(text(mod, "slug"))
                .append(")\n");
        report.append("Authors: ").append(authors(array(mod, "authors"))).append('\n');
        report.append("Downloads: ").append(number(mod, "downloadCount")).append('\n');
        report.append("Updated: ").append(text(mod, "dateModified")).append('\n');
        report.append("Categories: ")
                .append(categories(array(mod, "categories")))
                .append('\n');
        String summary = cut(text(mod, "summary"), MAX_SUMMARY);
        if (!summary.isBlank()) {
            report.append("Summary: ").append(summary).append('\n');
        }
        String links = links(mod);
        if (!links.isBlank()) {
            report.append("Links: ").append(links).append('\n');
        }
        JsonArray latest = array(mod, "latestFiles");
        if (latest != null && !latest.isEmpty()) {
            report.append("Latest files:\n");
            int shown = Math.min(MAX_LATEST_FILES, latest.size());
            for (int index = 0; index < shown; index++) {
                JsonElement element = latest.get(index);
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject file = element.getAsJsonObject();
                report.append("- id=")
                        .append(number(file, "id"))
                        .append(" | ")
                        .append(text(file, "displayName"))
                        .append(" | ")
                        .append(text(file, "fileName"))
                        .append(" | ")
                        .append(releaseType(number(file, "releaseType")))
                        .append(" | ")
                        .append(join(array(file, "gameVersions")))
                        .append('\n');
            }
        }
        if (includeDescription) {
            String body = stripHtml(description);
            if (!body.isBlank()) {
                report.append("Description:\n")
                        .append(cut(body, MAX_DESCRIPTION))
                        .append('\n');
            }
        }
        return bounded(report.toString());
    }

    static String files(JsonObject root) {
        JsonArray data = array(root, "data");
        JsonObject pagination = object(root, "pagination");
        long total = number(pagination, "totalCount");
        if (data == null || data.isEmpty()) {
            return "No CurseForge files matched.";
        }
        StringBuilder report = new StringBuilder();
        report.append("CurseForge files (")
                .append(data.size())
                .append(" of ")
                .append(total > 0 ? total : data.size())
                .append("):\n");
        for (JsonElement element : data) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject file = element.getAsJsonObject();
            report.append("- fileId=")
                    .append(number(file, "id"))
                    .append(" | ")
                    .append(text(file, "displayName"))
                    .append(" | ")
                    .append(text(file, "fileName"))
                    .append(" | ")
                    .append(releaseType(number(file, "releaseType")))
                    .append(" | ")
                    .append(text(file, "fileDate"))
                    .append(" | ")
                    .append(join(array(file, "gameVersions")))
                    .append('\n');
            report.append("  downloads=").append(number(file, "downloadCount")).append('\n');
            String downloadUrl = text(file, "downloadUrl");
            if (!downloadUrl.isBlank()) {
                report.append("  download: ").append(downloadUrl).append('\n');
            }
            JsonArray dependencies = array(file, "dependencies");
            if (dependencies != null && !dependencies.isEmpty()) {
                List<String> values = new ArrayList<>();
                for (JsonElement dependency : dependencies) {
                    if (dependency.isJsonObject()) {
                        JsonObject value = dependency.getAsJsonObject();
                        values.add(number(value, "modId") + " (" + relation(number(value, "relationType")) + ")");
                    }
                }
                report.append("  dependencies: ")
                        .append(String.join(", ", values))
                        .append('\n');
            }
        }
        return bounded(report.toString());
    }

    static String file(JsonObject root, boolean includeChangelog, String changelog) {
        JsonObject file = object(root, "data");
        if (file == null) {
            return "CurseForge file not found.";
        }
        StringBuilder report = new StringBuilder();
        report.append("CurseForge file ")
                .append(number(file, "id"))
                .append(" for mod ")
                .append(number(file, "modId"))
                .append(": ")
                .append(text(file, "displayName"))
                .append('\n');
        report.append("FileName: ").append(text(file, "fileName")).append('\n');
        report.append("Release: ")
                .append(releaseType(number(file, "releaseType")))
                .append('\n');
        report.append("Date: ").append(text(file, "fileDate")).append('\n');
        report.append("Size: ").append(number(file, "fileLength")).append(" bytes\n");
        report.append("Versions: ").append(join(array(file, "gameVersions"))).append('\n');
        report.append("Downloads: ").append(number(file, "downloadCount")).append('\n');
        JsonArray hashes = array(file, "hashes");
        if (hashes != null && !hashes.isEmpty()) {
            List<String> values = new ArrayList<>();
            for (JsonElement hash : hashes) {
                if (hash.isJsonObject()) {
                    JsonObject value = hash.getAsJsonObject();
                    values.add(hashAlgo(number(value, "algo")) + "=" + text(value, "value"));
                }
            }
            report.append("Hashes: ").append(String.join(", ", values)).append('\n');
        }
        JsonArray dependencies = array(file, "dependencies");
        if (dependencies != null && !dependencies.isEmpty()) {
            List<String> values = new ArrayList<>();
            for (JsonElement dependency : dependencies) {
                if (dependency.isJsonObject()) {
                    JsonObject value = dependency.getAsJsonObject();
                    values.add(number(value, "modId") + " (" + relation(number(value, "relationType")) + ")");
                }
            }
            report.append("Dependencies: ").append(String.join(", ", values)).append('\n');
        }
        String downloadUrl = text(file, "downloadUrl");
        if (!downloadUrl.isBlank()) {
            report.append("Download: ").append(downloadUrl).append('\n');
        }
        if (includeChangelog) {
            String body = stripHtml(changelog);
            if (!body.isBlank()) {
                report.append("Changelog:\n").append(cut(body, MAX_DESCRIPTION)).append('\n');
            }
        }
        return bounded(report.toString());
    }

    static String modDescription(String html) {
        String body = stripHtml(html);
        return body.isBlank() ? "" : "Description:\n" + cut(body, MAX_DESCRIPTION);
    }

    static String changelog(String html) {
        String body = stripHtml(html);
        return body.isBlank() ? "" : "Changelog:\n" + cut(body, MAX_DESCRIPTION);
    }

    static String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String text = html.replaceAll("(?is)<(script|style).*?</\\1>", " ");
        text = text.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?i)</p>", "\n");
        text = text.replaceAll("<[^>]+>", " ");
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        return text.replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").strip();
    }

    private static String pageUrl(JsonObject mod) {
        JsonObject links = object(mod, "links");
        String website = text(links, "websiteUrl");
        if (!website.isBlank()) {
            return website;
        }
        String slug = text(mod, "slug");
        return slug.isBlank() ? "" : "https://www.curseforge.com/minecraft/mc-mods/" + slug;
    }

    private static String links(JsonObject mod) {
        JsonObject links = object(mod, "links");
        if (links == null) {
            return "";
        }
        List<String> values = new ArrayList<>();
        addLink(values, "website", text(links, "websiteUrl"));
        addLink(values, "wiki", text(links, "wikiUrl"));
        addLink(values, "source", text(links, "sourceUrl"));
        addLink(values, "issues", text(links, "issuesUrl"));
        return String.join(", ", values);
    }

    private static void addLink(List<String> values, String label, String value) {
        if (value != null && !value.isBlank()) {
            values.add(label + "=" + value);
        }
    }

    private static String authors(JsonArray authors) {
        if (authors == null) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (JsonElement author : authors) {
            if (author.isJsonObject()) {
                String name = text(author.getAsJsonObject(), "name");
                if (!name.isBlank()) {
                    values.add(name);
                }
            }
        }
        return String.join(", ", values);
    }

    private static String categories(JsonArray categories) {
        if (categories == null) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (JsonElement category : categories) {
            if (category.isJsonObject()) {
                String name = text(category.getAsJsonObject(), "name");
                if (!name.isBlank()) {
                    values.add(name);
                }
            }
        }
        return String.join(", ", values);
    }

    private static String join(JsonArray values) {
        if (values == null) {
            return "";
        }
        List<String> result = new ArrayList<>();
        for (JsonElement value : values) {
            if (value.isJsonPrimitive()) {
                result.add(value.getAsString());
            }
        }
        return String.join(", ", result);
    }

    private static String releaseType(long value) {
        return switch ((int) value) {
            case 1 -> "release";
            case 2 -> "beta";
            case 3 -> "alpha";
            default -> "unknown";
        };
    }

    private static String relation(long value) {
        return switch ((int) value) {
            case 1 -> "embedded";
            case 2 -> "optional";
            case 3 -> "required";
            case 4 -> "tool";
            case 5 -> "incompatible";
            case 6 -> "include";
            default -> "unknown";
        };
    }

    private static String hashAlgo(long value) {
        return value == 1 ? "sha1" : value == 2 ? "md5" : "hash";
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null) {
            return null;
        }
        JsonElement value = parent.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonObject parent, String key) {
        if (parent == null) {
            return null;
        }
        JsonElement value = parent.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    private static String text(JsonObject object, String key) {
        if (object == null) {
            return "";
        }
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() || !value.isJsonPrimitive() ? "" : value.getAsString();
    }

    private static long number(JsonObject object, String key) {
        if (object == null) {
            return 0;
        }
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return 0;
        }
        try {
            return value.getAsLong();
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }

    private static String cut(String value, int max) {
        String text = value == null ? "" : value.strip();
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private static String bounded(String value) {
        return value.length() <= MAX_OUTPUT ? value : value.substring(0, MAX_OUTPUT);
    }

    static String normalizeModLoader(String value) {
        if (value == null || value.isBlank()) {
            return "6";
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "any", "0" -> "0";
            case "forge", "1" -> "1";
            case "fabric", "4" -> "4";
            case "quilt", "5" -> "5";
            case "neoforge", "neo", "6" -> "6";
            default -> "6";
        };
    }

    static String normalizeSort(String value) {
        if (value == null || value.isBlank()) {
            return "2";
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "featured", "1" -> "1";
            case "popularity", "popular", "2" -> "2";
            case "updated", "3" -> "3";
            case "name", "4" -> "4";
            case "author", "5" -> "5";
            case "downloads", "6" -> "6";
            case "category", "7" -> "7";
            case "gameversion", "8" -> "8";
            case "released", "11" -> "11";
            case "rating", "12" -> "12";
            default -> "2";
        };
    }
}
