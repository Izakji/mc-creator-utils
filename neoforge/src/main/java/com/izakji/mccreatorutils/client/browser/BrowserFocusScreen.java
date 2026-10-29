package com.izakji.mccreatorutils.client.browser;

import com.izakji.mccreatorutils.browser.BrowserGeometry;
import com.izakji.mccreatorutils.browser.BrowserInputCoordinates;
import com.izakji.mccreatorutils.browser.BrowserLayout;
import com.izakji.mccreatorutils.browser.BrowserTabRules;
import com.izakji.mccreatorutils.browser.BrowserUrlInput;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Transparent focus screen for the browser workspace.
 *
 * <p>The floating window is rendered here with a released cursor. Clicking outside the window
 * returns control to Minecraft while the workspace remains visible.
 *
 * @author Izakji
 */
public final class BrowserFocusScreen extends Screen {
    private static final int TAB_START = 2;
    private static final int TAB_GAP = 2;
    private static final int TAB_MIN_WIDTH = 44;
    private static final int PLUS_WIDTH = 16;
    private static final int NAV_BUTTON_WIDTH = 20;
    private static final int TITLE_BUTTON_WIDTH = BrowserWorkspaceRenderer.TITLE_BUTTON_WIDTH;

    private final BrowserWorkspace workspace = BrowserWorkspace.instance();
    private EditBox addressBar;
    private BrowserLayout layout;
    private boolean contentFocused;
    private boolean draggingWindow;
    private boolean resizingWindow;
    private int dragOffsetX;
    private int dragOffsetY;
    private int resizeStartWidth;
    private int resizeStartHeight;
    private double resizeStartMouseX;
    private double resizeStartMouseY;
    private Component error;

    public BrowserFocusScreen() {
        super(Component.translatable("screen.mccreatorutils.browser"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        layout = BrowserLayout.of(workspace.windowBounds());
        addressBar = new EditBox(
                font,
                layout.addressBar().x() + 5,
                layout.addressBar().y() + 2,
                Math.max(1, layout.addressBar().width() - 10),
                12,
                Component.translatable("screen.mccreatorutils.browser.address"));
        addressBar.setMaxLength(2048);
        addressBar.setValue(workspace.activeUrl());
        addRenderableWidget(addressBar);
        workspace.resizeActiveTab();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        layout = BrowserLayout.of(workspace.windowBounds());
        positionAddressBar();
        BrowserWorkspaceRenderer.render(graphics, workspace, false);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (error != null) {
            graphics.drawString(
                    font, error, layout.window().x() + 5, layout.window().bottom() - 15, 0xFF5555, false);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the game world visible behind the floating browser.
    }

    @Override
    public void tick() {
        super.tick();
        if (addressBar != null && !addressBar.isFocused()) {
            String active = workspace.activeUrl();
            if (!addressBar.getValue().equals(active)) {
                addressBar.setValue(active);
            }
        }
    }

    private void positionAddressBar() {
        if (addressBar == null) {
            return;
        }
        addressBar.setX(layout.addressBar().x() + 5);
        addressBar.setY(layout.addressBar().y() + 2);
        addressBar.setWidth(Math.max(1, layout.addressBar().width() - 10));
    }

    private boolean in(BrowserGeometry rectangle, double mouseX, double mouseY) {
        return mouseX >= rectangle.x()
                && mouseY >= rectangle.y()
                && mouseX < rectangle.right()
                && mouseY < rectangle.bottom();
    }

    private boolean inWindow(double mouseX, double mouseY) {
        return in(layout.window(), mouseX, mouseY);
    }

    private int contentPixelX(double mouseX) {
        return BrowserInputCoordinates.toTexturePixel(
                mouseX,
                layout.content().x(),
                Minecraft.getInstance().getWindow().getGuiScale());
    }

    private int contentPixelY(double mouseY) {
        return BrowserInputCoordinates.toTexturePixel(
                mouseY,
                layout.content().y(),
                Minecraft.getInstance().getWindow().getGuiScale());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout = BrowserLayout.of(workspace.windowBounds());
        if (in(layout.addressBar(), mouseX, mouseY)) {
            clearContentFocus();
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (!inWindow(mouseX, mouseY)) {
            onClose();
            return true;
        }

        if (in(layout.titleBar(), mouseX, mouseY)) {
            clearContentFocus();
            double titleRight = layout.titleBar().right();
            if (mouseX >= titleRight - TITLE_BUTTON_WIDTH) {
                workspace.closeWindow();
                return true;
            }
            if (mouseX >= titleRight - TITLE_BUTTON_WIDTH * 2) {
                onClose();
                return true;
            }
            if (mouseX >= titleRight - TITLE_BUTTON_WIDTH * 3) {
                workspace.toggleFullscreen();
                layout = BrowserLayout.of(workspace.windowBounds());
                positionAddressBar();
                return true;
            }
            if (!workspace.fullscreen()) {
                draggingWindow = true;
                dragOffsetX = (int) mouseX - layout.window().x();
                dragOffsetY = (int) mouseY - layout.window().y();
            }
            return true;
        }

        if (in(layout.tabStrip(), mouseX, mouseY)) {
            clearContentFocus();
            handleTabClick(mouseX);
            return true;
        }

        if (in(layout.navBar(), mouseX, mouseY)) {
            clearContentFocus();
            handleNavigationClick(mouseX);
            return true;
        }

        if (in(layout.addressBar(), mouseX, mouseY)) {
            clearContentFocus();
            focusAddressBar();
            return true;
        }

        if (in(layout.resizeGrip(), mouseX, mouseY) && !workspace.fullscreen()) {
            clearContentFocus();
            resizingWindow = true;
            resizeStartWidth = layout.window().width();
            resizeStartHeight = layout.window().height();
            resizeStartMouseX = mouseX;
            resizeStartMouseY = mouseY;
            return true;
        }

        if (in(layout.content(), mouseX, mouseY)) {
            BrowserTabHandle active = workspace.activeTab();
            if (active != null) {
                clearContentFocus();
                addressBar.setFocused(false);
                setFocused(null);
                contentFocused = true;
                active.setFocus(true);
                active.sendMousePress(contentPixelX(mouseX), contentPixelY(mouseY), button);
                return true;
            }
        }
        return false;
    }

    private void handleTabClick(double mouseX) {
        List<BrowserTabHandle> tabs = workspace.tabs();
        if (mouseX >= layout.tabStrip().right() - PLUS_WIDTH) {
            if (!BrowserTabRules.canOpenNewTab(tabs.size())) {
                error = Component.translatable("screen.mccreatorutils.browser.tab_limit");
                return;
            }
            BrowserOpenResult result = workspace.openNewTab();
            if (result == BrowserOpenResult.OPENED) {
                addressBar.setValue(workspace.activeUrl());
                focusAddressBar();
            }
            return;
        }
        int available = layout.tabStrip().width() - TAB_START - PLUS_WIDTH - 4;
        int tabWidth = tabs.isEmpty() ? 0 : Math.max(TAB_MIN_WIDTH, available / tabs.size());
        int x = layout.tabStrip().x() + TAB_START;
        for (int index = 0; index < tabs.size(); index++) {
            int right = Math.min(layout.tabStrip().right() - PLUS_WIDTH - 4, x + tabWidth - TAB_GAP);
            if (right <= x) {
                return;
            }
            if (mouseX >= x && mouseX < right) {
                if (mouseX >= right - 12) {
                    workspace.closeTab(index);
                } else {
                    workspace.setActiveTab(index);
                }
                return;
            }
            x = right + TAB_GAP;
        }
    }

    private void handleNavigationClick(double mouseX) {
        double relative = mouseX - layout.navBar().x();
        BrowserTabHandle active = workspace.activeTab();
        if (active == null) {
            return;
        }
        if (relative < NAV_BUTTON_WIDTH) {
            active.goBack();
        } else if (relative < NAV_BUTTON_WIDTH * 2) {
            active.goForward();
        } else if (relative < NAV_BUTTON_WIDTH * 3) {
            active.reload();
        }
    }

    private void focusAddressBar() {
        if (addressBar == null) {
            return;
        }
        setFocused(addressBar);
        addressBar.setFocused(true);
    }

    private void clearContentFocus() {
        if (contentFocused) {
            BrowserTabHandle active = workspace.activeTab();
            if (active != null) {
                active.setFocus(false);
            }
        }
        contentFocused = false;
        if (addressBar != null) {
            addressBar.setFocused(false);
        }
        setFocused(null);
    }

    private void navigateFromAddressBar() {
        Optional<String> normalized = BrowserUrlInput.normalize(addressBar.getValue());
        BrowserTabHandle active = workspace.activeTab();
        if (normalized.isEmpty()) {
            error = Component.translatable("screen.mccreatorutils.browser.invalid_url");
            focusAddressBar();
            return;
        }
        if (active == null) {
            return;
        }
        error = null;
        active.load(normalized.get());
        addressBar.setValue(normalized.get());
        addressBar.setFocused(false);
        setFocused(null);
        contentFocused = true;
        active.setFocus(true);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = draggingWindow;
        boolean wasResizing = resizingWindow;
        draggingWindow = false;
        resizingWindow = false;
        if (super.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (wasDragging || wasResizing) {
            return true;
        }
        if (contentFocused) {
            BrowserTabHandle active = workspace.activeTab();
            if (active != null) {
                active.sendMouseRelease(contentPixelX(mouseX), contentPixelY(mouseY), button);
            }
            return true;
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (contentFocused && layout != null && in(layout.content(), mouseX, mouseY)) {
            BrowserTabHandle active = workspace.activeTab();
            if (active != null) {
                active.sendMouseMove(contentPixelX(mouseX), contentPixelY(mouseY));
            }
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingWindow) {
            workspace.setWindowBounds(new BrowserGeometry(
                    (int) mouseX - dragOffsetX,
                    (int) mouseY - dragOffsetY,
                    layout.window().width(),
                    layout.window().height()));
            layout = BrowserLayout.of(workspace.windowBounds());
            positionAddressBar();
            return true;
        }
        if (resizingWindow) {
            int width = resizeStartWidth + (int) (mouseX - resizeStartMouseX);
            int height = resizeStartHeight + (int) (mouseY - resizeStartMouseY);
            workspace.setWindowBounds(
                    new BrowserGeometry(layout.window().x(), layout.window().y(), width, height));
            layout = BrowserLayout.of(workspace.windowBounds());
            positionAddressBar();
            return true;
        }
        if (contentFocused && in(layout.content(), mouseX, mouseY)) {
            BrowserTabHandle active = workspace.activeTab();
            if (active != null) {
                active.sendMouseMove(contentPixelX(mouseX), contentPixelY(mouseY));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (contentFocused && in(layout.content(), mouseX, mouseY)) {
            BrowserTabHandle active = workspace.activeTab();
            if (active != null) {
                active.sendMouseWheel(contentPixelX(mouseX), contentPixelY(mouseY), scrollY, 0);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (addressBar != null && addressBar.isFocused()) {
                addressBar.setFocused(false);
                setFocused(null);
                return true;
            }
            if (workspace.fullscreen()) {
                workspace.toggleFullscreen();
                layout = BrowserLayout.of(workspace.windowBounds());
                positionAddressBar();
            } else {
                onClose();
            }
            return true;
        }
        if (addressBar != null && addressBar.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                navigateFromAddressBar();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (contentFocused) {
            BrowserTabHandle active = workspace.activeTab();
            if (active != null) {
                active.sendKeyPress(keyCode, scanCode, modifiers);
                active.setFocus(true);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (addressBar != null && addressBar.isFocused()) {
            return super.keyReleased(keyCode, scanCode, modifiers);
        }
        if (contentFocused) {
            BrowserTabHandle active = workspace.activeTab();
            if (active != null) {
                active.sendKeyRelease(keyCode, scanCode, modifiers);
                active.setFocus(true);
                return true;
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (addressBar != null && addressBar.isFocused()) {
            return super.charTyped(codePoint, modifiers);
        }
        if (contentFocused && codePoint != 0) {
            BrowserTabHandle active = workspace.activeTab();
            if (active != null) {
                active.sendKeyTyped(codePoint, modifiers);
                active.setFocus(true);
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        if (workspace.fullscreen()) {
            workspace.toggleFullscreen();
            layout = BrowserLayout.of(workspace.windowBounds());
            positionAddressBar();
        }
        clearContentFocus();
        BrowserTabHandle active = workspace.activeTab();
        if (active != null) {
            active.setFocus(false);
        }
        super.onClose();
    }

    @Override
    public void removed() {
        clearContentFocus();
        super.removed();
    }
}
