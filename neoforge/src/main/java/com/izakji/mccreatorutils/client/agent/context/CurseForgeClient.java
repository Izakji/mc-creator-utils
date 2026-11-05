package com.izakji.mccreatorutils.client.agent.context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Async, read-only CurseForge REST client for agent tools.
 *
 * @author Izakji
 */
public final class CurseForgeClient {
    private static final String BASE_URL = "https://api.curseforge.com";
    private static final String USER_AGENT = "CreatorUtils";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Executor ioExecutor;
    private final Supplier<String> apiKey;

    public CurseForgeClient(Executor ioExecutor, Supplier<String> apiKey) {
        this.ioExecutor = ioExecutor;
        this.apiKey = apiKey;
    }

    public CompletableFuture<String> search(
            String query,
            String slug,
            String gameVersion,
            int modLoader,
            int categoryId,
            int sortField,
            int limit,
            int index) {
        StringBuilder path = new StringBuilder("/v1/mods/search?gameId=432&classId=6");
        append(path, "searchFilter", query);
        append(path, "slug", slug);
        append(path, "gameVersion", gameVersion);
        if (modLoader > 0) {
            path.append("&modLoaderType=").append(modLoader);
        }
        if (categoryId > 0) {
            path.append("&categoryId=").append(categoryId);
        }
        path.append("&sortField=").append(sortField).append("&sortOrder=desc");
        path.append("&index=")
                .append(clamp(index, 0, 10_000))
                .append("&pageSize=")
                .append(clamp(limit, 1, 20));
        return getJson(path.toString(), CurseForgeReport::search);
    }

    public CompletableFuture<String> mod(long modId, boolean includeDescription) {
        String path = "/v1/mods/" + modId;
        CompletableFuture<String> base = getJson(path, root -> CurseForgeReport.mod(root, false, ""));
        if (!includeDescription) {
            return base;
        }
        return base.thenCompose(text -> getText(
                        path + "/description?stripped=true",
                        description -> text + "\n" + CurseForgeReport.modDescription(description))
                .exceptionally(failure -> text));
    }

    public CompletableFuture<String> files(long modId, String gameVersion, int modLoader, int limit, int index) {
        StringBuilder path = new StringBuilder("/v1/mods/").append(modId).append("/files?");
        boolean first = true;
        if (gameVersion != null && !gameVersion.isBlank()) {
            path.append("gameVersion=").append(encode(gameVersion));
            first = false;
        }
        if (modLoader > 0) {
            if (!first) {
                path.append('&');
            }
            path.append("modLoaderType=").append(modLoader);
            first = false;
        }
        if (!first) {
            path.append('&');
        }
        path.append("index=")
                .append(clamp(index, 0, 10_000))
                .append("&pageSize=")
                .append(clamp(limit, 1, 20));
        return getJson(path.toString(), CurseForgeReport::files);
    }

    public CompletableFuture<String> file(long modId, long fileId, boolean includeChangelog) {
        String path = "/v1/mods/" + modId + "/files/" + fileId;
        CompletableFuture<String> base = getJson(path, root -> CurseForgeReport.file(root, false, ""));
        if (!includeChangelog) {
            return base;
        }
        return base.thenCompose(
                text -> getText(path + "/changelog", changelog -> text + "\n" + CurseForgeReport.changelog(changelog))
                        .exceptionally(failure -> text));
    }

    private CompletableFuture<String> getJson(String path, Function<JsonObject, String> formatter) {
        return CompletableFuture.supplyAsync(
                () -> {
                    String key = readKey();
                    if (key == null) {
                        return missingKey();
                    }
                    try {
                        HttpResponse<String> response = send(path, key);
                        if (response.statusCode() != 200) {
                            return statusError(response.statusCode());
                        }
                        JsonObject root =
                                JsonParser.parseString(response.body()).getAsJsonObject();
                        return formatter.apply(root);
                    } catch (IOException failure) {
                        return "CurseForge request failed: network error.";
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return "CurseForge request interrupted.";
                    } catch (RuntimeException malformed) {
                        return "CurseForge returned an unreadable response.";
                    }
                },
                ioExecutor);
    }

    private CompletableFuture<String> getText(String path, Function<String, String> formatter) {
        return CompletableFuture.supplyAsync(
                () -> {
                    String key = readKey();
                    if (key == null) {
                        return missingKey();
                    }
                    try {
                        HttpResponse<String> response = send(path, key);
                        if (response.statusCode() != 200) {
                            return statusError(response.statusCode());
                        }
                        JsonObject root =
                                JsonParser.parseString(response.body()).getAsJsonObject();
                        String data = root.has("data") && root.get("data").isJsonPrimitive()
                                ? root.get("data").getAsString()
                                : "";
                        return formatter.apply(data);
                    } catch (IOException failure) {
                        return "CurseForge request failed: network error.";
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return "CurseForge request interrupted.";
                    } catch (RuntimeException malformed) {
                        return "CurseForge returned an unreadable response.";
                    }
                },
                ioExecutor);
    }

    private String readKey() {
        try {
            String key = apiKey.get();
            return key == null || key.isBlank() ? null : key.strip();
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    private static HttpResponse<String> send(String path, String key) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .header("x-api-key", key)
                .GET()
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static boolean isError(String text) {
        return text != null
                && (text.startsWith("CurseForge API key")
                        || text.startsWith("CurseForge request")
                        || text.startsWith("CurseForge rejected")
                        || text.startsWith("CurseForge rate")
                        || text.startsWith("CurseForge resource")
                        || text.startsWith("CurseForge returned"));
    }

    private static String missingKey() {
        return "CurseForge API key is not configured. Set CURSEFORGE_API_KEY or edit "
                + "config/mccreatorutils-client.toml (exploreMods.curseForgeApiKey).";
    }

    private static String statusError(int status) {
        return switch (status) {
            case 403 -> "CurseForge rejected the API key (HTTP 403).";
            case 404 -> "CurseForge resource not found (HTTP 404).";
            case 429 -> "CurseForge rate limit reached (HTTP 429). Try again later.";
            default -> "CurseForge request failed (HTTP " + status + ").";
        };
    }

    private static void append(StringBuilder path, String name, String value) {
        if (value != null && !value.isBlank()) {
            path.append('&').append(name).append('=').append(encode(value));
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value.strip(), StandardCharsets.UTF_8);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
