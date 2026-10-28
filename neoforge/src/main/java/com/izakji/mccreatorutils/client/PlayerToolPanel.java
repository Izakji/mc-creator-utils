package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.player.PlayerCommands;
import com.izakji.mccreatorutils.player.XpAmountInput;
import com.izakji.mccreatorutils.player.XpType;
import com.izakji.mccreatorutils.session.CreatorToolSession;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * Player effect toggles and experience controls.
 *
 * @author Izakji
 */
public final class PlayerToolPanel extends UIElement {
    private final CreatorToolSession session = CreatorToolSession.instance();
    private final CommandConsole console = new CommandConsole();
    private final Switch invincibleToggle = new Switch();
    private final Switch waterBreathingToggle = new Switch();
    private final TextField xpAmountField = new TextField();
    private final Selector<XpType> xpTypeSelector = new Selector<>();
    private final Button addXpButton = new Button();

    public PlayerToolPanel() {
        this.layout(layout -> layout.widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(5));

        var description = new Label();
        description.setText(Component.translatable("tool.mccreatorutils.player.description"));
        addChild(description);

        addChild(buildInvincibleRow());
        addChild(buildClearEffectsButton());
        addChild(buildWaterBreathingRow());
        addChild(buildXpSection());
        addChild(console);
    }

    private UIElement buildInvincibleRow() {
        var row = toggleRow("tool.mccreatorutils.player.invincible", invincibleToggle);
        invincibleToggle.setOn(session.invincible(), false);
        invincibleToggle.setOnSwitchChanged(value -> {
            session.setInvincible(value);
            console.runCommand(value ? PlayerCommands.giveInvincible() : PlayerCommands.clearInvincible());
        });
        return row;
    }

    private UIElement buildWaterBreathingRow() {
        var row = toggleRow("tool.mccreatorutils.player.water_breathing", waterBreathingToggle);
        waterBreathingToggle.setOn(session.waterBreathing(), false);
        waterBreathingToggle.setOnSwitchChanged(value -> {
            session.setWaterBreathing(value);
            console.runCommand(value ? PlayerCommands.giveWaterBreathing() : PlayerCommands.clearWaterBreathing());
        });
        return row;
    }

    private UIElement buildClearEffectsButton() {
        var clearButton = new Button();
        clearButton.setText(Component.translatable("tool.mccreatorutils.player.clear_effects"));
        clearButton.layout(layout -> layout.widthPercent(100).height(16));
        clearButton.setOnClick(event -> {
            session.setInvincible(false);
            session.setWaterBreathing(false);
            invincibleToggle.setOn(false, false);
            waterBreathingToggle.setOn(false, false);
            console.runCommand(PlayerCommands.clearEffects());
        });
        return clearButton;
    }

    private UIElement buildXpSection() {
        var section = new UIElement();
        section.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));

        var label = new Label();
        label.setText(Component.translatable("tool.mccreatorutils.player.xp"));
        section.addChild(label);

        var row = new UIElement();
        row.layout(layout -> layout.widthPercent(100)
                .height(28)
                .flexDirection(FlexDirection.ROW)
                .gapAll(4));

        xpAmountField.setCharValidator(XpAmountInput::isAllowedCharacter);
        xpAmountField.setTextValidator(raw -> raw.isEmpty() || XpAmountInput.isValid(raw));
        xpAmountField.textFieldStyle(
                style -> style.placeholder(Component.translatable("tool.mccreatorutils.player.xp.placeholder")));
        xpAmountField.layout(layout -> layout.flex(1).height(14));
        xpAmountField.setText(session.xpAmountRaw());
        xpAmountField.setTextResponder(session::setXpAmountRaw);

        xpTypeSelector.setCandidates(List.of(XpType.values()));
        xpTypeSelector.setCandidateUIProvider(UIElementProvider.text(
                type -> type == null ? Component.empty() : Component.translatable(type.translationKey())));
        xpTypeSelector.layout(layout -> layout.width(62).height(14));
        xpTypeSelector.setSelected(session.xpType());

        addXpButton.setText(Component.translatable("tool.mccreatorutils.player.add_xp"));
        addXpButton.layout(layout -> layout.width(72).height(16));
        addXpButton.setActive(false);

        Runnable updateButton = () -> addXpButton.setActive(xpTypeSelector.getValue() != null
                && XpAmountInput.parse(xpAmountField.getRawText()).isPresent());
        xpAmountField.addEventListener(UIEvents.TICK, event -> {
            session.setXpAmountRaw(xpAmountField.getRawText());
            updateButton.run();
        });
        xpTypeSelector.setOnValueChanged(type -> {
            if (type != null) {
                session.setXpType(type);
            }
            updateButton.run();
        });
        addXpButton.setOnClick(event -> {
            session.setXpAmountRaw(xpAmountField.getRawText());
            XpAmountInput.parse(xpAmountField.getRawText()).ifPresent(amount -> {
                XpType type = xpTypeSelector.getValue();
                if (type != null) {
                    console.runCommand(PlayerCommands.addXp(amount, type));
                }
            });
        });

        row.addChildren(xpAmountField, xpTypeSelector, addXpButton);
        section.addChild(row);
        updateButton.run();
        return section;
    }

    private static UIElement toggleRow(String translationKey, Switch toggle) {
        var row = new UIElement();
        row.layout(layout -> layout.widthPercent(100)
                .height(16)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .gapAll(4));

        var label = new Label();
        label.setText(Component.translatable(translationKey));
        label.layout(layout -> layout.flex(1));
        row.addChildren(label, toggle);
        return row;
    }
}
