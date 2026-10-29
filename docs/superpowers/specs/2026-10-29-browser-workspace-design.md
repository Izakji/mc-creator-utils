# Browser Workspace Design

**Date:** 2026-10-29
**Status:** Draft for review
**Target:** Minecraft 1.21.1, NeoForge 21.1.265
**Related:** `2026-10-29-explore-mods-design.md`

## 1. Purpose

Extend Explore Mods with a persistent in-game browser workspace:

- remember the last selected mod while the client is running;
- show a floating browser window over normal gameplay;
- provide a configurable focus key that releases the cursor for browser input;
- support multiple tabs, address-bar navigation, dragging, resizing, and an in-game
  fullscreen/maximized mode;
- keep normal Minecraft input when the browser is not focused.

The existing optional Rinku dependency and page-resolution flow remain unchanged.

## 2. Scope

### In scope

- Session-only persistence of the Explore Mods selected mod ID.
- One client-side browser workspace shared by Explore Mods and later browser features.
- Floating HUD browser window visible during normal gameplay.
- Focus-key pointer mode with a transparent, non-pausing focus screen.
- Click inside browser to focus browser input; click outside or Escape to return to game.
- Multiple tabs, tab switching, tab closing, and new tabs.
- Tab cap of 8.
- Address bar, Back, Forward, Reload, and Close.
- Draggable title bar and bottom-right resize grip while focused.
- In-game fullscreen/maximized mode that restores the previous window bounds.
- Per-tab Rinku browser lifecycle and safe cleanup.
- Browser workspace hidden while any other Minecraft screen is open.
- Tests for session state, geometry, URL normalization, and tab selection rules.

### Out of scope

- Persisting selected mod, tabs, URLs, or window geometry across game restarts.
- OS-level fullscreen; “fullscreen” means an in-game maximized window.
- Browser history/bookmarks/download manager.
- Tab persistence across game launches.
- Multiple independent browser windows; one workspace with tabs.
- Direct game input while the browser is focused.
- Changing the CurseForge/Modrinth resolution rules.
- New Rinku versions or embedding Rinku/Chromium.

## 3. Decisions

- **PiP definition:** the floating browser window itself. It remains visible over normal gameplay.
  There is no separate video-only PiP surface.
- **Visibility:** visible only when `Minecraft.screen == null`; any other screen hides it.
- **Focus activation:** because Minecraft grabs the cursor during gameplay, a configurable
  **Focus Browser** key releases the cursor and opens the focus screen. The user then clicks the
  browser to send input. Clicking outside the window or pressing Escape returns control to the game.
- **Focus key default:** `B`, configurable through Minecraft controls.
- **Browser persistence:** tabs and window geometry live for the client session only. The selected
  mod also lives for the client session only, per user choice.
- **Tab model:** one Rinku browser per tab; maximum 8. Opening an already-open URL focuses its tab.
- **Fullscreen:** in-game maximized mode. Exiting fullscreen restores the previous floating bounds.
- **Chrome:** tabs + Back/Forward/Reload + address bar + fullscreen + close. Address-bar navigation is
  included; the prior Explore Mods browser omitted it.
- **Window interaction:** dragging and resizing happen only in focus mode, when the cursor is free.
- **HUD interaction:** the floating window is visual until the focus key is pressed; this avoids
  hidden cursor and gameplay-input conflicts.
- **Input ownership:** while the focus screen is open, game input is suspended as for any screen, but
  world ticks continue. Clicking outside or Escape restores normal game input.
- **Selected mod storage:** `CreatorToolSession`; no disk write.
- **No Rinku:** the Explore Mods notice remains; the browser workspace is unavailable. Existing
  Creator Utils behavior is unchanged.

## 4. Architecture

### 4.1 Common module

New pure classes under `com.izakji.mccreatorutils.browser`:

- `BrowserGeometry`
  - Record: `int x`, `int y`, `int width`, `int height`.
  - `BrowserGeometry clampTo(int screenWidth, int screenHeight, int minWidth, int minHeight)`.
  - `BrowserGeometry centered(int screenWidth, int screenHeight, int width, int height)`.
  - `BrowserGeometry fullscreen(int screenWidth, int screenHeight, int margin)`.
  - Immutable and safe for unit tests.
- `BrowserUrlInput`
  - `normalize(String raw) -> Optional<String>`.
  - Accepts `http`, `https`, and `about:blank`.
  - Prefixes `https://` when no scheme is present.
  - Rejects `javascript:`, `file:`, `data:`, and malformed input.
- `BrowserTabRules`
  - `MAX_TABS = 8`.
  - `findOpenTab(List<String> urls, String normalizedUrl) -> OptionalInt`.
  - `canOpenNewTab(int currentCount)`.
  - `nextActiveIndex(int currentIndex, int removedIndex, int oldSize)`.
- `BrowserLayout`
  - Computes tab strip, navigation bar, address bar, content bounds, and resize-grip bounds from a
    window geometry.
  - Constants: `TAB_HEIGHT=16`, `NAV_HEIGHT=20`, `ADDRESS_HEIGHT=16`, `MIN_WINDOW_WIDTH=320`,
    `MIN_WINDOW_HEIGHT=220`, `CONTENT_MARGIN=2`.
  - All values are GUI coordinates.

`ToolDescriptor`/session modifications:

- `CreatorToolSession.exploreModId()` returns `String` or null.
- `setExploreModId(String)` normalizes null/blank to null.
- Existing in-memory session behavior remains.

### 4.2 NeoForge module

New client classes under `com.izakji.mccreatorutils.client.browser`:

- `BrowserTabHandle` (Rinku-free interface)
  - Methods used by workspace, HUD, and focus screen:
    `String url()`, `void load(String url)`, `void resize(int width, int height)`,
    `void render(GuiGraphics graphics, int x, int y, int width, int height)`,
    `boolean canGoBack()`, `boolean canGoForward()`, `void goBack()`, `void goForward()`,
    `void reload()`, `boolean isLoading()`,
    `void sendMousePress(int x, int y, int button)`, `void sendMouseRelease(int x, int y, int button)`,
    `void sendMouseMove(int x, int y)`, `void sendMouseWheel(int x, int y, double amount, int modifiers)`,
    `void sendKeyPress(int keyCode, int scanCode, int modifiers)`,
    `void sendKeyRelease(int keyCode, int scanCode, int modifiers)`,
    `void sendKeyTyped(char codePoint, int modifiers)`, `void setFocus(boolean focused)`, `void close()`.
- `BrowserEngine` (Rinku-free factory interface)
  - `BrowserTabHandle createTab(String url) -> BrowserTabHandle`.
- `RinkuBrowserEngine` (isolated Rinku implementation)
  - The only browser-engine class that imports `de.keksuccino.rinku.*`.
  - Creates `RinkuBrowserTab` handles.
- `RinkuBrowserTab`
  - Rinku-specific tab handle: `RinkuBrowser` field, texture rendering via
    `RinkuBrowserTextureBlitter`, input forwarding, and `SafeClose` cleanup.
- `BrowserOpenResult`
  - Enum: `OPENED`, `FOCUSED_EXISTING`, `CAPACITY`, `UNAVAILABLE`, `FAILED`.
- `BrowserWorkspace`
  - Client singleton, Rinku-free.
  - Fields: `List<BrowserTabHandle> tabs`, `int activeTabIndex`, `BrowserGeometry windowBounds`,
    `BrowserGeometry restoreBounds`, `boolean fullscreen`, `boolean windowVisible`,
    `BrowserEngine engine`.
  - `isAvailable()`: `ModList.isLoaded("rinku")`; does not load Rinku classes.
  - `hasWindow()`.
  - `openPage(String url) -> BrowserOpenResult`: normalizes URL, reuses an existing matching tab,
    otherwise creates a tab if capacity allows, sets it active, makes the window visible, and opens
    focus mode. If the active screen is the Creator Utils screen, it is replaced by the focus screen.
  - `openNewTab() -> BrowserOpenResult`: opens `about:blank` and focuses it.
  - `focus()`: opens `BrowserFocusScreen` when a window exists and no other screen is open.
  - `closeTab(int index)`: closes the browser via the handle and updates active index/workspace
    window.
  - `closeWindow()`: closes all tabs safely, hides the HUD, clears state, and returns to game if the
    focus screen is open.
  - `setWindowBounds`, `toggleFullscreen`: clamp bounds and keep restore bounds.
  - `activeTab()`, `tabs()` returns immutable snapshots for rendering.
  - `onLogout()`: close all browser resources.
  - Lazily creates `RinkuBrowserEngine` only after `isAvailable()` is true.
- `BrowserWorkspaceRenderer`
  - Rinku-free renderer; uses `BrowserTabHandle.render` for content.
  - Draws window chrome, tabs, navigation, address bar text, loading state, and active browser
    texture.
  - Shared by HUD and focus screen.
  - No input handling.
- `BrowserHudOverlay`
  - `@EventBusSubscriber` client class listening to `RenderGuiEvent.Post`.
  - Renders the workspace only when:
    - Rinku is available;
    - `minecraft.screen == null`;
    - `minecraft.player != null`;
    - the workspace has tabs and is visible.
  - Does not receive clicks; focus key is used instead.
- `BrowserFocusScreen extends Screen`
  - Transparent background; `isPauseScreen()` returns false.
  - Renders the workspace and handles focus-mode input.
  - Registers vanilla `EditBox` for the address bar and vanilla `Button`s for navigation, tabs, and
    fullscreen/close.
  - On open, the browser content is unfocused until clicked. The screen itself owns the cursor release.
  - Click behavior:
    - inside content: focus browser and forward mouse press/release/move/wheel;
    - title bar: begin drag;
    - bottom-right grip: begin resize;
    - tab buttons: switch/close/new;
    - navigation widgets: Back/Forward/Reload;
    - address bar: edit and Enter loads the active tab;
    - fullscreen button: toggle maximize;
    - close button: close window;
    - outside window: close focus screen and return to game.
  - Escape:
    - if fullscreen: exit fullscreen and restore bounds;
    - otherwise close focus screen, leaving the floating window visible.
  - Keyboard input is forwarded to the active Rinku browser only when content is focused; address-bar
    edits and chrome shortcuts stay local.
  - Window dragging/resizing updates the workspace bounds and clamps to the current screen.
  - On close/removal, browser resources remain alive because the workspace is still visible; remove
    screen-only focus state.
- `BrowserKeyMappings`
  - Registers `FOCUS_BROWSER`, default `GLFW_KEY_B`, translation key
    `key.mccreatorutils.focus_browser`, category `key.categories.mccreatorutils`.
  - `ClientTickEvent.Post` consumes the key and calls `BrowserWorkspace.focus()` when no screen is
    open and a browser window exists.
- `ExploreModsToolPanel`
  - Restores `CreatorToolSession.exploreModId()` by mod ID from `InstalledModCatalog`.
  - Saves the selected mod ID to `CreatorToolSession` on selection.
  - `Open Page` resolves as before, then calls `BrowserWorkspace.openPage(uri.toString())` instead of
    `ModBrowserLauncher.open`.
  - If a page is already open, the workspace focuses its tab rather than opening a duplicate.
  - Maps `BrowserOpenResult` to status text, including capacity, unavailable, and failure.
- `ModBrowserLauncher`
  - Removed; `BrowserWorkspace` is the single browser entry point.
- `McCreatorUtilsClient`
  - Registers the new key mapping.
  - Listens for logout/stopping to close browser resources.
- `McCreaTorUtilsNeoForge`
  - No new config required for this feature. `ExploreModsConfig` continues to hold the optional
    CurseForge API key.

### 4.3 Rinku lifecycle

- Tabs are created lazily when the workspace first needs them.
- `Rinku.isInitialized()` false: workspace shows a clear unavailable/initializing state; no browser
  classes are created.
- Each tab has its own Rinku browser; hidden tabs remain alive until closed.
- Closing a tab/window closes its browser with `SafeClose`, making cleanup idempotent and
  exception-safe.
- Closing the game/logging out closes all workspace browsers before Rinku shutdown.
- Rinku remains optional and is not embedded.

## 5. User Interface

### 5.1 Floating window

- Default size: 70% of the window, clamped to at least 480 × 320 GUI pixels.
- Center the window on first open.
- Border, subtle shadow, title/tab strip, navigation row, address row, content area, resize grip.
- Window stays at its last bounds across focus enter/exit and tool reopen.
- Hidden while any non-focus screen is open.

### 5.2 Tab strip

- Active tab highlighted; inactive tabs clickable.
- `+` button adds a blank tab; disabled at 8 tabs.
- Each tab has an `X` close button.
- Tab label: host from URL, `New Tab` for `about:blank`, truncated to fit.
- Closing the active tab chooses the nearest remaining tab.
- Closing the last tab closes the window.

### 5.3 Navigation row

- Back, Forward, Reload.
- Fullscreen/maximize button.
- Close window button.
- Address bar spans the remaining width, shows the active tab URL, and accepts Enter.

### 5.4 Focus behavior

- Focus key is displayed in the Explore Mods panel description/tooltip and in Minecraft controls.
- With a browser window visible, pressing the focus key releases the mouse and shows the focus screen.
- First click in browser content starts browser keyboard/mouse input.
- Clicking outside the window closes focus mode; the HUD browser remains visible.
- Clicking a browser tab/navigation control does not steal content focus except where appropriate.
- Clicking the address bar focuses it; Enter loads; Escape leaves the address bar or exits fullscreen.
- Dragging/resizing only works in focus mode; the HUD window is not silently intercepting gameplay.

### 5.5 Fullscreen

- Fullscreen fills the in-game screen with a small outer margin.
- Tabs/navigation/address bar remain visible.
- Drag/resize disabled.
- Toggling off restores the pre-fullscreen geometry.
- Exiting focus mode while fullscreen restores the floating bounds before returning to gameplay.

## 6. Data Flow

1. Explore Mods panel restores the last selected mod from `CreatorToolSession`.
2. User selects a mod; the panel writes the mod ID to the session.
3. Open Page resolves the URL and calls `BrowserWorkspace.openPage`.
4. Workspace reuses a matching tab or creates one, makes the window visible, and opens focus mode.
5. Focus screen renders/controls the workspace; Rinku browser receives input only after content focus.
6. Closing focus returns to gameplay with the floating window still visible.
7. Focus key reopens the focus screen.
8. Closing the window closes every tab/browser and hides the HUD.

## 7. Error Handling

- Rinku absent: Explore panel notice; workspace unavailable; no classloading.
- Rinku initialization failed: focus/HUD shows an error; window remains closable.
- Tab creation failure: show error, keep other tabs usable.
- Tab count at 8: `+` disabled; opening a new URL replaces? No — report capacity failure and keep
  existing tabs; user must close a tab. (Decision: prefer predictable behavior over silent eviction.)
- Invalid address-bar input: keep current page and show a status error.
- Closing a browser throws: `SafeClose` swallows; tab is removed; workspace remains usable.
- Screen removed without normal close: focus screen cleans screen-only state; workspace browsers stay.
- Player logout: workspace closes all browsers.
- Window bounds off-screen after resize: clamp to current screen bounds.

## 8. Testing

### Automated common tests

- `BrowserGeometryTest`: centering, clamping, minimum size, fullscreen margin.
- `BrowserUrlInputTest`: http/https/about:blank, missing scheme, rejected schemes, malformed input.
- `BrowserTabRulesTest`: duplicate URL detection, max tabs, next active index.
- `CreatorToolSessionTest`: explore mod ID round-trip and null normalization.
- Existing `SafeCloseTest` covers safe browser cleanup.

### Manual client checks

- Explore Mods restores the selected mod after closing/reopening the GUI and after switching tools.
- Selected mod resets after restarting the client.
- Open Page opens the floating window and focused browser.
- Focus key releases cursor; content click focuses browser.
- Click outside/Escape returns game input while the window remains visible.
- Drag/resize work in focus mode and remain clamped.
- Tabs add/switch/close; duplicate URL focuses existing tab; 8-tab cap.
- Back/Forward/Reload/address bar work.
- Fullscreen fills and restores previous bounds.
- Other screens hide the floating window; closing them restores it.
- Window close closes all tabs and removes HUD.
- Rinku absent still loads Creator Utils and shows the notice.

## 9. Limitations

- Selected mod, tabs, URLs, and geometry survive only the client process, not restart.
- One browser workspace only.
- HUD window is not interactive until the focus key is pressed.
- Fullscreen is in-game, not OS-level.
- Rinku/Chromium resource usage grows with tab count; cap is 8.
- Pages that block embedded browsers or require external auth may misbehave.
