package com.izakji.mccreatorutils.explore;

/**
 * Display labels for Explore Mods selectors.
 *
 * @author Izakji
 */
public final class ModLabels {
    private ModLabels() {}

    /**
     * Returns the selector label for a mod, including the stable mod ID.
     *
     * @param mod installed mod, or null for the unselected state
     * @return display name and mod ID, or an empty string
     */
    public static String label(InstalledMod mod) {
        return mod == null ? "" : mod.displayName() + " (" + mod.modId() + ")";
    }
}
