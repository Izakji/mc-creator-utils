package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.client.registry.RegistryKind;
import com.izakji.mccreatorutils.client.registry.RegistryPicker;
import com.izakji.mccreatorutils.mobs.MobCommands;
import com.izakji.mccreatorutils.session.CreatorToolSession;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Mob browser, summon, and global entity clearing controls.
 *
 * @author Izakji
 */
public final class MobsToolPanel extends UIElement {
    private final CreatorToolSession session = CreatorToolSession.instance();
    private final CommandStatusLabel status = new CommandStatusLabel();
    private final Button summonButton = new Button();
    private final RegistryPicker picker;

    public MobsToolPanel() {
        this.layout(layout -> layout.widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(5));

        var description = new Label();
        description.setText(Component.translatable("tool.mccreatorutils.mobs.description"));
        addChild(description);

        picker = new RegistryPicker(RegistryKind.MOB, session.mobSelection(), entry -> {
            session.setMobSelection(entry);
            updateSummonButton();
        });
        addChild(picker);

        summonButton.setText(Component.translatable("tool.mccreatorutils.mobs.summon"));
        summonButton.layout(layout -> layout.widthPercent(100).height(16));
        summonButton.setOnClick(event -> {
            ResourceLocation mob = picker.selectedEntry();
            if (mob != null) {
                status.runCommand(MobCommands.summon(mob));
            }
        });
        addChild(summonButton);

        var killButton = new Button();
        killButton.setText(Component.translatable("tool.mccreatorutils.mobs.kill_all"));
        killButton.layout(layout -> layout.widthPercent(100).height(16));
        killButton.setOnClick(event -> status.runCommand(MobCommands.killAllExceptPlayers()));
        addChild(killButton);

        addChild(status);
        updateSummonButton();
    }

    private void updateSummonButton() {
        summonButton.setActive(picker.selectedEntry() != null);
    }
}
