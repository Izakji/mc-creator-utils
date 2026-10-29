# Browser Workspace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add session-only Explore Mods selection persistence and a Rinku-backed floating tabbed browser with focus mode, resize/drag, address navigation, fullscreen, and gameplay HUD visibility.

**Architecture:** Common code owns pure geometry, URL, tab, and layout rules. NeoForge owns a Rinku-free `BrowserWorkspace`, renderer, HUD overlay, and transparent focus screen; one Rinku adapter family creates the per-tab browsers. Explore Mods resolves a page, opens it in the workspace, and stores only the selected mod ID in `CreatorToolSession`.

**Tech Stack:** Java 21, Gradle 9, NeoForge 21.1.265, LDLib2 2.2.44, Rinku 3.0.4, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-29-browser-workspace-design.md`

## Global Constraints

- Minecraft `1.21.1`; NeoForge `21.1.265`; Rinku `3.0.4-1.21.1`; Java 21.
- Selected mod is session-only in `CreatorToolSession`; no disk persistence.
- Floating browser visible only when `Minecraft.screen == null`; any other screen hides it.
- Focus key default `B`, configurable through Minecraft controls.
- Focus screen is transparent and non-pausing; game world continues.
- Clicking outside the window or Escape returns focus to Minecraft; the floating browser remains.
- Tabs: maximum 8; one Rinku browser per tab; duplicate normalized URL focuses its tab.
- Window default: 70% of screen, minimum 480×320, clamped to screen; layout minimum 320×220.
- Fullscreen is in-game maximized mode; exiting restores the previous floating bounds.
- Address bar accepts only `http`, `https`, and `about:blank`; missing scheme gets `https://`.
- Rinku is optional. `de.keksuccino.*` imports are allowed only in the isolated Rinku engine/tab classes.
- Rinku and Chromium are never embedded in the Creator Utils jar.
- Browser cleanup uses `SafeClose`; logout/window close disposes every tab.
- No new Rinku version, config file, or page-resolution behavior.
- Every public type gets Javadoc with `@author Izakji`.
- Use `./gradlew` for verification.
- Commit after every task, ending commit messages with `Co-Authored-By: Claude Code <noreply@anthropic.com>`.

## Review Focus

1. Normal gameplay grabs the cursor. The focus key must release it before a browser click can be interpreted; clicking outside must return control without closing the floating window. Manual checks in Task 7.
2. Window drag/resize/fullscreen must clamp to the current GUI screen and restore the pre-fullscreen bounds. Tests in Task 1; manual checks in Task 7.
3. Tab close/active-tab selection must never index past the remaining tab list; closing the last tab must close the window and dispose all browsers. Tests in Task 1; manual checks in Task 7.
4. HUD visibility must not overlap inventory, chat, pause, or other screens; focus screen is the only exception and must render the workspace itself. Manual checks in Task 7.
5. Rinku absence must not load any Rinku class. Browser UI must remain on the Rinku-free interface path; jar inspection and no-Rinku client launch in Task 7.
6. Address-bar input must reject `javascript:`, `file:`, `data:`, and malformed URLs rather than passing them to Chromium. Tests in Task 1.

---

### Task 1: Common Browser Rules and Session Selection

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/browser/BrowserGeometry.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/browser/BrowserUrlInput.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/browser/BrowserTabRules.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/browser/BrowserLayout.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/browser/BrowserGeometryTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/browser/BrowserUrlInputTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/browser/BrowserTabRulesTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/browser/BrowserLayoutTest.java`
- Modify: `common/src/main/java/com/izakji/mccreatorutils/session/CreatorToolSession.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/session/CreatorToolSessionTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `record BrowserGeometry(int x, int y, int width, int height)` with
    `centered(int screenWidth, int screenHeight, int width, int height)`,
    `clampTo(int screenWidth, int screenHeight, int minWidth, int minHeight)`,
    `fullscreen(int screenWidth, int screenHeight, int margin)`, `right()`, and `bottom()`.
  - `BrowserUrlInput.normalize(String raw) -> Optional<String>`.
  - `BrowserTabRules.MAX_TABS = 8`, `canOpenNewTab(int count)`,
    `findOpenTab(List<String> urls, String normalizedUrl)`, and
    `nextActiveIndex(int currentIndex, int removedIndex, int oldSize)`.
  - `record BrowserLayout(BrowserGeometry window, BrowserGeometry titleBar,
    BrowserGeometry tabStrip, BrowserGeometry navBar, BrowserGeometry addressBar,
    BrowserGeometry content, BrowserGeometry resizeGrip)` with `of(BrowserGeometry)` and constants
    `TITLE_HEIGHT=16`, `TAB_HEIGHT=16`, `NAV_HEIGHT=20`, `ADDRESS_HEIGHT=16`,
    `MIN_WINDOW_WIDTH=320`, `MIN_WINDOW_HEIGHT=220`, `CONTENT_MARGIN=2`, `RESIZE_GRIP=12`.
  - `CreatorToolSession.exploreModId()` / `setExploreModId(String)`; null/blank normalizes to null.

- [ ] **Step 1: Write failing geometry and layout tests**

```java
class BrowserGeometryTest {
    @Test
    void centersWithinTheScreen() {
        assertEquals(new BrowserGeometry(300, 150, 400, 300),
                BrowserGeometry.centered(1000, 600, 400, 300));
    }

    @Test
    void clampsWindowToScreenAndMinimum() {
        assertEquals(new BrowserGeometry(0, 0, 320, 220),
                new BrowserGeometry(-50, -20, 100, 100).clampTo(1000, 600, 320, 220));
        assertEquals(new BrowserGeometry(680, 380, 320, 220),
                new BrowserGeometry(900, 500, 320, 220).clampTo(1000, 600, 320, 220));
    }

    @Test
    void fullscreenUsesMargin() {
        assertEquals(new BrowserGeometry(10, 10, 980, 580),
                BrowserGeometry.fullscreen(1000, 600, 10));
    }
}

class BrowserLayoutTest {
    @Test
    void stacksChromeAndContent() {
        var layout = BrowserLayout.of(new BrowserGeometry(100, 100, 400, 300));
        assertEquals(new BrowserGeometry(100, 100, 400, 16), layout.titleBar());
        assertEquals(new BrowserGeometry(100, 116, 400, 16), layout.tabStrip());
        assertEquals(new BrowserGeometry(100, 132, 400, 20), layout.navBar());
        assertEquals(new BrowserGeometry(100, 152, 400, 16), layout.addressBar());
        assertEquals(new BrowserGeometry(102, 170, 396, 228), layout.content());
        assertEquals(new BrowserGeometry(488, 388, 12, 12), layout.resizeGrip());
    }
}
```

- [ ] **Step 2: Write failing URL, tab, and session tests**

```java
class BrowserUrlInputTest {
    @Test
    void addsHttpsWhenMissing() {
        assertEquals("https://example.com/path",
                BrowserUrlInput.normalize("example.com/path").orElseThrow());
    }

    @Test
    void acceptsSupportedSchemes() {
        assertEquals("http://example.com",
                BrowserUrlInput.normalize("http://example.com").orElseThrow());
        assertEquals("about:blank", BrowserUrlInput.normalize("about:blank").orElseThrow());
    }

    @Test
    void rejectsUnsafeOrMalformedInput() {
        for (String raw : List.of("javascript:alert(1)", "file:///etc/passwd",
                "data:text/html,hi", "http://", "", "   ")) {
            assertTrue(BrowserUrlInput.normalize(raw).isEmpty(), raw);
        }
    }
}

class BrowserTabRulesTest {
    @Test
    void limitsTabsAndFindsExistingUrl() {
        assertTrue(BrowserTabRules.canOpenNewTab(7));
        assertFalse(BrowserTabRules.canOpenNewTab(8));
        assertEquals(OptionalInt.of(1),
                BrowserTabRules.findOpenTab(List.of("about:blank", "https://a.example"), "https://a.example"));
    }

    @Test
    void choosesNearestActiveIndexAfterRemoval() {
        assertEquals(0, BrowserTabRules.nextActiveIndex(0, 0, 3));
        assertEquals(1, BrowserTabRules.nextActiveIndex(2, 0, 3));
        assertEquals(1, BrowserTabRules.nextActiveIndex(2, 2, 3));
        assertEquals(1, BrowserTabRules.nextActiveIndex(1, 2, 3));
    }
}
```

Add to `CreatorToolSessionTest`:

```java
@Test
void storesExploreModIdForTheSession() {
    var session = new CreatorToolSession();
    assertNull(session.exploreModId());
    session.setExploreModId("example");
    assertEquals("example", session.exploreModId());
    session.setExploreModId(" ");
    assertNull(session.exploreModId());
}
```

- [ ] **Step 3: Run tests to verify RED**

Run: `./gradlew :common:test --tests "com.izakji.mccreatorutils.browser.*" --tests "com.izakji.mccreatorutils.session.CreatorToolSessionTest"`

Expected: compilation failure for missing browser classes and `exploreModId`.

- [ ] **Step 4: Implement the common rules**

- `BrowserGeometry` clamps width/height to the screen when the screen is smaller than the minimum,
  then clamps x/y into the remaining bounds.
- `BrowserLayout.of` uses the exact stacking above; content is `width - 2*margin` by
  `height - (TITLE_HEIGHT + TAB_HEIGHT + NAV_HEIGHT + ADDRESS_HEIGHT + 2*margin)`; resize grip is
  `12×12` at the bottom-right.
- `BrowserUrlInput` trims, accepts `about:blank`, applies `https://` when no scheme exists, and
  validates host presence; reject all other schemes.
- `BrowserTabRules.nextActiveIndex` returns 0 for an empty result and otherwise clamps to
  `oldSize - 2`.
- Add the `exploreModId` field to `CreatorToolSession`.
- Add Javadoc with `@author Izakji`.

- [ ] **Step 5: Run tests to verify GREEN**

Run: `./gradlew :common:test --tests "com.izakji.mccreatorutils.browser.*" --tests "com.izakji.mccreatorutils.session.CreatorToolSessionTest"`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/browser \
        common/src/test/java/com/izakji/mccreatorutils/browser \
        common/src/main/java/com/izakji/mccreatorutils/session/CreatorToolSession.java \
        common/src/test/java/com/izakji/mccreatorutils/session/CreatorToolSessionTest.java
git commit -m "feat: add browser workspace rules and session selection" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 2: Browser Contracts and Rinku Adapter

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/BrowserTabHandle.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/BrowserEngine.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/BrowserOpenResult.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/RinkuBrowserTab.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/RinkuBrowserEngine.java`

**Interfaces:**
- Consumes: Rinku classes from the extracted `build/rinku/rinku-mod.jar`, `SafeClose`.
- Produces:
  - `interface BrowserTabHandle` with:
    `String url()`, `void load(String url)`, `void resize(int width, int height)`,
    `void render(GuiGraphics graphics, int x, int y, int width, int height)`,
    `boolean isTextureReady()`, `boolean isLoading()`, `boolean canGoBack()`, `boolean canGoForward()`,
    `void goBack()`, `void goForward()`, `void reload()`, `void setFocus(boolean focused)`,
    `void sendMousePress(int x, int y, int button)`, `void sendMouseRelease(int x, int y, int button)`,
    `void sendMouseMove(int x, int y)`, `void sendMouseWheel(int x, int y, double amount, int modifiers)`,
    `void sendKeyPress(int keyCode, int scanCode, int modifiers)`,
    `void sendKeyRelease(int keyCode, int scanCode, int modifiers)`,
    `void sendKeyTyped(char codePoint, int modifiers)`, `void close()`.
  - `interface BrowserEngine { BrowserTabHandle create(String initialUrl); }`.
  - `enum BrowserOpenResult { OPENED, FOCUSED_EXISTING, CAPACITY, UNAVAILABLE, FAILED }`.
  - `RinkuBrowserTab implements BrowserTabHandle`.
  - `RinkuBrowserEngine implements BrowserEngine`.

- [ ] **Step 1: Implement the Rinku-free interfaces and result enum**

Define the exact methods above. `BrowserTabHandle` and `BrowserEngine` must not import Rinku or any
client browser implementation. Add Javadoc with `@author Izakji`.

- [ ] **Step 2: Implement `RinkuBrowserTab`**

Constructor accepts `String initialUrl`; calls `Rinku.createBrowser(initialUrl, false)`, enables
browser controls, and stores the handle. `render` uses `RinkuBrowserTextureBlitter.blit` when
`isTextureReady`, otherwise does nothing (renderer draws loading). Forward all input methods to the
`RinkuBrowser`. `url` returns `browser.getURL()` or the initial URL. `close` uses `SafeClose` and is
idempotent. `load` calls `browser.loadURL`. `resize` clamps to at least 1×1. `setFocus` forwards to
`browser.setFocus`.

- [ ] **Step 3: Implement `RinkuBrowserEngine`**

`create` throws `IllegalStateException` when `Rinku.isInitialized()` is false; otherwise returns a
new `RinkuBrowserTab`. This is the only engine class allowed to import `de.keksuccino.rinku.*`.

- [ ] **Step 4: Verify compile**

Run: `./gradlew :neoforge:compileJava`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client/browser \
git commit -m "feat: add browser contracts and rinku adapter" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 3: Rinku-Free Workspace Core

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/BrowserWorkspace.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/BrowserFocusScreen.java`

**Interfaces:**
- Consumes: `BrowserGeometry`, `BrowserUrlInput`, `BrowserTabRules`, `BrowserTabHandle`,
  `BrowserEngine`, `BrowserOpenResult`.
- Produces:
  - `BrowserWorkspace.instance()`, `isAvailable()`, `hasWindow()`.
  - `openPage(String url) -> BrowserOpenResult`.
  - `openNewTab() -> BrowserOpenResult`.
  - `focus()`.
  - `closeTab(int index)`.
  - `closeWindow()`.
  - `activeTab()`, `activeTabIndex()`, `tabs()`, `activeUrl()`.
  - `windowBounds()`, `setWindowBounds(BrowserGeometry)`, `fullscreen()`, `toggleFullscreen()`.
  - `onLogout()`.
  - A minimal `BrowserFocusScreen` shell with `isPauseScreen() == false`, so Task 5 can fill the
    input/render behavior without changing workspace contracts.

- [ ] **Step 1: Implement workspace state**

Workspace is a client singleton and must not import Rinku. Initialize window geometry on first page:

```java
int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
int width = Math.max(480, screenWidth * 70 / 100);
int height = Math.max(320, screenHeight * 70 / 100);
windowBounds = BrowserGeometry.centered(screenWidth, screenHeight, width, height)
        .clampTo(screenWidth, screenHeight, 320, 220);
```

`ensureEngine` checks `isAvailable()` before constructing `RinkuBrowserEngine`; catch
`RuntimeException | LinkageError` and return `null`. `openPage` normalizes with
`BrowserUrlInput.normalize`, returns `FAILED` when invalid, returns `UNAVAILABLE` when Rinku is
absent, returns `CAPACITY` at 8 tabs, focuses an existing normalized URL, otherwise creates a tab,
sets it active, makes the window visible, and calls `focus()`.

`focus()` opens `BrowserFocusScreen` when a window exists and the current screen is null or
`CreatorToolsScreen`. It must not replace another screen. `closeTab` closes the handle with
`SafeClose`, updates the active index via `BrowserTabRules`, and closes the window when no tabs
remain. `closeWindow` disposes every tab, clears state, and returns to game if the focus screen is
current. `toggleFullscreen` stores `restoreBounds` before filling the screen and restores it on exit.

- [ ] **Step 2: Add the focus screen shell**

`BrowserFocusScreen extends Screen`, has a no-arg constructor with the browser title, returns false
from `isPauseScreen`, and leaves render/input empty until Task 5. It must not import Rinku.

- [ ] **Step 3: Verify compile**

Run: `./gradlew :neoforge:compileJava`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/BrowserWorkspace.java \
        neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/BrowserFocusScreen.java
git commit -m "feat: add browser workspace state" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 4: Window Renderer and HUD Overlay

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/BrowserWorkspaceRenderer.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/BrowserHudOverlay.java`
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/BrowserFocusScreen.java`
- Modify: `neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json`

**Interfaces:**
- Consumes: `BrowserWorkspace`, `BrowserTabHandle`, `BrowserLayout`, `BrowserGeometry`.
- Produces:
  - `BrowserWorkspaceRenderer.render(GuiGraphics graphics, BrowserWorkspace workspace, boolean showAddressText)`.
  - `BrowserHudOverlay` renders the floating window over normal gameplay.
  - A focus-screen `render` hook that draws the workspace but not HUD-only behavior.

- [ ] **Step 1: Implement the workspace renderer**

Use `BrowserLayout.of(workspace.windowBounds())`. Draw:
- window background/border/shadow;
- title bar with active tab domain and fullscreen/close indicators;
- tab strip with active tab highlight and truncated labels;
- navigation row with Back/Forward/Reload labels;
- address row; draw `workspace.activeUrl()` only when `showAddressText` is true;
- content background and `activeTab().render(graphics, content.x(), content.y(), content.width(), content.height())`;
- loading text when `activeTab().isLoading()` or texture not ready;
- resize grip marks at the bottom-right.

Use `Minecraft.getInstance().font` and simple `GuiGraphics.fill/drawString` calls. Do not import
Rinku.

- [ ] **Step 2: Implement the HUD overlay**

Add a client event subscriber for `RenderGuiEvent.Post`:

```java
@SubscribeEvent
static void onRenderGui(RenderGuiEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.screen != null || minecraft.player == null) return;
    BrowserWorkspace workspace = BrowserWorkspace.instance();
    if (workspace.hasWindow()) {
        BrowserWorkspaceRenderer.render(event.getGuiGraphics(), workspace, true);
    }
}
```

No mouse handling in the HUD.

- [ ] **Step 3: Add basic focus-screen rendering**

`BrowserFocusScreen.render` calls `BrowserWorkspaceRenderer.render(graphics, workspace, false)`.
Task 5 adds the address bar, controls, and input.

- [ ] **Step 4: Add language keys**

Add keys for browser title, focus key, tab limit, fullscreen, close, loading, Back, Forward, Reload,
New Tab, address placeholder, invalid URL, Rinku unavailable, and browser-open failure.

- [ ] **Step 5: Verify compile and formatting**

Run: `./gradlew :neoforge:compileJava`

Expected: `BUILD SUCCESSFUL`.

Run: `./gradlew :neoforge:spotlessApply`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client/browser \
        neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json
git commit -m "feat: render browser workspace hud" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 5: Focus Screen Window Controls and Input

**Files:**
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/BrowserFocusScreen.java`
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/client/McCreatorUtilsClient.java`
- Modify: `neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json`

**Interfaces:**
- Consumes: `BrowserWorkspace`, `BrowserWorkspaceRenderer`, `BrowserLayout`, `BrowserUrlInput`,
  `BrowserTabRules`, `BrowserGeometry`.
- Produces:
  - Full focus-screen input, tab/nav/address/fullscreen/close interaction.
  - `FOCUS_BROWSER` key mapping, default `B`.
  - Logout cleanup hook.

- [ ] **Step 1: Add focus key and logout wiring**

In `McCreatorUtilsClient`, add:

```java
public static final KeyMapping FOCUS_BROWSER = new KeyMapping(
        "key.mccreatorutils.focus_browser",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_B,
        "key.categories.mccreatorutils");
```

Register it with `RegisterKeyMappingsEvent`. In `ClientTickEvent.Post`, after the existing GUI key:
if `minecraft.screen == null` and `BrowserWorkspace.instance().hasWindow()`, consume the key and call
`BrowserWorkspace.instance().focus()`.

Subscribe to `ClientPlayerNetworkEvent.LoggingOut` and call
`BrowserWorkspace.instance().onLogout()`.

- [ ] **Step 2: Implement focus-screen layout and widgets**

In `BrowserFocusScreen.init`:
- get workspace bounds/layout;
- add a vanilla `EditBox` at `layout.addressBar()` with the active URL; max length 2048;
- set `isPauseScreen()` false; do not add vanilla buttons for tabs/nav, use hit testing in
  `mouseClicked`;
- update the address field from the active tab URL when it is not focused.

`render`: draw the workspace renderer with `showAddressText=false`, then `super.render` so the
EditBox appears above the address bar.

- [ ] **Step 3: Implement mouse/window interactions**

- Click outside the window: `onClose()` (return to game; workspace remains).
- Click title bar: begin drag with `BrowserLayout.titleBar`; update workspace bounds in
  `mouseDragged`, clamp with `BrowserGeometry.clampTo`.
- Click resize grip: begin resize; update width/height with minimum 320×220 and clamp.
- Click tab: set active index; click tab X: `workspace.closeTab(index)`.
- Click `+`: `workspace.openNewTab()`; focus the address bar.
- Click Back/Forward/Reload: call active handle methods; update navigation state.
- Click fullscreen: `workspace.toggleFullscreen()`; rebuild layout.
- Click close: `workspace.closeWindow()`.
- Click content: clear widget focus, `activeTab().setFocus(true)`, send mouse press; set
  `contentFocused=true`.
- Mouse release/move/wheel: forward to active handle only when content is focused/in bounds.
- Address bar Enter: `BrowserUrlInput.normalize`; invalid sets a screen-local error text; valid loads
  active tab and focuses content.

- [ ] **Step 4: Implement keyboard behavior**

- Escape: if fullscreen, exit fullscreen and restore bounds; otherwise close focus screen.
- Address bar focused: forward key/char events to `super` first, Enter loads URL.
- Content focused: forward key press/release/typed to active handle.
- Clicking chrome/tab/nav clears content focus; clicking content restores it.
- When focus screen closes/removes, clear content focus and leave workspace/tabs alive.

- [ ] **Step 5: Verify compile and formatting**

Run: `./gradlew :neoforge:compileJava`

Expected: `BUILD SUCCESSFUL`.

Run: `./gradlew :neoforge:spotlessApply`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client/browser/BrowserFocusScreen.java \
        neoforge/src/main/java/com/izakji/mccreatorutils/client/McCreatorUtilsClient.java \
        neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json
git commit -m "feat: add browser focus controls" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 6: Explore Mods Integration and Old Browser Removal

**Files:**
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/client/explore/ExploreModsToolPanel.java`
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/client/explore/ModBrowserLauncher.java`
- Delete: `neoforge/src/main/java/com/izakji/mccreatorutils/client/explore/ModBrowserScreen.java`
- Delete: `neoforge/src/main/java/com/izakji/mccreatorutils/client/explore/ModBrowserLauncher.java`
- Modify: `neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json`

**Interfaces:**
- Consumes: `BrowserWorkspace`, `BrowserOpenResult`, `CreatorToolSession`, `InstalledModCatalog`.
- Produces:
  - Explore Mods restores/saves `exploreModId`.
  - Open Page opens the workspace and maps all `BrowserOpenResult` values.
  - Old full-screen browser classes removed.

- [ ] **Step 1: Integrate selected mod persistence**

After constructing the mod selector, read `CreatorToolSession.exploreModId()`. Find the matching
`InstalledMod` by mod ID; if present, `modSelector.setSelected(match, false)` and update the Open Page
button. On selector change, call `session.setExploreModId(selected == null ? null : selected.modId())`.

- [ ] **Step 2: Route Open Page through the workspace**

When the resolver returns a page, call:

```java
BrowserOpenResult result = BrowserWorkspace.instance().openPage(page.uri().toString());
```

Map results:
- `OPENED` / `FOCUSED_EXISTING`: workspace opens/focuses the browser; no panel status needed.
- `CAPACITY`: “Browser tab limit reached.”
- `UNAVAILABLE`: Rinku notice/“Rinku required.”
- `FAILED`: “Could not open browser page.”

Keep the existing guarded `CreatorToolsScreen` check and `removed` flag.

- [ ] **Step 3: Remove the old browser path**

Delete `ModBrowserLauncher` and `ModBrowserScreen`; ensure no imports or references remain. The
workspace is the sole browser entry point.

- [ ] **Step 4: Verify compile and formatting**

Run: `./gradlew :neoforge:compileJava`

Expected: `BUILD SUCCESSFUL`.

Run: `./gradlew spotlessApply`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client/explore \
        neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json
git commit -m "feat: integrate explore mods browser workspace" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 7: Final Verification

**Files:**
- No source changes expected.

**Interfaces:**
- Consumes: all previous tasks.
- Produces: verified build, tests, jar, and manual browser checks.

- [ ] **Step 1: Run the full build and tests**

Run: `./gradlew build`

Expected: `BUILD SUCCESSFUL`; all common tests pass.

- [ ] **Step 2: Verify Rinku is not embedded**

Run:

```bash
unzip -l neoforge/build/libs/mccreatorutils-1.0.0.jar | grep -E 'de/keksuccino|org/cef' || true
```

Expected: no matches.

- [ ] **Step 3: Manual no-Rinku check**

Temporarily remove the Rinku `implementation` dependency, rebuild, and launch. Verify Creator Utils
loads, Explore Mods shows the Rinku notice, no `NoClassDefFoundError` occurs, and the browser key
does nothing. Restore the dependency and rebuild.

- [ ] **Step 4: Manual browser checks with Rinku**

Run: `./gradlew :neoforge:runClient`

Verify:
- selected mod survives closing/reopening Explore Mods and switching tools;
- selected mod resets after restarting the client;
- Open Page opens the floating window;
- focus key releases the cursor; click content focuses browser input;
- click outside/Escape returns game input while the window stays visible;
- dragging/resizing works and clamps;
- tabs add/switch/close; duplicate URL focuses existing tab; 8-tab cap;
- Back/Forward/Reload/address bar work;
- fullscreen maximizes and restores bounds;
- other screens hide the HUD;
- closing the window disposes all tabs;
- logout disposes all tabs.

If the Rinku JCEF runtime cannot be downloaded in this environment, leave this task unverified and
record that limitation explicitly; do not claim full browser verification.

- [ ] **Step 5: Commit verification fixes only when needed**

If verification required source changes, stage the changed source files explicitly and commit:

```bash
git commit -m "fix: finish browser workspace integration" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

If no source changes are needed, create no empty commit.

## Plan Self-Review

- Spec coverage: session selection, HUD window, focus key, tabs, address bar, drag/resize, fullscreen,
  visibility rules, Rinku isolation, cleanup, and tests each map to a task.
- Type consistency: `BrowserGeometry`, `BrowserLayout`, `BrowserTabHandle`, `BrowserEngine`,
  `BrowserOpenResult`, `BrowserWorkspace`, and `BrowserFocusScreen` are used consistently.
- Scope: one workspace, no tab/window disk persistence, no OS fullscreen, no new page-resolution
  rules.
