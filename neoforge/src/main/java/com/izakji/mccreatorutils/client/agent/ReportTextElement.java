package com.izakji.mccreatorutils.client.agent;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Bounded wrapped text block for report details.
 *
 * @author Izakji
 */
public final class ReportTextElement extends UIElement {
    private static final int BACKGROUND = 0xFF181818;
    private static final int TITLE = 0xFFD0D0D0;
    private static final int BODY = 0xFFB0B0B0;
    private final String title;
    private final String text;

    public ReportTextElement(String title, String text) {
        this.title = title;
        this.text = text;
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
        context.graphics.drawString(font, title, x + 4, y + 4, TITLE, false);
        int lineY = y + 16;
        for (String raw : text.split("\\n", -1)) {
            for (var sequence : font.split(Component.literal(raw), Math.max(1, w - 8))) {
                if (lineY + 9 > y + h) {
                    return;
                }
                context.graphics.drawString(font, sequence, x + 4, lineY, BODY, false);
                lineY += 9;
            }
        }
    }
}
