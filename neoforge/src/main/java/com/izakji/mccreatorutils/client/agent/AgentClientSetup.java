package com.izakji.mccreatorutils.client.agent;

import com.izakji.mccreatorutils.McCreatorUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Loads agent settings and instructions away from the render thread.
 *
 * @author Izakji
 */
@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)
public final class AgentClientSetup {
    private AgentClientSetup() {}

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        try {
            AgentClientService.instance();
        } catch (RuntimeException failure) {
            McCreatorUtils.LOGGER.error("Could not initialize the agent service", failure);
        }
    }
}
