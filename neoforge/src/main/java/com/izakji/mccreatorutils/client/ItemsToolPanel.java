package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.client.registry.RegistryKind;
import com.izakji.mccreatorutils.client.registry.RegistryPicker;
import com.izakji.mccreatorutils.items.GiveCommands;
import com.izakji.mccreatorutils.items.ItemCountInput;
import com.izakji.mccreatorutils.session.CreatorToolSession;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Item browser, give, and destructive clearing controls.
 *
 * @author Izakji
 */
public final class ItemsToolPanel extends UIElement {
    private final CreatorToolSession session = CreatorToolSession.instance();
    private final CommandStatusLabel status = new CommandStatusLabel();
    private final TextField countField = new TextField();
    private final Button giveButton = new Button();
    private final ModelPreview preview = new ModelPreview(ModelPreview.Kind.ITEM);
    private final RegistryPicker picker;

    public ItemsToolPanel() {
        this.layout(layout -> layout.widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.ROW)
                .gapAll(5));

        var controls = new UIElement();
        controls.layout(layout -> layout.flex(1)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(5));
        preview.layout(layout -> layout.width(170).heightPercent(100));
        addChildren(controls, preview);

        var description = new Label();
        description.setText(Component.translatable("tool.mccreatorutils.items.description"));
        controls.addChild(description);

        picker = new RegistryPicker(RegistryKind.ITEM, session.itemSelection(), entry -> {
            session.setItemSelection(entry);
            preview.setSelection(entry);
            updateState();
        });
        controls.addChild(picker);
        preview.setSelection(session.itemSelection());

        var countLabel = new Label();
        countLabel.setText(Component.translatable("tool.mccreatorutils.items.count"));
        controls.addChild(countLabel);

        countField.setCharValidator(ItemCountInput::isAllowedCharacter);
        countField.setTextValidator(ItemCountInput::isValid);
        countField.textFieldStyle(
                style -> style.placeholder(Component.translatable("tool.mccreatorutils.items.count.placeholder")));
        countField.layout(layout -> layout.flex(1).height(14));
        countField.setText(session.itemCountRaw());
        countField.setTextResponder(session::setItemCountRaw);
        var countRow = new UIElement();
        countRow.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(4));
        countRow.addChild(countField);
        controls.addChild(countRow);
        countField.addEventListener(UIEvents.TICK, event -> {
            session.setItemCountRaw(countField.getRawText());
            updateState();
        });

        giveButton.setText(Component.translatable("tool.mccreatorutils.items.give"));
        giveButton.layout(layout -> layout.widthPercent(100).height(16));
        giveButton.setOnClick(event -> {
            ResourceLocation item = picker.selectedEntry();
            ItemCountInput.parse(countField.getRawText()).ifPresent(count -> {
                if (item != null) {
                    status.runCommand(GiveCommands.give(item, count));
                }
            });
        });
        controls.addChild(giveButton);

        var clearRow = new UIElement();
        clearRow.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(4));
        var clearInventory = new Button();
        clearInventory.setText(Component.translatable("tool.mccreatorutils.items.clear_inventory"));
        clearInventory.layout(layout -> layout.flex(1).height(16));
        clearInventory.setOnClick(event -> status.runCommand(GiveCommands.clearInventory()));
        var clearWorldItems = new Button();
        clearWorldItems.setText(Component.translatable("tool.mccreatorutils.items.clear_world_items"));
        clearWorldItems.layout(layout -> layout.flex(1).height(16));
        clearWorldItems.setOnClick(event -> status.runCommand(GiveCommands.clearWorldItems()));
        clearRow.addChildren(clearInventory, clearWorldItems);
        controls.addChild(clearRow);

        controls.addChild(status);
        updateState();
    }

    private void updateState() {
        boolean hasItem = picker.selectedEntry() != null;
        countField.setActive(hasItem);
        giveButton.setActive(
                hasItem && ItemCountInput.parse(countField.getRawText()).isPresent());
    }
}
