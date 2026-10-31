package com.izakji.mccreatorutils.client.agent.context;

import com.izakji.mccreatorutils.McCreatorUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Feeds the low-frequency performance sampler.
 *
 * @author Izakji
 */
@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)
public final class ClientContextTelemetry {
    private ClientContextTelemetry() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        PerformanceMonitor.instance().tick(Minecraft.getInstance());
    }
}
