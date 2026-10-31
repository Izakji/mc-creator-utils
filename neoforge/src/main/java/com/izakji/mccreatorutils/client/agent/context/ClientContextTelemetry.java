package com.izakji.mccreatorutils.client.agent.context;

import com.izakji.mccreatorutils.McCreatorUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Feeds the low-frequency client diagnostics samplers.
 *
 * @author Izakji
 */
@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)
public final class ClientContextTelemetry {
    private ClientContextTelemetry() {}

    @SubscribeEvent
    static void onClientTickPre(ClientTickEvent.Pre event) {
        TickDiagnostics.instance().beginTick();
    }

    @SubscribeEvent
    static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        TickDiagnostics.instance().endTick();
        FrameDiagnostics.instance().sampleIfDue(minecraft);
        SystemDiagnostics.instance().sampleIfDue();
        NetworkDiagnostics.instance().sampleIfDue(minecraft);
    }
}
