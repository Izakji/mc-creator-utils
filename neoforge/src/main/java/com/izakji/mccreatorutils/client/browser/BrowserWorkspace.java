package com.izakji.mccreatorutils.client.browser;

import com.izakji.mccreatorutils.browser.BrowserGeometry;
import com.izakji.mccreatorutils.browser.BrowserLayout;
import com.izakji.mccreatorutils.browser.BrowserSession;
import com.izakji.mccreatorutils.browser.BrowserTabRules;
import com.izakji.mccreatorutils.browser.BrowserUrlInput;
import com.izakji.mccreatorutils.client.CreatorToolsScreen;
import com.izakji.mccreatorutils.util.SafeClose;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;

/**
 * Client-side owner of the floating browser window and its tabs.
 *
 * <p>This class is deliberately Rinku-free. Rinku-specific creation is isolated in
 * {@link RinkuBrowserEngine} and loaded only after {@link #isAvailable()} succeeds.
 *
 * @author Izakji
 */
public final class BrowserWorkspace {
    private static final BrowserWorkspace INSTANCE = new BrowserWorkspace();
    private static final int FULLSCREEN_MARGIN = 8;

    private final java.util.ArrayList<BrowserTabHandle> tabs = new java.util.ArrayList<>();
    private final BrowserSession savedSession = new BrowserSession();
    private BrowserEngine engine;
    private int activeTabIndex = -1;
    private BrowserGeometry windowBounds;
    private BrowserGeometry restoreBounds;
    private boolean fullscreen;
    private boolean windowVisible;

    private BrowserWorkspace() {}

    /**
     * Returns the shared workspace.
     *
     * @return workspace singleton
     */
    public static BrowserWorkspace instance() {
        return INSTANCE;
    }

    /**
     * Returns whether Rinku is installed.
     *
     * @return true when Rinku is loaded
     */
    public boolean isAvailable() {
        return ModList.get().isLoaded("rinku");
    }

    /**
     * Returns whether the floating browser window has tabs.
     *
     * @return true when visible tabs exist
     */
    public boolean hasWindow() {
        return windowVisible && !tabs.isEmpty();
    }

    /**
     * Opens a page, reusing an existing normalized URL tab.
     *
     * @param url page URL
     * @return open result
     */
    public BrowserOpenResult openPage(String url) {
        Optional<String> normalized = BrowserUrlInput.normalize(url);
        if (normalized.isEmpty()) {
            return BrowserOpenResult.FAILED;
        }
        if (!isAvailable()) {
            return BrowserOpenResult.UNAVAILABLE;
        }
        if (tabs.isEmpty() && savedSession.hasPages()) {
            restoreSavedSession();
        }
        return openUrl(normalized.get(), true);
    }

    /**
     * Opens a new blank tab.
     *
     * @return open result
     */
    public BrowserOpenResult openNewTab() {
        if (!isAvailable()) {
            return BrowserOpenResult.UNAVAILABLE;
        }
        return openUrl("about:blank", false);
    }

    private BrowserOpenResult openUrl(String url, boolean reuseExisting) {
        if (reuseExisting) {
            OptionalInt existing = BrowserTabRules.findOpenTab(
                    tabs.stream().map(BrowserTabHandle::url).toList(), url);
            if (existing.isPresent()) {
                activeTabIndex = existing.getAsInt();
                ensureWindowBounds();
                windowVisible = true;
                resizeActiveTab();
                focus();
                return BrowserOpenResult.FOCUSED_EXISTING;
            }
        }
        if (!BrowserTabRules.canOpenNewTab(tabs.size())) {
            return BrowserOpenResult.CAPACITY;
        }

        BrowserEngine currentEngine = ensureEngine();
        if (currentEngine == null) {
            return BrowserOpenResult.FAILED;
        }
        BrowserTabHandle tab;
        try {
            tab = currentEngine.create(url);
        } catch (RuntimeException | LinkageError failure) {
            return BrowserOpenResult.FAILED;
        }
        tabs.add(tab);
        activeTabIndex = tabs.size() - 1;
        ensureWindowBounds();
        windowVisible = true;
        resizeActiveTab();
        focus();
        return BrowserOpenResult.OPENED;
    }

    /**
     * Toggles the floating browser window without disk persistence.
     *
     * @return true when the browser is visible after the toggle
     */
    public boolean toggleBrowser() {
        if (!isAvailable()) {
            return false;
        }
        if (hasWindow()) {
            closeWindow();
            return false;
        }
        if (savedSession.hasPages()) {
            if (!restoreSavedSession()) {
                return false;
            }
        } else if (openNewTab() != BrowserOpenResult.OPENED) {
            return false;
        }
        focus();
        return hasWindow();
    }

    /**
     * Opens focus mode when the floating window exists.
     */
    public void focus() {
        if (!hasWindow()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null || minecraft.screen instanceof CreatorToolsScreen) {
            minecraft.setScreen(new BrowserFocusScreen());
        }
    }

    /**
     * Closes one tab and updates the active index.
     *
     * @param index tab index
     */
    public void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }
        int oldSize = tabs.size();
        BrowserTabHandle removed = tabs.remove(index);
        SafeClose.run(removed::close);
        if (tabs.isEmpty()) {
            savedSession.clear();
            closeWindow();
            return;
        }
        activeTabIndex = BrowserTabRules.nextActiveIndex(activeTabIndex, index, oldSize);
        resizeActiveTab();
    }

    /**
     * Activates a tab by index.
     *
     * @param index tab index
     */
    public void setActiveTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            activeTabIndex = index;
            resizeActiveTab();
        }
    }

    /**
     * Resizes the active browser to the current content rectangle.
     */
    public void resizeActiveTab() {
        BrowserTabHandle active = activeTab();
        if (active == null) {
            return;
        }
        ensureWindowBounds();
        BrowserLayout layout = BrowserLayout.of(windowBounds);
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        active.resize(Math.max(1, (int) (layout.content().width() * scale)), Math.max(1, (int)
                (layout.content().height() * scale)));
    }

    /**
     * Closes every tab, snapshots the open pages, and hides the workspace.
     */
    public void closeWindow() {
        if (!tabs.isEmpty()) {
            savedSession.save(tabs.stream().map(BrowserTabHandle::url).toList(), activeTabIndex);
        }
        for (BrowserTabHandle tab : List.copyOf(tabs)) {
            SafeClose.run(tab::close);
        }
        tabs.clear();
        activeTabIndex = -1;
        engine = null;
        windowVisible = false;
        fullscreen = false;
        restoreBounds = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BrowserFocusScreen) {
            minecraft.setScreen(null);
        }
    }

    /**
     * Returns the active tab.
     *
     * @return active tab, or null
     */
    public BrowserTabHandle activeTab() {
        return activeTabIndex >= 0 && activeTabIndex < tabs.size() ? tabs.get(activeTabIndex) : null;
    }

    /**
     * Returns the active tab index.
     *
     * @return active index, or -1
     */
    public int activeTabIndex() {
        return activeTabIndex;
    }

    /**
     * Returns an immutable tab snapshot.
     *
     * @return tabs
     */
    public List<BrowserTabHandle> tabs() {
        return List.copyOf(tabs);
    }

    /**
     * Returns the active tab URL.
     *
     * @return active URL, or an empty string
     */
    public String activeUrl() {
        BrowserTabHandle active = activeTab();
        return active == null ? "" : active.url();
    }

    /**
     * Returns current window bounds.
     *
     * @return window geometry
     */
    public BrowserGeometry windowBounds() {
        ensureWindowBounds();
        Minecraft minecraft = Minecraft.getInstance();
        windowBounds = windowBounds.clampTo(
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                BrowserLayout.MIN_WINDOW_WIDTH,
                BrowserLayout.MIN_WINDOW_HEIGHT);
        return windowBounds;
    }

    /**
     * Sets window bounds, clamped to the current screen.
     *
     * @param bounds requested bounds
     */
    public void setWindowBounds(BrowserGeometry bounds) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        windowBounds = bounds.clampTo(screenWidth, screenHeight, 320, 220);
        resizeActiveTab();
    }

    /**
     * Returns whether the browser is maximized.
     *
     * @return fullscreen state
     */
    public boolean fullscreen() {
        return fullscreen;
    }

    /**
     * Toggles in-game fullscreen and restores previous bounds when exiting.
     */
    public void toggleFullscreen() {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        if (fullscreen) {
            BrowserGeometry restored = restoreBounds == null
                    ? BrowserGeometry.centered(
                            screenWidth,
                            screenHeight,
                            Math.max(480, screenWidth * 70 / 100),
                            Math.max(320, screenHeight * 70 / 100))
                    : restoreBounds;
            windowBounds = restored.clampTo(screenWidth, screenHeight, 320, 220);
            fullscreen = false;
            restoreBounds = null;
        } else {
            restoreBounds = windowBounds();
            windowBounds = BrowserGeometry.fullscreen(screenWidth, screenHeight, FULLSCREEN_MARGIN);
            fullscreen = true;
        }
        resizeActiveTab();
    }

    /**
     * Releases all browser resources when the player logs out.
     */
    public void onLogout() {
        closeWindow();
    }

    private boolean restoreSavedSession() {
        if (!isAvailable() || !tabs.isEmpty() || !savedSession.hasPages()) {
            return false;
        }
        BrowserEngine currentEngine = ensureEngine();
        if (currentEngine == null) {
            return false;
        }
        List<BrowserTabHandle> restored = new java.util.ArrayList<>();
        for (String url : savedSession.urls()) {
            if (restored.size() >= BrowserTabRules.MAX_TABS) {
                break;
            }
            try {
                restored.add(currentEngine.create(url));
            } catch (RuntimeException | LinkageError failure) {
                // Keep restoring the remaining pages; a failed tab must not lose the session.
            }
        }
        if (restored.isEmpty()) {
            return false;
        }
        tabs.addAll(restored);
        int savedIndex = savedSession.activeIndex();
        activeTabIndex = savedIndex >= 0 && savedIndex < restored.size() ? savedIndex : restored.size() - 1;
        windowVisible = true;
        ensureWindowBounds();
        resizeActiveTab();
        return true;
    }

    private BrowserEngine ensureEngine() {
        if (engine == null) {
            try {
                engine = new RinkuBrowserEngine();
            } catch (RuntimeException | LinkageError failure) {
                return null;
            }
        }
        return engine;
    }

    private void ensureWindowBounds() {
        if (windowBounds == null) {
            Minecraft minecraft = Minecraft.getInstance();
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();
            int width = Math.max(480, screenWidth * 70 / 100);
            int height = Math.max(320, screenHeight * 70 / 100);
            windowBounds = BrowserGeometry.centered(screenWidth, screenHeight, width, height)
                    .clampTo(screenWidth, screenHeight, 320, 220);
        }
    }
}
