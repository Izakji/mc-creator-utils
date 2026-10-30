package com.izakji.mccreatorutils.locate;

import com.izakji.mccreatorutils.McCreatorUtils;
import com.izakji.mccreatorutils.locate.engine.LocateService;
import com.izakji.mccreatorutils.locate.engine.LocateSettings;
import java.time.Duration;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
    private static volatile LocateCoordinator coordinator;

    private LocateLifecycle() {}

    public static LocateService service() {
        return service;
    }

    public static LocateCoordinator coordinator() {
        return coordinator;
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        LocateService previous = service;
        if (previous != null) {
            previous.close();
        }
        BiomeBackendFactory.clearCache();

        LocateSettings settings = LocateConfig.settings();
        service = new LocateService(
                settings.biomeThreads(),
                settings.maxQueuedSearches(),
                Duration.ofSeconds(settings.resultCacheSeconds()),
                System::currentTimeMillis);
        coordinator = new LocateCoordinator();
        McCreatorUtils.LOGGER.info(
                "Async locate service started with {} biome threads and {} queued searches",
                settings.biomeThreads(),
                settings.maxQueuedSearches());
    }

    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        LocateService current = service;
        service = null;
        coordinator = null;
        if (current != null) {
            current.close();
        }
        BiomeBackendFactory.clearCache();
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
            current.clearCacheDimension(level.dimension());
            BiomeBackendFactory.clearCache();
        }
    }

    @SubscribeEvent
    static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LocateCoordinator current = coordinator;
        if (current != null) {
            current.cancelPlayer(event.getEntity().getUUID());
        }
    }

    public static void onConfigReloading(net.neoforged.fml.event.config.ModConfigEvent.Reloading event) {
        if (!event.getConfig().getSpec().equals(LocateConfig.SPEC)) {
            return;
        }
        LocateService current = service;
        if (current == null) {
            return;
        }
        LocateSettings settings = LocateConfig.settings();
        current.reconfigure(
                settings.biomeThreads(),
                settings.maxQueuedSearches(),
                Duration.ofSeconds(settings.resultCacheSeconds()));
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        LocateService current = service;
        if (current != null) {
            current.cancelAll();
            current.clearCache();
        }
        BiomeBackendFactory.clearCache();
    }
}
