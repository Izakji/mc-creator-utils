package com.izakji.mccreatorutils.explore;

import java.net.URI;
import java.util.Objects;

/**
 * Resolved page for a mod.
 *
 * @param platform platform that hosts the page
 * @param uri canonical project page URI
 * @author Izakji
 */
public record ResolvedModPage(ModPlatform platform, URI uri) {
    public ResolvedModPage {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(uri, "uri");
    }
}
