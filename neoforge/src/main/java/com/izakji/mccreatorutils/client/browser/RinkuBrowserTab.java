package com.izakji.mccreatorutils.client.browser;

import com.izakji.mccreatorutils.util.SafeClose;
import de.keksuccino.rinku.Rinku;
import de.keksuccino.rinku.RinkuBrowser;
import de.keksuccino.rinku.RinkuBrowserTextureBlitter;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Rinku-backed browser tab.
 *
 * @author Izakji
 */
public final class RinkuBrowserTab implements BrowserTabHandle {
    private final String initialUrl;
    private RinkuBrowser browser;
    private boolean disposed;

    public RinkuBrowserTab(String initialUrl) {
        this.initialUrl = initialUrl;
        this.browser = Rinku.createBrowser(initialUrl, false);
        this.browser.useBrowserControls(true);
    }

    @Override
    public String url() {
        if (browser == null) {
            return initialUrl;
        }
        String current = browser.getURL();
        return current == null || current.isBlank() ? initialUrl : current;
    }

    @Override
    public void load(String url) {
        if (browser != null) {
            browser.loadURL(url);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (browser != null) {
            browser.resize(Math.max(1, width), Math.max(1, height));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (browser != null && browser.isTextureReady()) {
            RinkuBrowserTextureBlitter.blit(graphics, browser, x, y, width, height);
        }
    }

    @Override
    public boolean isTextureReady() {
        return browser != null && browser.isTextureReady();
    }

    @Override
    public boolean isLoading() {
        return browser != null && browser.isLoading();
    }

    @Override
    public boolean canGoBack() {
        return browser != null && browser.canGoBack();
    }

    @Override
    public boolean canGoForward() {
        return browser != null && browser.canGoForward();
    }

    @Override
    public void goBack() {
        if (browser != null) {
            browser.goBack();
        }
    }

    @Override
    public void goForward() {
        if (browser != null) {
            browser.goForward();
        }
    }

    @Override
    public void reload() {
        if (browser != null) {
            browser.reload();
        }
    }

    @Override
    public void setFocus(boolean focused) {
        if (browser != null) {
            browser.setFocus(focused);
        }
    }

    @Override
    public void sendMousePress(int x, int y, int button) {
        if (browser != null) {
            browser.sendMousePress(x, y, button);
        }
    }

    @Override
    public void sendMouseRelease(int x, int y, int button) {
        if (browser != null) {
            browser.sendMouseRelease(x, y, button);
        }
    }

    @Override
    public void sendMouseMove(int x, int y) {
        if (browser != null) {
            browser.sendMouseMove(x, y);
        }
    }

    @Override
    public void sendMouseWheel(int x, int y, double amount, int modifiers) {
        if (browser != null) {
            browser.sendMouseWheel(x, y, amount, modifiers);
        }
    }

    @Override
    public void sendKeyPress(int keyCode, int scanCode, int modifiers) {
        if (browser != null) {
            browser.sendKeyPress(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void sendKeyRelease(int keyCode, int scanCode, int modifiers) {
        if (browser != null) {
            browser.sendKeyRelease(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void sendKeyTyped(char codePoint, int modifiers) {
        if (browser != null) {
            browser.sendKeyTyped(codePoint, modifiers);
        }
    }

    @Override
    public void close() {
        if (disposed) {
            return;
        }
        disposed = true;
        RinkuBrowser current = browser;
        browser = null;
        if (current != null) {
            SafeClose.run(current::close);
        }
    }
}
