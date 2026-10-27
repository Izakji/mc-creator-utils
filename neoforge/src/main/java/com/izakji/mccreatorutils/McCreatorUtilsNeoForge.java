package com.izakji.mccreatorutils;

import com.izakji.mccreatorutils.locate.LocateConfig;
import com.izakji.mccreatorutils.locate.LocateLifecycle;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * NeoForge entrypoint for Creator Utils.
 *
 * @author Izakji
 */
@Mod(McCreatorUtils.MOD_ID)
public final class McCreatorUtilsNeoForge {
    public McCreatorUtilsNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, LocateConfig.SPEC);
        modEventBus.addListener(LocateLifecycle::onConfigReloading);
    }
}
