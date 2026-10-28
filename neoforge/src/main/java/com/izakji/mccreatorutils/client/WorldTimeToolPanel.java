package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.session.CreatorToolSession;
import com.izakji.mccreatorutils.time.TimeCommands;
import com.izakji.mccreatorutils.time.TimeInput;
import com.izakji.mccreatorutils.time.TimePreset;
import com.izakji.mccreatorutils.weather.WeatherCommands;
import com.izakji.mccreatorutils.weather.WeatherDurationInput;
import com.izakji.mccreatorutils.weather.WeatherUnit;
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
 * Controls for vanilla time, daylight-cycle, and weather commands.
 *
 * @author Izakji
 */
public final class WorldTimeToolPanel extends UIElement {
    private final CreatorToolSession session = CreatorToolSession.instance();
    private final CommandStatusLabel status = new CommandStatusLabel();

    public WorldTimeToolPanel() {
        super();
        this.layout(layout -> layout.widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(5));

        var description = new Label();
        description.setText(Component.translatable("tool.mccreatorutils.world_time.description"));
        addChild(description);

        addChild(buildPresetSection());
        addChild(buildTicksSection());
        addChild(buildFreezeSection());
        addChild(buildWeatherSection());
        addChild(status);
    }

    private UIElement buildPresetSection() {
        var section = new UIElement();
        section.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));

        var label = new Label();
        label.setText(Component.translatable("tool.mccreatorutils.world_time.preset"));
        section.addChild(label);

        var row = new UIElement();
        row.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(4));

        var presetSelector = new Selector<TimePreset>();
        presetSelector.setCandidates(List.of(TimePreset.values()));
        presetSelector.setCandidateUIProvider(UIElementProvider.text(preset -> Component.translatable(
                preset == null ? "tool.mccreatorutils.world_time.preset.select" : preset.translationKey())));
        presetSelector.layout(layout -> layout.flex(1).height(14));
        presetSelector.setSelected(session.timePreset());
        presetSelector.setOnValueChanged(preset -> {
            if (preset != null) {
                session.setTimePreset(preset);
            }
        });

        var setButton = new Button();
        setButton.setText(Component.translatable("tool.mccreatorutils.world_time.set_preset"));
        setButton.layout(layout -> layout.width(72).height(16));
        setButton.setOnClick(event -> {
            TimePreset preset = presetSelector.getValue();
            if (preset != null) {
                status.runCommand(TimeCommands.setPreset(preset));
            }
        });

        row.addChildren(presetSelector, setButton);
        section.addChild(row);
        return section;
    }

    private UIElement buildTicksSection() {
        var section = new UIElement();
        section.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));

        var label = new Label();
        label.setText(Component.translatable("tool.mccreatorutils.world_time.ticks"));
        section.addChild(label);

        var row = new UIElement();
        row.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(4));

        var tickField = new TextField();
        tickField.setCharValidator(TimeInput::isAllowedCharacter);
        tickField.setTextValidator(TimeInput::isValid);
        tickField.textFieldStyle(
                style -> style.placeholder(Component.translatable("tool.mccreatorutils.world_time.ticks.placeholder")));
        tickField.layout(layout -> layout.flex(1).height(14));
        tickField.setText(session.timeTicksRaw());
        tickField.setTextResponder(session::setTimeTicksRaw);

        var setButton = new Button();
        setButton.setText(Component.translatable("tool.mccreatorutils.world_time.set_ticks"));
        setButton.layout(layout -> layout.width(72).height(16));
        setButton.setActive(false);

        Runnable updateRunState = () ->
                setButton.setActive(TimeInput.parse(tickField.getRawText()).isPresent());
        tickField.addEventListener(UIEvents.TICK, event -> {
            session.setTimeTicksRaw(tickField.getRawText());
            updateRunState.run();
        });
        setButton.setOnClick(event -> {
            session.setTimeTicksRaw(tickField.getRawText());
            TimeInput.parse(tickField.getRawText()).ifPresent(ticks -> status.runCommand(TimeCommands.setTicks(ticks)));
        });
        updateRunState.run();

        row.addChildren(tickField, setButton);
        section.addChild(row);
        return section;
    }

    private UIElement buildFreezeSection() {
        var row = new UIElement();
        row.layout(layout -> layout.widthPercent(100)
                .height(16)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .gapAll(4));

        var label = new Label();
        label.setText(Component.translatable("tool.mccreatorutils.world_time.freeze"));
        label.layout(layout -> layout.flex(1));

        var toggle = new Switch();
        toggle.setOn(session.freezeTime(), false);
        toggle.setOnSwitchChanged(frozen -> {
            session.setFreezeTime(frozen);
            status.runCommand(TimeCommands.setFrozen(frozen));
        });

        row.addChildren(label, toggle);
        return row;
    }

    private UIElement buildWeatherSection() {
        var section = new UIElement();
        section.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));

        var label = new Label();
        label.setText(Component.translatable("tool.mccreatorutils.world_time.weather"));
        section.addChild(label);

        var actionRow = new UIElement();
        actionRow.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(4));

        var clearButton = new Button();
        clearButton.setText(Component.translatable("tool.mccreatorutils.world_time.clear"));
        clearButton.layout(layout -> layout.flex(1).height(16));
        clearButton.setOnClick(event -> status.runCommand(WeatherCommands.clear()));

        var rainButton = new Button();
        rainButton.setText(Component.translatable("tool.mccreatorutils.world_time.rain"));
        rainButton.layout(layout -> layout.flex(1).height(16));
        rainButton.setOnClick(event -> status.runCommand(WeatherCommands.rain()));

        actionRow.addChildren(clearButton, rainButton);
        section.addChild(actionRow);

        var thunderRow = new UIElement();
        thunderRow.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.ROW).gapAll(4));

        var durationField = new TextField();
        durationField.setCharValidator(WeatherDurationInput::isAllowedCharacter);
        durationField.setTextValidator(WeatherDurationInput::isValidOrEmpty);
        durationField.textFieldStyle(style ->
                style.placeholder(Component.translatable("tool.mccreatorutils.world_time.thunder.placeholder")));
        durationField.layout(layout -> layout.flex(1).height(14));
        durationField.setText(session.thunderDurationRaw());
        durationField.setTextResponder(session::setThunderDurationRaw);

        var unitSelector = new Selector<WeatherUnit>();
        unitSelector.setCandidates(List.of(WeatherUnit.values()));
        unitSelector.setCandidateUIProvider(UIElementProvider.text(
                unit -> unit == null ? Component.empty() : Component.translatable(unit.translationKey())));
        unitSelector.layout(layout -> layout.width(58).height(14));
        unitSelector.setSelected(session.thunderUnit());
        unitSelector.setOnValueChanged(unit -> {
            if (unit != null) {
                session.setThunderUnit(unit);
            }
        });

        var thunderButton = new Button();
        thunderButton.setText(Component.translatable("tool.mccreatorutils.world_time.thunder"));
        thunderButton.layout(layout -> layout.width(72).height(16));
        thunderButton.setActive(false);

        Runnable updateRunState = () -> {
            String raw = durationField.getRawText();
            thunderButton.setActive(raw.isEmpty()
                    || (unitSelector.getValue() != null
                            && WeatherDurationInput.parse(raw).isPresent()));
        };
        durationField.addEventListener(UIEvents.TICK, event -> {
            session.setThunderDurationRaw(durationField.getRawText());
            updateRunState.run();
        });
        thunderButton.setOnClick(event -> {
            session.setThunderDurationRaw(durationField.getRawText());
            String raw = durationField.getRawText();
            if (raw.isEmpty()) {
                status.runCommand(WeatherCommands.thunder());
                return;
            }
            WeatherUnit unit = unitSelector.getValue();
            WeatherDurationInput.parse(raw).ifPresent(value -> {
                if (unit != null) {
                    status.runCommand(WeatherCommands.thunder(value, unit));
                }
            });
        });
        updateRunState.run();

        thunderRow.addChildren(durationField, unitSelector, thunderButton);
        section.addChild(thunderRow);
        return section;
    }
}
