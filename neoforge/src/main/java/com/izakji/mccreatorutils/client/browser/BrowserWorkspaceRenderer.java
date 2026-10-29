package com.izakji.mccreatorutils.client.browser;

import com.izakji.mccreatorutils.McCreatorUtils;
import com.izakji.mccreatorutils.browser.BrowserGeometry;
import com.izakji.mccreatorutils.browser.BrowserLayout;
import com.izakji.mccreatorutils.browser.BrowserTabRules;
import java.net.URI;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the browser window chrome and active Rinku texture.
 *
 * @author Izakji
 */
public final class BrowserWorkspaceRenderer {
    private static final int WINDOW_BACKGROUND = 0xFF202020;
    private static final int WINDOW_BORDER = 0xFF000000;
    private static final int TITLE_BACKGROUND = 0xFF2B2B2B;
    private static final int TAB_BACKGROUND = 0xFF1A1A1A;
    private static final int TAB_ACTIVE = 0xFF3A3A3A;
    private static final int NAV_BACKGROUND = 0xFF242424;
    private static final int ADDRESS_BACKGROUND = 0xFF101010;
    private static final int CONTENT_BACKGROUND = 0xFF0C0C0C;
    private static final int TEXT = 0xFFD0D0D0;
    private static final int MUTED_TEXT = 0xFF8A8A8A;
    private static final int LOADING = 0xFFFFD45A;
    private static final int TAB_GAP = 2;
    private static final int TAB_START = 2;
    private static final int PLUS_WIDTH = 16;
    private static final int TAB_CLOSE_SIZE = 8;
    static final int TITLE_BUTTON_WIDTH = 18;
    private static final int TITLE_ICON_SIZE = 16;
    private static final int TITLE_ICON_CONTENT_SIZE = 12;
    private static final ResourceLocation CLOSE_ICON =
            ResourceLocation.fromNamespaceAndPath(McCreatorUtils.MOD_ID, "textures/gui/close.png");
    private static final ResourceLocation FULLSCREEN_ICON =
            ResourceLocation.fromNamespaceAndPath(McCreatorUtils.MOD_ID, "textures/gui/fullscreen.png");

    private BrowserWorkspaceRenderer() {}

    /**
     * Renders the workspace window.
     *
     * @param graphics graphics
     * @param workspace workspace
     * @param showAddressText whether to draw the URL text (HUD) or leave it to the focus EditBox
     */
    public static void render(GuiGraphics graphics, BrowserWorkspace workspace, boolean showAddressText) {
        BrowserGeometry window = workspace.windowBounds();
        BrowserLayout layout = BrowserLayout.of(window);
        Minecraft minecraft = Minecraft.getInstance();

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
                layout.titleBar().x(),
                layout.titleBar().bottom() - 1,
                layout.titleBar().right(),
                layout.titleBar().bottom(),
                WINDOW_BORDER);

        String title = workspace.activeUrl().isBlank()
                ? Component.translatable("screen.mccreatorutils.browser").getString()
                : tabLabel(workspace.activeUrl());
        graphics.drawString(
                minecraft.font,
                truncate(minecraft, title, layout.titleBar().width() - 48),
                layout.titleBar().x() + 5,
                layout.titleBar().y() + 4,
                TEXT,
                false);
        int iconY = layout.titleBar().y();
        int iconRight = layout.titleBar().right();
        graphics.blit(
                FULLSCREEN_ICON,
                iconRight - TITLE_BUTTON_WIDTH * 2 + 3,
                iconY + 2,
                0.0F,
                0.0F,
                TITLE_ICON_CONTENT_SIZE,
                TITLE_ICON_CONTENT_SIZE,
                TITLE_ICON_SIZE,
                TITLE_ICON_SIZE);
        graphics.blit(
                CLOSE_ICON,
                iconRight - TITLE_BUTTON_WIDTH + 1,
                iconY,
                0.0F,
                0.0F,
                TITLE_ICON_SIZE,
                TITLE_ICON_SIZE,
                TITLE_ICON_SIZE,
                TITLE_ICON_SIZE);

        drawTabs(graphics, workspace, layout);
        drawNavigation(graphics, layout, minecraft);
        drawAddress(graphics, workspace, layout, minecraft, showAddressText);
        drawContent(graphics, workspace, layout, minecraft);
    }

    private static void drawTabs(GuiGraphics graphics, BrowserWorkspace workspace, BrowserLayout layout) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.fill(
                layout.tabStrip().x(),
                layout.tabStrip().y(),
                layout.tabStrip().right(),
                layout.tabStrip().bottom(),
                TAB_BACKGROUND);
        List<BrowserTabHandle> tabs = workspace.tabs();
        int available = layout.tabStrip().width() - TAB_START - PLUS_WIDTH - 4;
        int tabWidth = tabs.isEmpty() ? 0 : Math.max(44, available / tabs.size());
        int x = layout.tabStrip().x() + TAB_START;
        for (int index = 0; index < tabs.size(); index++) {
            int right = Math.min(layout.tabStrip().right() - PLUS_WIDTH - 4, x + tabWidth - TAB_GAP);
            if (right <= x) {
                break;
            }
            graphics.fill(
                    x,
                    layout.tabStrip().y() + 2,
                    right,
                    layout.tabStrip().bottom() - 2,
                    index == workspace.activeTabIndex() ? TAB_ACTIVE : TAB_BACKGROUND);
            String label = truncate(minecraft, tabLabel(tabs.get(index).url()), right - x - 22);
            graphics.drawString(
                    minecraft.font,
                    label,
                    x + 4,
                    layout.tabStrip().y() + 5,
                    index == workspace.activeTabIndex() ? TEXT : MUTED_TEXT,
                    false);
            graphics.blit(
                    CLOSE_ICON,
                    right - 12,
                    layout.tabStrip().y() + 4,
                    0.0F,
                    0.0F,
                    TAB_CLOSE_SIZE,
                    TAB_CLOSE_SIZE,
                    TITLE_ICON_SIZE,
                    TITLE_ICON_SIZE);
            x = right + TAB_GAP;
        }
        graphics.drawString(
                minecraft.font,
                "+",
                layout.tabStrip().right() - PLUS_WIDTH,
                layout.tabStrip().y() + 4,
                BrowserTabRules.canOpenNewTab(tabs.size()) ? TEXT : MUTED_TEXT,
                false);
    }

    private static void drawNavigation(GuiGraphics graphics, BrowserLayout layout, Minecraft minecraft) {
        graphics.fill(
                layout.navBar().x(),
                layout.navBar().y(),
                layout.navBar().right(),
                layout.navBar().bottom(),
                NAV_BACKGROUND);
        graphics.drawString(
                minecraft.font, "<", layout.navBar().x() + 6, layout.navBar().y() + 6, TEXT, false);
        graphics.drawString(
                minecraft.font, ">", layout.navBar().x() + 26, layout.navBar().y() + 6, TEXT, false);
        graphics.drawString(
                minecraft.font, "R", layout.navBar().x() + 46, layout.navBar().y() + 6, TEXT, false);
    }

    private static void drawAddress(
            GuiGraphics graphics,
            BrowserWorkspace workspace,
            BrowserLayout layout,
            Minecraft minecraft,
            boolean showAddressText) {
        graphics.fill(
                layout.addressBar().x() + 4,
                layout.addressBar().y() + 2,
                layout.addressBar().right() - 4,
                layout.addressBar().bottom() - 2,
                ADDRESS_BACKGROUND);
        if (showAddressText) {
            graphics.drawString(
                    minecraft.font,
                    truncate(
                            minecraft,
                            workspace.activeUrl(),
                            layout.addressBar().width() - 16),
                    layout.addressBar().x() + 8,
                    layout.addressBar().y() + 5,
                    MUTED_TEXT,
                    false);
        }
    }

    private static void drawContent(
            GuiGraphics graphics, BrowserWorkspace workspace, BrowserLayout layout, Minecraft minecraft) {
        BrowserGeometry content = layout.content();
        graphics.fill(content.x(), content.y(), content.right(), content.bottom(), CONTENT_BACKGROUND);
        BrowserTabHandle active = workspace.activeTab();
        if (active == null) {
            return;
        }
        if (active.isTextureReady()) {
            active.render(graphics, content.x(), content.y(), content.width(), content.height());
        } else {
            graphics.drawCenteredString(
                    minecraft.font,
                    Component.translatable("screen.mccreatorutils.browser.loading"),
                    content.x() + content.width() / 2,
                    content.y() + content.height() / 2,
                    LOADING);
        }
        if (active.isLoading() && active.isTextureReady()) {
            graphics.drawCenteredString(
                    minecraft.font,
                    Component.translatable("screen.mccreatorutils.browser.loading"),
                    content.x() + content.width() / 2,
                    content.y() + content.height() - 12,
                    LOADING);
        }
        if (!workspace.fullscreen()) {
            BrowserGeometry grip = layout.resizeGrip();
            graphics.fill(grip.right() - 2, grip.bottom() - 10, grip.right(), grip.bottom() - 8, MUTED_TEXT);
            graphics.fill(grip.right() - 6, grip.bottom() - 6, grip.right(), grip.bottom() - 4, MUTED_TEXT);
        }
    }

    private static String tabLabel(String url) {
        if (url == null || url.isBlank() || url.equals("about:blank")) {
            return Component.translatable("screen.mccreatorutils.browser.new_tab")
                    .getString();
        }
        try {
            String host = URI.create(url).getHost();
            return host == null || host.isBlank() ? url : host;
        } catch (RuntimeException malformed) {
            return url;
        }
    }

    private static String truncate(Minecraft minecraft, String value, int maxWidth) {
        if (value == null) {
            return "";
        }
        if (maxWidth <= 0) {
            return "";
        }
        String result = value;
        while (minecraft.font.width(result) > maxWidth && !result.isEmpty()) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
