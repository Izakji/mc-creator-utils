package com.izakji.mccreatorutils.client.agent;

import com.izakji.mccreatorutils.McCreatorUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Draws the floating agent chat over normal gameplay.
 *
 * @author Izakji
 */
@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)
public final class AgentChatHudOverlay {
    private AgentChatHudOverlay() {}

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) {
            return;
        }
        AgentChatWindow window = AgentChatWindow.instance();
        if (!window.visible()) {
            return;
        }
        AgentChatRenderer.renderWithHits(
                event.getGuiGraphics(), AgentChatSession.instance(), window.state(), new AgentChatScreenState(), true);
    }
}
