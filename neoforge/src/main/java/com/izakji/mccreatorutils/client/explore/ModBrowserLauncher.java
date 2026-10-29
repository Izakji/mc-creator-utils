package com.izakji.mccreatorutils.client.explore;

import com.izakji.mccreatorutils.explore.ResolvedModPage;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;

/**
 * Opens the optional Rinku browser without forcing Rinku classes onto the tool panel path.
 *
 * @author Izakji
 */
public final class ModBrowserLauncher {
    private ModBrowserLauncher() {}

    /**
     * Opens a page when Rinku is installed; otherwise does nothing.
     *
     * @param page resolved mod page
     */
    public static void open(ResolvedModPage page) {
        if (!ModList.get().isLoaded("rinku")) {
            return;
        }
        Minecraft.getInstance().setScreen(new ModBrowserScreen(page.uri().toString()));
    }
}
