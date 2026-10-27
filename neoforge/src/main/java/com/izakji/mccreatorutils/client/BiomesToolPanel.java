package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.client.registry.RegistryCatalog;
import com.izakji.mccreatorutils.client.registry.RegistryKind;
import com.izakji.mccreatorutils.client.registry.RegistryPicker;
import com.izakji.mccreatorutils.session.CreatorToolSession;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Biomes browser and teleport controls.
 *
 * @author Izakji
 */
public final class BiomesToolPanel extends UIElement {
    private final CreatorToolSession session = CreatorToolSession.instance();
    private final CommandStatusLabel status = new CommandStatusLabel();
    private final Button teleportButton = new Button();
    private final RegistryPicker picker;

    public BiomesToolPanel() {
        this.layout(layout -> layout.widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(5));

        var description = new Label();
        description.setText(Component.translatable("tool.mccreatorutils.biomes.description"));
        addChild(description);

        picker = new RegistryPicker(RegistryKind.BIOME, session.biomeSelection(), entry -> {
            session.setBiomeSelection(entry);
            updateTeleportButton();
        });
        addChild(picker);

        teleportButton.setText(Component.translatable("tool.mccreatorutils.biomes.teleport"));
        teleportButton.layout(layout -> layout.widthPercent(100).height(16));
        teleportButton.setOnClick(event -> teleport());
        addChild(teleportButton);

        addChild(status);
        updateTeleportButton();
    }

    private void teleport() {
        ResourceLocation biome = picker.selectedEntry();
        if (biome == null) {
            return;
        }
        Component targetName = RegistryCatalog.label(RegistryKind.BIOME, biome);
        status.showInfo(Component.translatable("status.mccreatorutils.searching", targetName));
        teleportButton.setActive(false);
        ClientLocateTeleporter.requestBiome(biome, targetName, result -> {
            updateTeleportButton();
            status.showResult(result);
        });
    }

    private void updateTeleportButton() {
        teleportButton.setActive(picker.selectedEntry() != null && !ClientLocateTeleporter.isPending());
    }
}
