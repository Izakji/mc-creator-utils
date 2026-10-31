package com.izakji.mccreatorutils.client.agent;

import com.izakji.mccreatorutils.agent.AgentChatGeometry;
import com.izakji.mccreatorutils.agent.AgentChatLayout;
import com.izakji.mccreatorutils.agent.AgentChatWindowState;
import com.izakji.mccreatorutils.agent.AgentMarkdown;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * Draws the floating agent chat chrome and transcript.
 *
 * @author Izakji
 */
public final class AgentChatRenderer {
    private static final int WINDOW_BACKGROUND = 0xFF202020;
    private static final int WINDOW_BORDER = 0xFF000000;
    private static final int TITLE_BACKGROUND = 0xFF2B2B2B;
    private static final int TRANSCRIPT_BACKGROUND = 0xFF141414;
    private static final int INPUT_BACKGROUND = 0xFF101010;
    private static final int TEXT = 0xFFD0D0D0;
    private static final int MUTED_TEXT = 0xFF8A8A8A;
    private static final int USER_TEXT = 0xFF8BC4FF;
    private static final int ERROR_TEXT = 0xFFFF7777;
    private static final int TOOL_TEXT = 0xFFFFD45A;
    private static final int REASONING_TEXT = 0xFFB8A6FF;
    private static final int HEADING_TEXT = 0xFF62C45A;
    private static final int SUBTITLE_TEXT = 0xFFF7700C;
    private static final int CODE_TEXT = 0xFF0CC4F7;
    private static final int AGENT_TEXT = 0xFFE55FFF;
    private static final float TEXT_SCALE = 7f / 9f;
    private static final int LINE_HEIGHT = 8;
    private static CachedTranscript cache;

    private AgentChatRenderer() {}

    public static void render(
            GuiGraphics graphics, AgentChatSession session, AgentChatWindowState windowState, boolean hud) {
        renderWithHits(graphics, session, windowState, new AgentChatScreenState(), hud);
    }

    /**
     * Draws the window and returns clickable transcript headers.
     *
     * @param graphics graphics
     * @param session chat session
     * @param windowState window bounds
     * @param screenState scroll/collapse state
     * @param hud whether this is the unfocused HUD render
     * @return clickable entry headers
     */
    public static List<EntryHit> renderWithHits(
            GuiGraphics graphics,
            AgentChatSession session,
            AgentChatWindowState windowState,
            AgentChatScreenState screenState,
            boolean hud) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        AgentChatLayout layout = AgentChatLayout.of(windowState.bounds(width, height));
        AgentChatGeometry window = layout.window();

        graphics.fill(window.x() - 2, window.y() - 2, window.right() + 2, window.bottom() + 2, 0x66000000);
        graphics.fill(window.x(), window.y(), window.right(), window.bottom(), WINDOW_BACKGROUND);
        graphics.fill(window.x(), window.y(), window.right(), window.y() + 1, WINDOW_BORDER);
        graphics.fill(window.x(), window.bottom() - 1, window.right(), window.bottom(), WINDOW_BORDER);
        graphics.fill(window.x(), window.y(), window.x() + 1, window.bottom(), WINDOW_BORDER);
        graphics.fill(window.right() - 1, window.y(), window.right(), window.bottom(), WINDOW_BORDER);
        graphics.fill(
                layout.titleBar().x(),
                layout.titleBar().y(),
                layout.titleBar().right(),
                layout.titleBar().bottom(),
                TITLE_BACKGROUND);
        graphics.fill(
                layout.transcript().x(),
                layout.transcript().y(),
                layout.transcript().right(),
                layout.transcript().bottom(),
                TRANSCRIPT_BACKGROUND);
        graphics.fill(
                layout.inputBar().x(),
                layout.inputBar().y(),
                layout.inputBar().right(),
                layout.inputBar().bottom(),
                INPUT_BACKGROUND);

        String title =
                session.conversation().map(conversation -> conversation.title()).orElse("Agent Chat");
        graphics.drawString(
                minecraft.font,
                truncate(minecraft.font, title, layout.titleBar().width() - 48),
                layout.titleBar().x() + 5,
                layout.titleBar().y() + 5,
                TEXT,
                false);
        if (hud) {
            graphics.drawString(
                    minecraft.font,
                    "X",
                    layout.closeButton().x() + 5,
                    layout.closeButton().y() + 5,
                    MUTED_TEXT,
                    false);
            graphics.drawString(
                    minecraft.font,
                    windowState.fullscreen() ? "-" : "+",
                    layout.fullscreenButton().x() + 5,
                    layout.fullscreenButton().y() + 5,
                    MUTED_TEXT,
                    false);
            graphics.drawString(
                    minecraft.font,
                    session.busy() ? "..." : ">",
                    layout.sendButton().x() + 5,
                    layout.sendButton().y() + 5,
                    MUTED_TEXT,
                    false);
            graphics.drawString(
                    minecraft.font,
                    truncate(
                            minecraft.font,
                            session.profileId().isBlank() ? "Ask the agent..." : session.profileId(),
                            100),
                    layout.profileButton().x() + 4,
                    layout.profileButton().y() + 5,
                    MUTED_TEXT,
                    false);
        }

        List<EntryHit> hits = drawTranscript(graphics, minecraft.font, layout, session, screenState, hud);
        if (!hud) {
            graphics.fill(
                    layout.resizeGrip().right() - 2,
                    layout.resizeGrip().bottom() - 10,
                    layout.resizeGrip().right(),
                    layout.resizeGrip().bottom() - 8,
                    MUTED_TEXT);
            graphics.fill(
                    layout.resizeGrip().right() - 6,
                    layout.resizeGrip().bottom() - 6,
                    layout.resizeGrip().right(),
                    layout.resizeGrip().bottom() - 4,
                    MUTED_TEXT);
        }
        return hits;
    }

    private static List<EntryHit> drawTranscript(
            GuiGraphics graphics,
            Font font,
            AgentChatLayout layout,
            AgentChatSession session,
            AgentChatScreenState screenState,
            boolean hud) {
        AgentChatGeometry area = layout.transcript();
        int textWidth = Math.max(1, area.width() - 10);
        List<Line> lines = cachedLines(font, session, screenState, textWidth);
        int loaderHeight = session.busy() ? 18 : 0;
        int viewportBottom = area.bottom() - loaderHeight;
        int viewportHeight = Math.max(1, viewportBottom - area.y() - 6);
        int contentHeight = lines.size() * LINE_HEIGHT;
        int maxOffset = Math.max(0, contentHeight - viewportHeight);
        screenState.onContentMeasured(contentHeight);
        screenState.clampToContent(maxOffset);
        int startY = contentHeight <= viewportHeight
                ? area.y() + 4
                : viewportBottom - contentHeight + screenState.scrollOffset();
        graphics.enableScissor(area.x() + 1, area.y() + 1, area.right() - 1, viewportBottom);
        List<EntryHit> visibleHits = new ArrayList<>();
        int firstLine = Math.max(0, (area.y() - startY) / LINE_HEIGHT - 1);
        int lastLine = Math.min(lines.size(), (viewportBottom - startY) / LINE_HEIGHT + 2);
        for (int index = firstLine; index < lastLine; index++) {
            Line line = lines.get(index);
            int y = startY + index * LINE_HEIGHT;
            if (line.header() && line.entryIndex() >= 0) {
                visibleHits.add(new EntryHit(
                        line.entryIndex(), new AgentChatGeometry(area.x() + 4, y, textWidth, LINE_HEIGHT)));
            }
            graphics.pose().pushPose();
            graphics.pose().translate(area.x() + 5, y, 0);
            graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1f);
            graphics.drawString(font, line.text(), 0, 0, TEXT, false);
            graphics.pose().popPose();
        }
        graphics.disableScissor();
        if (session.busy()) {
            drawWalkingPig(graphics, area.x() + 5, area.bottom() - 15);
        }
        return hud ? List.of() : visibleHits;
    }

    private static List<Line> cachedLines(
            Font font, AgentChatSession session, AgentChatScreenState screenState, int textWidth) {
        long modelRevision = session.model().revision();
        long collapseRevision = screenState.collapsedRevision();
        CachedTranscript current = cache;
        if (current != null
                && current.modelRevision() == modelRevision
                && current.collapseRevision() == collapseRevision
                && current.textWidth() == textWidth) {
            return current.lines();
        }
        List<Line> lines = buildLines(font, session, screenState, textWidth);
        cache = new CachedTranscript(modelRevision, collapseRevision, textWidth, lines);
        return lines;
    }

    private static List<Line> buildLines(
            Font font, AgentChatSession session, AgentChatScreenState screenState, int textWidth) {
        List<Line> lines = new ArrayList<>();
        List<AgentChatModel.Entry> entries = session.model().entries();
        for (int index = 0; index < entries.size(); index++) {
            AgentChatModel.Entry entry = entries.get(index);
            if (entry.kind() == AgentChatModel.Kind.REASONING) {
                continue;
            }
            boolean collapsible = entry.kind() == AgentChatModel.Kind.TOOL;
            boolean collapsed = collapsible && screenState.isCollapsed(index);
            String marker = collapsible ? (collapsed ? "[+] " : "[-] ") : "";
            String header = marker + header(entry);
            if (!header.isBlank()) {
                lines.add(new Line(styled(header, color(entry.kind())).getVisualOrderText(), true, index));
            }
            if (!collapsed) {
                int scaledWidth = Math.max(1, Math.round(textWidth / TEXT_SCALE));
                for (String raw : entry.text().split("\\n", -1)) {
                    Component line = markdown(raw);
                    for (FormattedCharSequence sequence : font.split(line, scaledWidth)) {
                        lines.add(new Line(sequence, false, -1));
                    }
                }
            }
            lines.add(new Line(FormattedCharSequence.EMPTY, false, -1));
        }
        return List.copyOf(lines);
    }

    private static void drawWalkingPig(GuiGraphics graphics, int x, int y) {
        int frame = AgentPigAnimation.frame(System.currentTimeMillis());
        int body = 0xFFF2A7A3;
        int dark = 0xFFD98C88;
        int snout = 0xFFE89B97;
        int hoof = 0xFF5A403D;
        int eye = 0xFF2B1C1C;
        graphics.fill(x + 2, y + 4, x + 12, y + 10, body);
        graphics.fill(x + 11, y + 3, x + 16, y + 9, body);
        graphics.fill(x + 12, y + 1, x + 14, y + 4, dark);
        graphics.fill(x + 15, y + 6, x + 17, y + 8, snout);
        graphics.fill(x + 13, y + 5, x + 14, y + 6, eye);
        graphics.fill(x + 1, y + 4, x + 2, y + 6, dark);
        int shift = frame % 2;
        graphics.fill(x + 3 + shift, y + 10, x + 5 + shift, y + 13, dark);
        graphics.fill(x + 6 - shift, y + 10, x + 8 - shift, y + 12, dark);
        graphics.fill(x + 9 + shift, y + 10, x + 11 + shift, y + 13, dark);
        graphics.fill(x + 12 - shift, y + 10, x + 14 - shift, y + 12, dark);
        graphics.fill(x + 3 + shift, y + 12, x + 5 + shift, y + 13, hoof);
        graphics.fill(x + 9 + shift, y + 12, x + 11 + shift, y + 13, hoof);
    }

    private static String header(AgentChatModel.Entry entry) {
        return switch (entry.kind()) {
            case USER -> "You";
            case ASSISTANT -> "Agent";
            case REASONING -> "Reasoning";
            case TOOL -> "Tool: " + entry.title();
            case APPROVAL -> "Approval: " + entry.title();
            case ERROR -> "Error";
            case NOTICE -> "";
        };
    }

    private static int color(AgentChatModel.Kind kind) {
        return switch (kind) {
            case USER -> USER_TEXT;
            case ASSISTANT -> AGENT_TEXT;
            case ERROR -> ERROR_TEXT;
            case TOOL -> TOOL_TEXT;
            case REASONING -> REASONING_TEXT;
            case APPROVAL -> TOOL_TEXT;
            default -> TEXT;
        };
    }

    private static Component styled(String text, int color) {
        return Component.literal(text).withStyle(style -> style.withColor(color));
    }

    private static Component markdown(String line) {
        List<AgentMarkdown.Span> spans = AgentMarkdown.parseLine(line);
        boolean heading = isWholeLineBold(spans);
        MutableComponent result = Component.empty();
        for (AgentMarkdown.Span span : spans) {
            MutableComponent part = Component.literal(span.text());
            Style style = Style.EMPTY;
            if (span.bold()) {
                style = style.withBold(true).withColor(heading ? HEADING_TEXT : SUBTITLE_TEXT);
            }
            if (span.italic()) {
                style = style.withItalic(true);
            }
            if (span.code()) {
                style = style.withColor(CODE_TEXT);
            }
            result.append(part.withStyle(style));
        }
        return result;
    }

    private static boolean isWholeLineBold(List<AgentMarkdown.Span> spans) {
        boolean sawBold = false;
        for (AgentMarkdown.Span span : spans) {
            if (span.text().isBlank()) {
                continue;
            }
            if (!span.bold()) {
                return false;
            }
            sawBold = true;
        }
        return sawBold;
    }

    private static String truncate(Font font, String value, int maxWidth) {
        if (value == null) {
            return "";
        }
        String result = value;
        while (font.width(result) > maxWidth && !result.isEmpty()) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * One clickable transcript entry header.
     *
     * @param entryIndex transcript entry index
     * @param bounds header bounds
     */
    public record EntryHit(int entryIndex, AgentChatGeometry bounds) {}

    private record Line(FormattedCharSequence text, boolean header, int entryIndex) {}

    private record CachedTranscript(long modelRevision, long collapseRevision, int textWidth, List<Line> lines) {}
}
