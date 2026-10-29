package com.izakji.mccreatorutils.client.explore;

import com.izakji.mccreatorutils.util.SafeClose;
import de.keksuccino.rinku.Rinku;
import de.keksuccino.rinku.RinkuBrowser;
import de.keksuccino.rinku.RinkuBrowserTextureBlitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Full-screen Rinku browser used by the Explore Mods tool.
 *
 * @author Izakji
 */
public final class ModBrowserScreen extends Screen {
    private static final int MARGIN = 10;
    private static final int NAV_Y = 24;
    private static final int NAV_WIDTH = 24;
    private static final int NAV_HEIGHT = 20;
    private static final int BROWSER_Y = 50;
    private static final int BACKGROUND_COLOR = 0xF0101010;

    private final String initialUrl;
    private RinkuBrowser browser;
    private Button backButton;
    private Button forwardButton;
    private Button reloadButton;
    private Component failure;

    public ModBrowserScreen(String initialUrl) {
        super(Component.translatable("screen.mccreatorutils.mod_browser"));
        this.initialUrl = initialUrl;
    }

    @Override
    protected void init() {
        super.init();
        if (browser == null && failure == null) {
            createBrowser();
        }

        int x = MARGIN;
        backButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> goBack())
                .bounds(x, NAV_Y, NAV_WIDTH, NAV_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.mccreatorutils.mod_browser.back")))
                .build());
        x += NAV_WIDTH + 4;
        forwardButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> goForward())
                .bounds(x, NAV_Y, NAV_WIDTH, NAV_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.mccreatorutils.mod_browser.forward")))
                .build());
        x += NAV_WIDTH + 4;
        reloadButton = addRenderableWidget(Button.builder(Component.literal("R"), button -> reload())
                .bounds(x, NAV_Y, NAV_WIDTH, NAV_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.mccreatorutils.mod_browser.reload")))
                .build());
        addRenderableWidget(Button.builder(Component.literal("X"), button -> onClose())
                .bounds(width - MARGIN - NAV_WIDTH, NAV_Y, NAV_WIDTH, NAV_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.mccreatorutils.mod_browser.close")))
                .build());

        resizeBrowser();
        updateNavigationState();
    }

    private void createBrowser() {
        try {
            if (!Rinku.isInitialized()) {
                failure = Component.translatable(
                        Rinku.isInitializationAllowed()
                                ? "screen.mccreatorutils.mod_browser.not_ready"
                                : "screen.mccreatorutils.mod_browser.failure");
                return;
            }
            browser = Rinku.createBrowser(initialUrl, false);
            browser.useBrowserControls(true);
            browser.setFocus(true);
        } catch (RuntimeException | LinkageError exception) {
            browser = null;
            failure = Component.translatable("screen.mccreatorutils.mod_browser.failure");
        }
    }

    private int browserX() {
        return MARGIN;
    }

    private int browserWidth() {
        return Math.max(1, width - MARGIN * 2);
    }

    private int browserHeight() {
        return Math.max(1, height - BROWSER_Y - MARGIN);
    }

    private boolean isInBrowserBounds(double mouseX, double mouseY) {
        return mouseX >= browserX()
                && mouseY >= BROWSER_Y
                && mouseX < browserX() + browserWidth()
                && mouseY < BROWSER_Y + browserHeight();
    }

    private int browserMouseX(double x) {
        return (int) ((x - browserX()) * minecraft.getWindow().getGuiScale());
    }

    private int browserMouseY(double y) {
        return (int) ((y - BROWSER_Y) * minecraft.getWindow().getGuiScale());
    }

    private void resizeBrowser() {
        if (browser != null) {
            browser.resize(
                    Math.max(1, (int) (browserWidth() * minecraft.getWindow().getGuiScale())),
                    Math.max(1, (int) (browserHeight() * minecraft.getWindow().getGuiScale())));
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        resizeBrowser();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND_COLOR);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);

        if (failure != null) {
            graphics.drawCenteredString(font, failure, width / 2, height / 2, 0xFF5555);
            return;
        }
        if (browser == null) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.mccreatorutils.mod_browser.loading"),
                    width / 2,
                    height / 2,
                    0xFFFFFF);
            return;
        }
        if (browser.isTextureReady()) {
            RinkuBrowserTextureBlitter.blit(graphics, browser, browserX(), BROWSER_Y, browserWidth(), browserHeight());
        } else {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.mccreatorutils.mod_browser.loading"),
                    width / 2,
                    height / 2,
                    0xFFFFFF);
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateNavigationState();
    }

    private void updateNavigationState() {
        if (backButton != null) {
            backButton.active = browser != null && browser.canGoBack();
        }
        if (forwardButton != null) {
            forwardButton.active = browser != null && browser.canGoForward();
        }
        if (reloadButton != null) {
            reloadButton.active = browser != null;
        }
    }

    private void goBack() {
        if (browser != null) {
            browser.goBack();
        }
    }

    private void goForward() {
        if (browser != null) {
            browser.goForward();
        }
    }

    private void reload() {
        if (browser != null) {
            browser.reload();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (browser == null || failure != null || !isInBrowserBounds(mouseX, mouseY)) {
            return false;
        }
        setFocused(null);
        browser.sendMousePress(browserMouseX(mouseX), browserMouseY(mouseY), button);
        browser.setFocus(true);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (super.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (browser == null || failure != null) {
            return false;
        }
        browser.sendMouseRelease(browserMouseX(mouseX), browserMouseY(mouseY), button);
        browser.setFocus(true);
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (browser != null && failure == null && isInBrowserBounds(mouseX, mouseY)) {
            browser.sendMouseMove(browserMouseX(mouseX), browserMouseY(mouseY));
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (browser != null && failure == null && isInBrowserBounds(mouseX, mouseY)) {
            browser.sendMouseMove(browserMouseX(mouseX), browserMouseY(mouseY));
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (browser == null || failure != null || !isInBrowserBounds(mouseX, mouseY)) {
            return false;
        }
        browser.sendMouseWheel(browserMouseX(mouseX), browserMouseY(mouseY), scrollY, 0);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (browser == null || failure != null) {
            return false;
        }
        browser.sendKeyPress(keyCode, scanCode, modifiers);
        browser.setFocus(true);
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (super.keyReleased(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (browser == null || failure != null) {
            return false;
        }
        browser.sendKeyRelease(keyCode, scanCode, modifiers);
        browser.setFocus(true);
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (super.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (browser == null || failure != null || codePoint == 0) {
            return false;
        }
        browser.sendKeyTyped(codePoint, modifiers);
        browser.setFocus(true);
        return true;
    }

    private void closeBrowser() {
        RinkuBrowser current = browser;
        browser = null;
        if (current != null) {
            SafeClose.run(current::close);
        }
    }

    @Override
    public void onClose() {
        closeBrowser();
        super.onClose();
    }

    @Override
    public void removed() {
        closeBrowser();
        super.removed();
    }
}
