package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.client.registry.RegistryKind;
import com.izakji.mccreatorutils.client.registry.RegistryPicker;
import com.izakji.mccreatorutils.dimension.DimensionCommands;
import com.izakji.mccreatorutils.session.CreatorToolSession;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Dimension browser and teleport controls.
 *
 * @author Izakji
 */
public final class DimensionsToolPanel extends UIElement {
    private final CreatorToolSession session = CreatorToolSession.instance();
    private final CommandStatusLabel status = new CommandStatusLabel();
    private final Button teleportButton = new Button();
    private final RegistryPicker picker;

    public DimensionsToolPanel() {
        this.layout(layout -> layout.widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(5));

        var description = new Label();
        description.setText(Component.translatable("tool.mccreatorutils.dimensions.description"));
        addChild(description);

        picker = new RegistryPicker(RegistryKind.DIMENSION, session.dimensionSelection(), entry -> {
            session.setDimensionSelection(entry);
            updateTeleportButton();
        });
        addChild(picker);
        if (!picker.hasGroups()) {
            var notice = new Label();
            notice.setText(Component.translatable("tool.mccreatorutils.dimensions.unavailable"));
            notice.textStyle(style -> style.textColor(ColorPattern.YELLOW.color));
            addChild(notice);
        }

        teleportButton.setText(Component.translatable("tool.mccreatorutils.dimensions.teleport"));
        teleportButton.layout(layout -> layout.widthPercent(100).height(16));
        teleportButton.setOnClick(event -> teleport());
        addChild(teleportButton);

        addChild(status);
        addEventListener(UIEvents.TICK, event -> updateTeleportButton());
        updateTeleportButton();
    }

    private void teleport() {
        ResourceLocation dimension = picker.selectedEntry();
        if (dimension == null) {
            return;
        }
        status.runCommand(DimensionCommands.teleport(dimension));
    }

    private void updateTeleportButton() {
        teleportButton.setActive(picker.selectedEntry() != null);
    }
}
