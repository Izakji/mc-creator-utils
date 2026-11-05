package com.izakji.mccreatorutils.client.browser;

import com.izakji.mccreatorutils.McCreatorUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Domain and path patterns blocked in the in-game browser.
 *
 * @author Izakji
 */
final class BrowserAdBlockList {
    private static final List<String> BUILT_IN = List.of(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "googletagservices.com",
            "googletagmanager.com",
            "google-analytics.com",
            "adservice.google.com",
            "amazon-adsystem.com",
            "adsrvr.org",
            "adnxs.com",
            "criteo.com",
            "criteo.net",
            "taboola.com",
            "outbrain.com",
            "pubmatic.com",
            "rubiconproject.com",
            "openx.net",
            "casalemedia.com",
            "moatads.com",
            "scorecardresearch.com",
            "quantserve.com",
            "serving-sys.com",
            "smartadserver.com",
            "teads.tv",
            "zedo.com",
            "revcontent.com",
            "mgid.com",
            "propellerads.com",
            "popads.net",
            "onclickads.net",
            "adform.net",
            "adsafeprotected.com",
            "advertising.com",
            "yieldmo.com",
            "sharethrough.com",
            "triplelift.com",
            "indexexchange.com",
            "contextweb.com",
            "districtm.io",
            "gumgum.com",
            "lijit.com",
            "sovrn.com",
            "spotxchange.com",
            "spotx.tv",
            "tremorhub.com",
            "vidazoo.com",
            "yieldlab.net",
            "adroll.com",
            "pagead",
            "adsbygoogle",
            "/prebid",
            "gpt.js");
    private final List<String> patterns;

    BrowserAdBlockList(List<String> userPatterns) {
        List<String> combined = new ArrayList<>(BUILT_IN);
        if (userPatterns != null) {
            for (String pattern : userPatterns) {
                if (pattern != null && !pattern.isBlank() && !pattern.startsWith("#")) {
                    combined.add(pattern.strip().toLowerCase(Locale.ROOT));
                }
            }
        }
        patterns = List.copyOf(combined);
    }

    static BrowserAdBlockList load() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("mccreatorutils").resolve("browser-blocklist.txt");
        if (!Files.isRegularFile(file)) {
            return new BrowserAdBlockList(List.of());
        }
        try {
            return new BrowserAdBlockList(Files.readAllLines(file));
        } catch (IOException failure) {
            McCreatorUtils.LOGGER.warn("Could not read browser ad block list: {}", file, failure);
            return new BrowserAdBlockList(List.of());
        }
    }

    boolean matches(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String normalized = url.toLowerCase(Locale.ROOT);
        for (String pattern : patterns) {
            if (normalized.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    int ruleCount() {
        return patterns.size();
    }
}
