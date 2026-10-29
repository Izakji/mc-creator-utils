package com.izakji.mccreatorutils.explore;

import java.util.Optional;

/**
 * Chooses a platform page from author-provided mod metadata.
 *
 * @author Izakji
 */
public final class ModPageResolver {
    private ModPageResolver() {}

    /**
     * Returns the first CurseForge metadata page, otherwise the first Modrinth metadata page.
     *
     * @param mod installed mod metadata
     * @return preferred metadata page, or empty when none is supported
     */
    public static Optional<ResolvedModPage> fromMetadata(InstalledMod mod) {
        for (String url : mod.metadataUrls()) {
            Optional<ResolvedModPage> page = ModPageUrls.parse(url);
            if (page.isPresent() && page.orElseThrow().platform() == ModPlatform.CURSEFORGE) {
                return page;
            }
        }
        for (String url : mod.metadataUrls()) {
            Optional<ResolvedModPage> page = ModPageUrls.parse(url);
            if (page.isPresent() && page.orElseThrow().platform() == ModPlatform.MODRINTH) {
                return page;
            }
        }
        return Optional.empty();
    }
}
