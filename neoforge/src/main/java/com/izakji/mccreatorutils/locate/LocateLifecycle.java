package com.izakji.mccreatorutils.locate;

import com.izakji.mccreatorutils.McCreatorUtils;
import com.izakji.mccreatorutils.locate.engine.LocateService;
import com.izakji.mccreatorutils.locate.engine.LocateSettings;
import java.time.Duration;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server lifecycle and tick watchdog for the locate service.
 *
 * @author Izakji
 */
@EventBusSubscriber(modid = McCreatorUtils.MOD_ID)
public final class LocateLifecycle {
    private static volatile LocateService service;

    private LocateLifecycle() {}

    public static LocateService service() {
        return service;
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        LocateService previous = service;
        if (previous != null) {
            previous.close();
        }

        LocateSettings settings = LocateConfig.settings();
        service = new LocateService(
                settings.biomeThreads(),
                settings.maxQueuedSearches(),
                Duration.ofSeconds(settings.resultCacheSeconds()),
                System::currentTimeMillis);
        McCreatorUtils.LOGGER.info(
                "Async locate service started with {} biome threads and {} queued searches",
                settings.biomeThreads(),
                settings.maxQueuedSearches());
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        LocateService current = service;
        service = null;
        if (current != null) {
            current.close();
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        LocateService current = service;
        if (current != null) {
            current.tick(System.currentTimeMillis());
        }
    }

    @SubscribeEvent
    static void onLevelUnload(LevelEvent.Unload event) {
        LocateService current = service;
        if (current != null && event.getLevel() instanceof ServerLevel level) {
            current.cancelDimension(level.dimension());
        }
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        LocateService current = service;
        if (current != null) {
            current.clearCache();
        }
    }
}
