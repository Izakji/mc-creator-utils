package com.izakji.mccreatorutils.client.agent;

import com.izakji.mccreatorutils.client.agent.context.LogSummaryReport;
import com.izakji.mccreatorutils.client.agent.context.PerformanceReportData;
import com.izakji.mccreatorutils.client.agent.context.ReportPoint;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * Builds the visual performance report layout.
 *
 * @author Izakji
 */
public final class AgentReportUi {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int FPS_COLOR = 0xFF7BD88F;
    private static final int TPS_COLOR = 0xFF8BC4FF;
    private static final int MSPT_COLOR = 0xFFFFD45A;
    private static final int HEAP_COLOR = 0xFFFF9F7A;
    private static final int PING_COLOR = 0xFFB8A6FF;

    private AgentReportUi() {}

    public static ModularUI create(PerformanceReportData data, Runnable refresh, Runnable close) {
        var root = new UIElement()
                .layout(layout -> layout.width(760)
                        .height(580)
                        .flexDirection(FlexDirection.COLUMN)
                        .paddingAll(6)
                        .gapAll(4))
                .style(style -> style.background(Sprites.BORDER));

        root.addChild(header(data, refresh, close));
        root.addChild(stats(data));

        var scroller = new ScrollerView();
        scroller.layout(layout -> layout.widthPercent(100).flex(1));
        scroller.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        scroller.addScrollViewChild(chart("FPS", "fps", FPS_COLOR, data.fps(), -1, -1));
        scroller.addScrollViewChild(chart("TPS", "tps", TPS_COLOR, data.tps(), 0, 22));
        scroller.addScrollViewChild(chart("MSPT", "ms", MSPT_COLOR, data.mspt(), -1, -1));
        scroller.addScrollViewChild(chart("Heap", "MiB", HEAP_COLOR, data.heap(), -1, -1));
        scroller.addScrollViewChild(chart("Ping", "ms", PING_COLOR, data.ping(), -1, -1));
        scroller.addScrollViewChild(new ReportTextElement("Findings", String.join("\n", data.notes())));
        scroller.addScrollViewChild(new ReportTextElement("Logs", LogSummaryReport.format(data.logs())));
        root.addChild(scroller);

        UI.DynamicSizeProvider dynamicSize = screenSize -> Size.of(
                Math.max(1, Math.min(760, screenSize.getWidth() - 20)),
                Math.max(1, Math.min(580, screenSize.getHeight() - 20)));
        return ModularUI.of(UI.of(root, dynamicSize));
    }

    private static UIElement header(PerformanceReportData data, Runnable refresh, Runnable close) {
        var header = new UIElement();
        header.layout(layout -> layout.widthPercent(100)
                .height(18)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .gapAll(4));
        var title = new Label();
        title.setText(Component.literal("Performance Report"));
        title.layout(layout -> layout.width(120).height(16));
        var time = new Label();
        time.setText(Component.literal(Instant.ofEpochMilli(data.generatedAt())
                .atZone(ZoneId.systemDefault())
                .format(TIME)));
        time.layout(layout -> layout.flex(1).height(16));
        var refreshButton = new Button();
        refreshButton.setText(Component.literal("Refresh"));
        refreshButton.layout(layout -> layout.width(60).height(16));
        refreshButton.setOnClick(event -> refresh.run());
        var closeButton = new Button();
        closeButton.setText(Component.literal("Close"));
        closeButton.layout(layout -> layout.width(50).height(16));
        closeButton.setOnClick(event -> close.run());
        header.addChildren(title, time, refreshButton, closeButton);
        return header;
    }

    private static UIElement stats(PerformanceReportData data) {
        var row = new UIElement();
        row.layout(layout -> layout.widthPercent(100)
                .height(38)
                .flexDirection(FlexDirection.ROW)
                .gapAll(4));
        row.addChildren(
                tile("FPS / 1% low", value(data.currentFps()) + " / " + value(data.lowFps()), FPS_COLOR),
                tile("TPS / MSPT p95", value(data.currentTps()) + " / " + value(data.p95Mspt()), TPS_COLOR),
                tile("Heap", heap(data), HEAP_COLOR),
                tile("CPU", percent(data.processCpuPercent()), MSPT_COLOR),
                tile("Ping", data.currentPing() < 0 ? "n/a" : data.currentPing() + " ms", PING_COLOR));
        return row;
    }

    private static UIElement tile(String label, String value, int color) {
        var tile = new ReportStatElement(label, value, color);
        tile.layout(layout -> layout.flex(1).height(38));
        return tile;
    }

    private static UIElement chart(
            String title, String unit, int color, List<ReportPoint> points, double min, double max) {
        var chart = new ReportChartElement(title, unit, color, points, min, max);
        chart.layout(layout -> layout.widthPercent(100).height(96));
        return chart;
    }

    private static String heap(PerformanceReportData data) {
        if (data.heapUsedMb() < 0) {
            return "n/a";
        }
        if (data.heapMaxMb() <= 0) {
            return value(data.heapUsedMb()) + " MiB";
        }
        return value(data.heapUsedMb()) + " / " + value(data.heapMaxMb()) + " MiB";
    }

    private static String value(double number) {
        return number < 0 ? "n/a" : String.format(Locale.ROOT, "%.1f", number);
    }

    private static String percent(double number) {
        return number < 0 ? "n/a" : String.format(Locale.ROOT, "%.1f%%", number);
    }
}
