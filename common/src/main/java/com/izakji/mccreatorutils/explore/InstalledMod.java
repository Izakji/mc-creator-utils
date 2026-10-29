package com.izakji.mccreatorutils.explore;

import java.util.List;
import java.util.Objects;

/**
 * Metadata for one loaded mod.
 *
 * @param modId stable mod identifier
 * @param displayName human-readable mod name
 * @param version loaded mod version
 * @param metadataUrls author-provided URLs, in declaration order
 * @author Izakji
 */
public record InstalledMod(String modId, String displayName, String version, List<String> metadataUrls) {
    public InstalledMod {
        modId = modId == null ? "" : modId;
        displayName = displayName == null ? "" : displayName;
        version = version == null ? "" : version;
        metadataUrls = metadataUrls == null
                ? List.of()
                : metadataUrls.stream().filter(Objects::nonNull).toList();
    }

    /**
     * Returns the text used by the searchable mod selector.
     *
     * @return display name followed by mod ID
     */
    public String searchText() {
        return (displayName + " " + modId).trim();
    }
}
