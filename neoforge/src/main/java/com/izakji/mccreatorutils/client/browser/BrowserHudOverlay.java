package com.izakji.mccreatorutils.client.browser;

import com.izakji.mccreatorutils.McCreatorUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Draws the floating browser over normal gameplay.
 *
 * @author Izakji
 */
@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)
public final class BrowserHudOverlay {
    private BrowserHudOverlay() {}

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) {
            return;
        }
        BrowserWorkspace workspace = BrowserWorkspace.instance();
        if (workspace.hasWindow()) {
            BrowserWorkspaceRenderer.render(event.getGuiGraphics(), workspace, true);
        }
    }
}
