package com.izakji.mccreatorutils.client;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Small scrollable command history console.
 *
 * @author Izakji
 */
public final class CommandConsole extends UIElement {
    private static final int MAX_LINES = 80;
    private static final float FONT_SIZE = 9.0f;
    private static final float LINE_HEIGHT = 14.0f;

    private final ScrollerView scroller = new ScrollerView();
    private final UIElement lines = new UIElement();
    private final List<Label> lineLabels = new ArrayList<>();
    private int maxLineWidth = 226;

    public CommandConsole() {
        layout(layout -> layout.widthPercent(100).height(112));
        style(style -> style.background(Sprites.RECT_DARK));
        scroller.layout(layout -> layout.widthPercent(100).flex(1));
        scroller.scrollerStyle(style -> style.mode(ScrollerMode.BOTH)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.AUTO));
        lines.layout(
                layout -> layout.width(226).flexDirection(FlexDirection.COLUMN).gapAll(1));
        scroller.addScrollViewChild(lines);
        addChild(scroller);
    }

    public void runCommand(String command) {
        ClientCommandRunner.CommandResult result = ClientCommandRunner.execute(command);
        append(Component.literal("> /" + command), 0xFFFFFFFF);
        append(result.message(), result.success() ? ColorPattern.GREEN.color : ColorPattern.RED.color);
    }

    public void append(Component text, int color) {
        Label line = new Label();
        line.setText(text);
        line.textStyle(style -> style.fontSize(FONT_SIZE)
                .textColor(color)
                .textShadow(false)
                .textWrap(TextWrap.NONE)
                .adaptiveWidth(false));
        int width = Math.max(226, Math.round(Minecraft.getInstance().font.width(text) * (FONT_SIZE / 9.0f)) + 8);
        line.layout(layout -> layout.width(width).height(LINE_HEIGHT));
        lines.addChild(line);
        lineLabels.add(line);

        maxLineWidth = Math.max(maxLineWidth, width);
        lines.layout(layout ->
                layout.width(maxLineWidth).flexDirection(FlexDirection.COLUMN).gapAll(1));

        while (lineLabels.size() > MAX_LINES) {
            Label removed = lineLabels.remove(0);
            lines.removeChild(removed);
        }
        scroller.scrollToChildDelayed(line);
    }
}
