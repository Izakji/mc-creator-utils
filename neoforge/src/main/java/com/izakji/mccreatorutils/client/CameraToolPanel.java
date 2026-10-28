package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.camera.CamMode;
import com.izakji.mccreatorutils.camera.CameraCommands;
import com.izakji.mccreatorutils.camera.DurationInput;
import com.izakji.mccreatorutils.camera.DurationUnit;
import com.izakji.mccreatorutils.session.CreatorToolSession;
import com.izakji.mccreatorutils.tool.ToolCatalog;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/**
 * Controls for automating CMDCam {@code /cam mode} and {@code /cam duration}.
 *
 * @author Izakji
 */
public final class CameraToolPanel extends UIElement {
    private final CreatorToolSession session = CreatorToolSession.instance();
    private final CommandStatusLabel status = new CommandStatusLabel();

    public CameraToolPanel() {
        super();
        this.layout(layout -> layout.widthPercent(100)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(5));

        var controls = new UIElement();
        controls.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(5));
        addChild(controls);

        var description = new Label();
        description.setText(Component.translatable("tool.mccreatorutils.camera.description"));
        controls.addChild(description);

        controls.addChild(buildModeSection());
        controls.addChild(buildDurationSection());

        status.setText(Component.empty());
        controls.addChild(status);

        if (!ModList.get().isLoaded(ToolCatalog.CAMERA.requiredModId())) {
            controls.setActive(false);
            var notice = new Label();
            notice.setText(Component.translatable("notice.mccreatorutils.cmdcam_missing"));
            notice.textStyle(style -> style.textColor(ColorPattern.YELLOW.color));
            addChild(notice);
        }
    }

    private UIElement buildModeSection() {
        var section = new UIElement();
        section.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));

        var label = new Label();
        label.setText(Component.translatable("tool.mccreatorutils.camera.mode"));
        section.addChild(label);

        var row = new UIElement();
        row.layout(layout -> layout.widthPercent(100)
                .height(28)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .gapAll(4));

        var modeSelector = new SearchableSelector<CamMode>();
        modeSelector.setCandidates(
                List.of(CamMode.values()),
                mode -> Component.translatable(
                        mode == null ? "tool.mccreatorutils.camera.mode.select" : mode.translationKey()));
        modeSelector.layout(layout -> layout.flex(1).height(27));
        modeSelector.setSelected(session.cameraMode());

        var runButton = new Button();
        runButton.setText(Component.translatable("tool.mccreatorutils.camera.set_mode"));
        runButton.layout(layout -> layout.width(72).height(16));
        runButton.setActive(modeSelector.getValue() != null);
        modeSelector.setOnValueChanged(mode -> {
            session.setCameraMode(mode);
            runButton.setActive(mode != null);
        });
        runButton.setOnClick(event -> {
            CamMode mode = modeSelector.getValue();
            if (mode != null) {
                status.runCommand(CameraCommands.mode(mode));
            }
        });

        row.addChildren(modeSelector, runButton);
        section.addChild(row);
        return section;
    }

    private UIElement buildDurationSection() {
        var section = new UIElement();
        section.layout(layout ->
                layout.widthPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2));

        var label = new Label();
        label.setText(Component.translatable("tool.mccreatorutils.camera.duration"));
        section.addChild(label);

        var row = new UIElement();
        row.layout(layout -> layout.widthPercent(100)
                .height(28)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .gapAll(4));

        var durationField = new TextField();
        durationField.setCharValidator(DurationInput::isAllowedCharacter);
        durationField.setTextValidator(DurationInput::isValid);
        durationField.textFieldStyle(
                style -> style.placeholder(Component.translatable("tool.mccreatorutils.camera.duration.placeholder")));
        durationField.layout(layout -> layout.flex(1).height(14));
        durationField.setText(session.cameraDurationRaw());
        durationField.setTextResponder(session::setCameraDurationRaw);

        var unitSelector = new SearchableSelector<DurationUnit>();
        unitSelector.setCandidates(
                List.of(DurationUnit.values()),
                unit -> unit == null ? Component.empty() : Component.translatable(unit.translationKey()));
        unitSelector.layout(layout -> layout.width(58).height(27));
        unitSelector.setSelected(session.cameraDurationUnit());

        var runButton = new Button();
        runButton.setText(Component.translatable("tool.mccreatorutils.camera.set_duration"));
        runButton.layout(layout -> layout.widthPercent(100).height(16));
        runButton.setActive(false);

        Runnable updateRunState = () -> {
            DurationUnit unit = unitSelector.getValue();
            runButton.setActive(unit != null
                    && DurationInput.parse(durationField.getRawText()).isPresent());
        };
        durationField.addEventListener(UIEvents.TICK, event -> {
            session.setCameraDurationRaw(durationField.getRawText());
            updateRunState.run();
        });
        unitSelector.setOnValueChanged(unit -> {
            if (unit != null) {
                session.setCameraDurationUnit(unit);
            }
            updateRunState.run();
        });
        runButton.setOnClick(event -> {
            session.setCameraDurationRaw(durationField.getRawText());
            DurationUnit unit = unitSelector.getValue();
            DurationInput.parse(durationField.getRawText()).ifPresent(value -> {
                if (unit != null) {
                    status.runCommand(CameraCommands.duration(value, unit));
                }
            });
        });
        updateRunState.run();

        row.addChildren(durationField, unitSelector);
        section.addChildren(row, runButton);
        return section;
    }
}
