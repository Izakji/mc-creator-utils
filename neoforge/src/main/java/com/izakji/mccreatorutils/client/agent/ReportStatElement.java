package com.izakji.mccreatorutils.client.agent;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.Minecraft;

/**
 * One compact report statistic tile.
 *
 * @author Izakji
 */
public final class ReportStatElement extends UIElement {
    private static final int BACKGROUND = 0xFF202020;
    private static final int LABEL = 0xFF8A8A8A;
    private final String label;
    private final String value;
    private final int color;

    public ReportStatElement(String label, String value, int color) {
        this.label = label;
        this.value = value;
        this.color = color;
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
        context.graphics.fill(x, y, x + w, y + h, BACKGROUND);
        var font = Minecraft.getInstance().font;
        context.graphics.drawString(font, label, x + 5, y + 4, LABEL, false);
        context.graphics.drawString(font, value, x + 5, y + 16, color, false);
    }
}
