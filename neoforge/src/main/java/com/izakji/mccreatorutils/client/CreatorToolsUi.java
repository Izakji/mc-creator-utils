package com.izakji.mccreatorutils.client;

import com.izakji.mccreatorutils.tool.ToolCatalog;
import com.izakji.mccreatorutils.tool.ToolDescriptor;
import com.izakji.mccreatorutils.tool.ToolId;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.math.Size;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Builds the two-pane Creator Utils interface.
 *
 * @author Izakji
 */
public final class CreatorToolsUi {
    private final Map<ToolId, Button> toolButtons = new EnumMap<>(ToolId.class);
    private final Label heading = new Label();
    private final UIElement content = new UIElement();
    private ToolId selectedTool;

    private CreatorToolsUi() {}

    public static ModularUI create() {
        return new CreatorToolsUi().build();
    }

    private ModularUI build() {
        var root = new UIElement()
                .layout(layout -> layout.width(360)
                        .height(240)
                        .flexDirection(FlexDirection.ROW)
                        .paddingAll(6)
                        .gapAll(6))
                .style(style -> style.background(Sprites.BORDER));

        var leftPane = new UIElement()
                .layout(layout -> layout.width(104)
                        .heightPercent(100)
                        .flexDirection(FlexDirection.COLUMN)
                        .paddingAll(4)
                        .gapAll(4))
                .style(style -> style.background(Sprites.RECT_DARK));

        var toolList = new ScrollerView();
        toolList.layout(layout -> layout.widthPercent(100).flex(1));
        toolList.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));

        for (ToolDescriptor tool : ToolCatalog.tools()) {
            var button = new Button();
            button.setText(Component.translatable(tool.translationKey()));
            button.layout(layout -> layout.widthPercent(100).height(16));
            button.setOnClick(event -> selectTool(tool));
            toolButtons.put(tool.id(), button);
            toolList.addScrollViewChild(button);
        }

        leftPane.addChild(toolList);

        var rightPane = new UIElement()
                .layout(layout -> layout.flex(1)
                        .heightPercent(100)
                        .flexDirection(FlexDirection.COLUMN)
                        .paddingAll(6)
                        .gapAll(4))
                .style(style -> style.background(Sprites.RECT_DARK));

        var headerRow = new UIElement();
        headerRow.layout(layout -> layout.widthPercent(100)
                .height(16)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .gapAll(4));

        heading.layout(layout -> layout.flex(1).height(12));
        var closeButton = new Button();
        closeButton.setText("X");
        closeButton.layout(layout -> layout.width(16).height(16));
        closeButton.style(style -> style.tooltips(Component.translatable("tooltip.mccreatorutils.close")));
        closeButton.setOnClick(event -> Minecraft.getInstance().setScreen(null));
        headerRow.addChildren(heading, closeButton);

        var divider = new UIElement()
                .layout(layout -> layout.widthPercent(100).height(1))
                .style(style -> style.background(ColorPattern.DARK_GRAY.rectTexture()));
        content.layout(layout -> layout.widthPercent(100).flex(1));
        rightPane.addChildren(headerRow, divider, content);

        root.addChildren(leftPane, rightPane);

        UI.DynamicSizeProvider dynamicSize = screenSize -> Size.of(
                Math.max(1, Math.min(360, screenSize.getWidth() - 20)),
                Math.max(1, Math.min(240, screenSize.getHeight() - 20)));

        selectTool(ToolCatalog.tools().get(0));
        return ModularUI.of(UI.of(root, dynamicSize));
    }

    private void selectTool(ToolDescriptor tool) {
        if (selectedTool == tool.id()) {
            return;
        }
        selectedTool = tool.id();
        toolButtons.forEach((id, button) -> button.buttonStyle(
                style -> style.baseTexture(id == tool.id() ? Sprites.RECT_RD_LIGHT : Sprites.RECT_RD)));
        heading.setText(Component.translatable(tool.translationKey()));
        content.clearAllChildren();
        if (tool.id() == ToolId.CAMERA) {
            content.addChild(new CameraToolPanel());
        } else if (tool.id() == ToolId.WORLD_TIME) {
            content.addChild(new WorldTimeToolPanel());
        } else if (tool.id() == ToolId.BIOMES) {
            content.addChild(new BiomesToolPanel());
        } else if (tool.id() == ToolId.STRUCTURES) {
            content.addChild(new StructuresToolPanel());
        } else if (tool.id() == ToolId.DIMENSIONS) {
            content.addChild(new DimensionsToolPanel());
        } else if (tool.id() == ToolId.ITEMS) {
            content.addChild(new ItemsToolPanel());
        } else if (tool.id() == ToolId.MOBS) {
            content.addChild(new MobsToolPanel());
        }
    }
}
