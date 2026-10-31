package com.izakji.mccreatorutils.client.agent;

import com.izakji.mccreatorutils.client.agent.context.ReportPoint;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

/**
 * Minimal line chart drawn directly into a LDLib2 element.
 *
 * @author Izakji
 */
public final class ReportChartElement extends UIElement {
    private static final int BACKGROUND = 0xFF181818;
    private static final int GRID = 0xFF303030;
    private static final int AXIS_TEXT = 0xFF8A8A8A;
    private static final int PLOT_LEFT = 34;
    private static final int PLOT_TOP = 18;
    private static final int PLOT_RIGHT = 6;
    private static final int PLOT_BOTTOM = 14;
    private final String title;
    private final String unit;
    private final int color;
    private final List<ReportPoint> points;
    private final double fixedMin;
    private final double fixedMax;

    public ReportChartElement(
            String title, String unit, int color, List<ReportPoint> points, double fixedMin, double fixedMax) {
        this.title = title;
        this.unit = unit;
        this.color = color;
        this.points = List.copyOf(points);
        this.fixedMin = fixedMin;
        this.fixedMax = fixedMax;
    }

    @Override
    public void drawContents(GUIContext context) {
        float width = getSizeWidth();
        float height = getSizeHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        int x = Math.round(getPositionX());
        int y = Math.round(getPositionY());
        int w = Math.round(width);
        int h = Math.round(height);
        var graphics = context.graphics;
        var font = Minecraft.getInstance().font;
        graphics.fill(x, y, x + w, y + h, BACKGROUND);
        graphics.drawString(font, title, x + 4, y + 4, 0xFFD0D0D0, false);

        int plotLeft = x + PLOT_LEFT;
        int plotTop = y + PLOT_TOP;
        int plotRight = x + w - PLOT_RIGHT;
        int plotBottom = y + h - PLOT_BOTTOM;
        if (plotRight <= plotLeft || plotBottom <= plotTop) {
            return;
        }
        if (points.isEmpty()) {
            graphics.drawString(font, "No samples yet", plotLeft + 4, plotTop + 8, AXIS_TEXT, false);
            return;
        }
        double min = fixedMin;
        double max = fixedMax;
        if (max <= min) {
            min = Double.MAX_VALUE;
            max = -Double.MAX_VALUE;
            for (ReportPoint point : points) {
                min = Math.min(min, point.value());
                max = Math.max(max, point.value());
            }
            double padding = Math.max(1.0, (max - min) * 0.1);
            min = Math.max(0, min - padding);
            max += padding;
        }
        if (max <= min) {
            max = min + 1;
        }
        for (int line = 0; line <= 4; line++) {
            int lineY = plotBottom - (plotBottom - plotTop) * line / 4;
            graphics.fill(plotLeft, lineY, plotRight, lineY + 1, GRID);
            double value = min + (max - min) * line / 4.0;
            String label = format(value);
            graphics.drawString(font, label, x + 3, lineY - 4, AXIS_TEXT, false);
        }
        int previousX = -1;
        int previousY = -1;
        for (int index = 0; index < points.size(); index++) {
            int pointX = plotLeft + (points.size() == 1 ? 0 : (plotRight - plotLeft) * index / (points.size() - 1));
            double normalized = (points.get(index).value() - min) / (max - min);
            int pointY = plotBottom - (int) Math.round(normalized * (plotBottom - plotTop));
            if (previousX >= 0) {
                drawLine(graphics, previousX, previousY, pointX, pointY, color);
            }
            previousX = pointX;
            previousY = pointY;
        }
        ReportPoint last = points.get(points.size() - 1);
        graphics.drawString(
                font,
                format(last.value()) + (unit.isEmpty() ? "" : " " + unit),
                plotRight - font.width(format(last.value()) + (unit.isEmpty() ? "" : " " + unit)),
                y + 4,
                color,
                false);
        graphics.drawString(font, "older", plotLeft + 3, plotBottom + 3, AXIS_TEXT, false);
        graphics.drawString(font, "now", plotRight - font.width("now"), plotBottom + 3, AXIS_TEXT, false);
    }

    private static void drawLine(
            net.minecraft.client.gui.GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int step = 0; step <= steps; step++) {
            int x = x1 + (x2 - x1) * step / steps;
            int y = y1 + (y2 - y1) * step / steps;
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }
}
