package com.izakji.mccreatorutils.client.explore;

import com.izakji.mccreatorutils.explore.InstalledMod;
import com.izakji.mccreatorutils.explore.ModApiJson;
import com.izakji.mccreatorutils.explore.ModPageResolver;
import com.izakji.mccreatorutils.explore.ModPageUrls;
import com.izakji.mccreatorutils.explore.ResolvedModPage;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Resolves a mod page asynchronously, preferring CurseForge when an API key is configured.
 *
 * @author Izakji
 */
public final class ModLinkResolver {
    private static final String USER_AGENT = "mccreatorutils/1.0.0 (Minecraft 1.21.1; NeoForge 21.1.265)";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final String MODRINTH_FACETS = "%5B%5B%22project_type:mod%22%5D%5D";
    private static final Map<String, CompletableFuture<Optional<ResolvedModPage>>> CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, daemonFactory());
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private ModLinkResolver() {}

    /**
     * Resolves a mod page without blocking the caller.
     *
     * @param mod installed mod metadata
     * @return future containing the page when one is found
     */
    public static CompletableFuture<Optional<ResolvedModPage>> resolve(InstalledMod mod) {
        CompletableFuture<Optional<ResolvedModPage>> existing = CACHE.get(mod.modId());
        if (existing != null) {
            return existing;
        }

        CompletableFuture<Optional<ResolvedModPage>> created =
                CompletableFuture.supplyAsync(() -> resolveSync(mod), EXECUTOR);
        CompletableFuture<Optional<ResolvedModPage>> previous = CACHE.putIfAbsent(mod.modId(), created);
        if (previous != null) {
            created.cancel(false);
            return previous;
        }
        created.whenComplete((result, error) -> {
            if (error != null || result == null || result.isEmpty()) {
                CACHE.remove(mod.modId(), created);
            }
        });
        return created;
    }

    private static Optional<ResolvedModPage> resolveSync(InstalledMod mod) {
        try {
            String apiKey = ExploreModsConfig.curseForgeApiKey();
            if (!apiKey.isBlank()) {
                Optional<ResolvedModPage> curseForge = lookupCurseForge(mod, apiKey);
                if (curseForge.isPresent()) {
                    return curseForge;
                }
            }

            Optional<ResolvedModPage> metadata = ModPageResolver.fromMetadata(mod);
            if (metadata.isPresent()) {
                return metadata;
            }

            Optional<ResolvedModPage> modrinth = lookupModrinth(mod);
            if (modrinth.isPresent()) {
                return modrinth;
            }
        } catch (RuntimeException failure) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static Optional<ResolvedModPage> lookupCurseForge(InstalledMod mod, String apiKey) {
        String search = URLEncoder.encode(mod.displayName(), StandardCharsets.UTF_8);
        Optional<String> response = get(
                URI.create("https://api.curseforge.com/v1/mods/search?gameId=432&classId=6&pageSize=20&searchFilter="
                        + search),
                apiKey);
        if (response.isPresent()) {
            Optional<ResolvedModPage> page = ModApiJson.curseForgeResult(response.get(), mod);
            if (page.isPresent()) {
                return page;
            }
        }

        for (String slug : ModPageUrls.slugCandidates(mod)) {
            String encoded = URLEncoder.encode(slug, StandardCharsets.UTF_8);
            Optional<String> slugResponse = get(
                    URI.create("https://api.curseforge.com/v1/mods/search?gameId=432&classId=6&slug=" + encoded),
                    apiKey);
            if (slugResponse.isPresent()) {
                Optional<ResolvedModPage> page = ModApiJson.curseForgeResult(slugResponse.get(), mod);
                if (page.isPresent()) {
                    return page;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<ResolvedModPage> lookupModrinth(InstalledMod mod) {
        for (String slug : ModPageUrls.slugCandidates(mod)) {
            String encoded = URLEncoder.encode(slug, StandardCharsets.UTF_8);
            Optional<String> response = get(URI.create("https://api.modrinth.com/v2/project/" + encoded), "");
            if (response.isPresent()) {
                Optional<ResolvedModPage> page = ModApiJson.modrinthProjectResult(response.get());
                if (page.isPresent()) {
                    return page;
                }
            }
        }

        String query = URLEncoder.encode(mod.displayName(), StandardCharsets.UTF_8);
        Optional<String> response = get(
                URI.create("https://api.modrinth.com/v2/search?query=" + query + "&limit=10&facets=" + MODRINTH_FACETS),
                "");
        return response.flatMap(json -> ModApiJson.modrinthSearchResult(json, mod));
    }

    private static Optional<String> get(URI uri, String apiKey) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("x-api-key", apiKey);
        }

        try {
            HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return Optional.of(response.body());
            }
        } catch (IOException failure) {
            return Optional.empty();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static ThreadFactory daemonFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "mccreatorutils-mod-link");
            thread.setDaemon(true);
            return thread;
        };
    }
}
