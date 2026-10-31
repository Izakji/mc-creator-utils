package com.izakji.mccreatorutils.client.agent;

import com.izakji.mccreatorutils.client.agent.context.PerformanceReportData;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Screen hosting the visual agent performance report.
 *
 * @author Izakji
 */
public final class AgentReportScreen extends ModularUIScreen {
    private final Screen previous;

    private AgentReportScreen(
            PerformanceReportData data, Screen previous, Supplier<CompletableFuture<PerformanceReportData>> source) {
        super(
                AgentReportUi.create(data, () -> refresh(source, previous), () -> restore(previous)),
                Component.literal("Performance Report"));
        this.previous = previous;
    }

    /**
     * Opens the report over the current screen.
     *
     * @param data report snapshot
     * @param source refresh source
     */
    public static void open(PerformanceReportData data, Supplier<CompletableFuture<PerformanceReportData>> source) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new AgentReportScreen(data, minecraft.screen, source));
    }

    @Override
    public void onClose() {
        restore(previous);
    }

    private static void refresh(Supplier<CompletableFuture<PerformanceReportData>> source, Screen previous) {
        source.get()
                .whenComplete((data, failure) -> Minecraft.getInstance().execute(() -> {
                    if (failure == null && data != null) {
                        Minecraft.getInstance().setScreen(new AgentReportScreen(data, previous, source));
                    }
                }));
    }

    private static void restore(Screen previous) {
        Minecraft.getInstance().setScreen(previous);
    }
}
