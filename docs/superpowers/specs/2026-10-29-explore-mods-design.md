# Explore Mods Design

> **Superseded browser architecture (2026-10-30):** the full-screen
> `ModBrowserLauncher`/`ModBrowserScreen` viewer described below was replaced by the Rinku-free
> `BrowserWorkspace`. See `2026-10-29-browser-workspace-design.md`. Page-resolution rules and the
> optional Rinku dependency remain current.

**Date:** 2026-10-29
**Status:** Draft for review
**Target:** Minecraft 1.21.1, NeoForge 21.1.265
**Branch:** current working branch

## 1. Purpose

Add an **Explore Mods** tool that lets a creator search installed mods and open the selected mod's
CurseForge or Modrinth page in an in-game Chromium browser.

The browser is provided by the optional Rinku library. Creator Utils must still load and function
normally when Rinku is not installed.

## 2. Scope

### In scope

- New `Explore Mods` navigation button and client tool panel.
- Searchable list of all loaded mods, excluding loader-internal entries.
- Mod metadata collection: mod ID, display name, version, and author-provided URLs.
- CurseForge-first page resolution when a CurseForge API key is configured.
- Metadata-first page resolution when no key is configured.
- Modrinth public API fallback.
- Asynchronous resolution with timeouts and a small success cache.
- Optional Rinku integration and a custom in-game browser screen.
- Client config entry for a CurseForge API key.
- Unit tests for deterministic URL selection and normalization.
- Build support for Rinku's wrapper/nested-mod distribution.

### Out of scope

- Embedding Rinku or its Chromium runtime in Creator Utils.
- A CurseForge API key supplied by Creator Utils.
- Bulk preloading of every mod's page or API lookup when the panel opens.
- Downloading or installing mods.
- Opening arbitrary metadata homepages when no CurseForge or Modrinth page exists.
- Address-bar editing in the browser; navigation links within pages remain supported.
- Fabric implementation.
- Persisting the selected mod between game launches.

## 3. Decisions

### 3.1 Platform selection

The preferred order is:

1. CurseForge API lookup, only when `exploreMods.curseForgeApiKey` is non-blank.
2. A CurseForge URL supplied by mod metadata.
3. A Modrinth URL supplied by mod metadata.
4. Modrinth lookup through its public API.
5. Failure: report that no CurseForge or Modrinth page was found.

Without a CurseForge API key, a mod whose author metadata points to Modrinth opens Modrinth even if
the mod also exists on CurseForge. This is the accepted limitation until a key is supplied.

The CurseForge API key is read from client config, never logged, and never committed. When the key is
filled in, the CurseForge lookup becomes active without code changes.

### 3.2 Rinku dependency

Rinku is **optional**:

- Declared as an optional client dependency in `neoforge.mods.toml`.
- Present on the development compile and runtime classpath.
- Not embedded in the Creator Utils jar.
- If absent at runtime, Explore Mods shows a “Rinku required” notice and disables browsing.
- Rinku-specific classes are isolated in `ModBrowserLauncher` and `ModBrowserScreen`; the tool panel
  never touches Rinku types when Rinku is absent.

The published Rinku artifact is `de.keksuccino:rinku-neoforge:3.0.4-1.21.1`. The local Rinku 3.0.5
source tree is used as API reference only. If 3.0.5 is published later, only the version property
needs updating.

### 3.3 Metadata sources

For each `IModInfo`, collect URLs from:

- the mod-level `displayURL` config element;
- the owning file's `displayURL` config element;
- `IModInfo.getModURL()` / the `modUrl` config element.

Only URLs whose host is `curseforge.com` or `modrinth.com` are treated as platform pages. Other
homepages are ignored. URLs are normalized to the canonical mod page path.

### 3.4 Browser shape

The in-game browser is a full Minecraft screen with:

- a title;
- Back, Forward, Reload, and Close buttons;
- a loading indicator;
- the Rinku browser texture below the navigation row.

Browser input forwarding follows Rinku's `ExampleScreen` contract. The browser is closed and
deregistered when the screen closes.

## 4. Architecture

### 4.1 Common module

New loader-neutral classes under `com.izakji.mccreatorutils.explore`:

- `ModPlatform`
  - Enum: `CURSEFORGE`, `MODRINTH`.
- `InstalledMod`
  - Record: `String modId`, `String displayName`, `String version`, `List<String> metadataUrls`.
  - Normalizes null values to empty strings/lists.
- `ResolvedModPage`
  - Record: `ModPlatform platform`, `URI uri`.
- `ModPageUrls`
  - Detects a supported platform from a URL.
  - Normalizes `curseforge.com/minecraft/mc-mods/<slug>` and `modrinth.com/mod/<slug>` URLs.
  - Ignores unsupported hosts and malformed URLs.
  - Produces slug candidates for lookup: normalized mod ID and normalized display name.
- `ModPageResolver`
  - Selects a metadata page synchronously:
    - CurseForge metadata URL wins over Modrinth metadata URL.
  - Exposes a network-result decision helper so the NeoForge resolver can combine API results with
    metadata without duplicating preference rules.
- `SearchFilter` remains the shared mod-name filter.

`ToolId` and `ToolCatalog` additions:

- `ToolId.EXPLORE_MODS("explore_mods")`.
- `ToolCatalog.EXPLORE_MODS` with translation key `tool.mccreatorutils.explore_mods` and required mod
  ID `rinku`.
- Appended after `PLAYER` in the navigation order.

### 4.2 NeoForge module

New client classes under `com.izakji.mccreatorutils.client.explore`:

- `InstalledModCatalog`
  - Reads `ModList.get().getMods()`.
  - Excludes system IDs: `minecraft`, `neoforge`, `fml`.
  - Builds `InstalledMod` entries with metadata URLs.
  - Sorts case-insensitively by display name.
  - Returns an immutable list.
- `ModLinkResolver`
  - Async network resolver using `java.net.http.HttpClient`.
  - Inputs: `InstalledMod`, optional CurseForge API key.
  - CurseForge lookup:
    - `GET https://api.curseforge.com/v1/mods/search`
    - `gameId=432`, `classId=6`, search by display name and by slug candidates.
    - Header `x-api-key`.
    - Accept only an exact normalized slug/title match; do not open the first fuzzy result.
    - Read `data[].slug` or `data[].links.websiteUrl`.
  - Modrinth lookup:
    - `GET https://api.modrinth.com/v2/project/<slug>` for each slug candidate.
    - Fallback to `GET https://api.modrinth.com/v2/search`.
    - Accept only an exact slug or case-insensitive title match.
    - Open `https://modrinth.com/mod/<slug>`.
  - Requests use an explicit User-Agent, connect/request timeouts, and no credentials in logs.
  - Successful results are cached by mod ID for the client session. Failures are not cached.
  - Never performs network I/O on the render thread.
- `ExploreModsConfig`
  - NeoForge `ModConfig.Type.CLIENT` spec.
  - `exploreMods.curseForgeApiKey`: string, default empty.
  - Accessor returns a trimmed value; not-loaded config returns empty.
- `ExploreModsToolPanel`
  - `ComboSelector<InstalledMod>` populated from `InstalledModCatalog`.
  - Left field filters by display name and mod ID; right dropdown selects a mod.
  - `Open Page` button, status label, and Rinku availability notice.
  - On click:
    1. Disable the button and show “Looking up page…”.
    2. Resolve asynchronously.
    3. On success, call `ModBrowserLauncher.open(page)` on the client thread.
    4. On failure, show a red status and re-enable the button.
  - If Rinku is absent, show an informational notice and keep `Open Page` disabled.
- `ModBrowserLauncher`
  - Checks `ModList.get().isLoaded("rinku")`.
  - Sets the Minecraft screen to `ModBrowserScreen`.
  - Is the only tool-panel entry point that references Rinku classes.
- `ModBrowserScreen`
  - `Screen` implementation backed by `RinkuBrowser`.
  - Initializes the browser with the resolved URL using `Rinku.createBrowser(url, false)`.
  - Resizes the browser to the available GUI area multiplied by GUI scale.
  - Renders with `RinkuBrowserTextureBlitter`.
  - Forwards mouse press/release/move/wheel and keyboard events to the browser.
  - Updates Back/Forward enabled state each tick.
  - Shows “Loading…” until the browser texture is ready.
  - Closes the browser in `onClose`.
  - Handles initialization failure without crashing the game; displays the error and offers Close.
- `CreatorToolsUi`
  - Adds a panel case for `ToolId.EXPLORE_MODS`.

### 4.3 Build integration

Add:

- repository `https://keksuccino.github.io/maven/`;
- property `rinku_version=3.0.4-1.21.1`;
- dependency on the Rinku wrapper for development runtime;
- a Gradle extraction task that copies the nested
  `META-INF/jarjar/*-mod.jar` from the wrapper to a stable build path;
- `compileOnly files(<extracted jar>)` for Rinku API classes;
- compile task dependency on extraction;
- optional `rinku` dependency in `neoforge.mods.toml` with `side="CLIENT"`.

No Rinku classes or Chromium assets are embedded in the Creator Utils jar.

## 5. User Interface

### 5.1 Navigation

Left pane order becomes:

1. Camera
2. World Time
3. Biomes
4. Structures
5. Dimensions
6. Items
7. Mobs
8. Player
9. Explore Mods

### 5.2 Explore Mods panel

- Description: “Search installed mods and open their CurseForge or Modrinth page.”
- Rinku availability notice when applicable.
- Mod selector: same `ComboSelector` layout as registry entry selectors; search field matches display
  name and mod ID.
- `Open Page` button.
- Status label:
  - empty by default;
  - “Looking up page…” while resolving;
  - “Opening CurseForge…” / “Opening Modrinth…”;
  - red “No CurseForge or Modrinth page found.”;
  - red network/API failure message.
- Panel fits the existing 520 x 300 Creator Utils window.

### 5.3 Browser screen

- Dark background and centered title.
- Navigation bar: Back, Forward, Reload, Close.
- Browser area fills the remaining screen with a small margin.
- Loading indicator while the page is starting.
- Escape closes the browser through the standard screen close path.

## 6. Configuration

Client config file, generated by NeoForge:

```toml
[exploreMods]
    # Optional CurseForge API key used to prefer CurseForge pages.
    # Leave blank to use mod metadata and Modrinth's public API only.
    curseForgeApiKey = ""
```

Rules:

- The key is read at resolve time, so editing the config does not require restarting if NeoForge
  reloads the client config.
- The key is never included in logs, status text, or exception messages.
- Empty key disables all CurseForge API calls; metadata CurseForge URLs still work.

## 7. Data Flow

1. Panel construction reads loaded mods and builds the selector list locally.
2. User types to filter and selects a mod.
3. `Open Page` starts async resolution:
   - optional CurseForge API lookup;
   - metadata CurseForge URL;
   - metadata Modrinth URL;
   - Modrinth API lookup.
4. Result is cached on success and returned to the client thread.
5. `ModBrowserLauncher` opens `ModBrowserScreen` with the resolved URI.
6. Rinku creates/loads the browser; the screen renders the texture and forwards input.
7. Closing the screen closes the Rinku browser.

## 8. Error Handling

- Rinku absent: tool still opens, notice shown, Open Page disabled.
- Rinku present but browser initialization fails: browser screen shows an error; game remains usable.
- No mod selected: Open Page disabled.
- No supported page found: red status; no browser opened.
- Network timeout or HTTP error: red status; fallback chain continues; failures are not cached.
- CurseForge API key rejected/rate-limited: fall back to metadata and Modrinth; key value never shown.
- Modrinth API non-200 response: treat lookup as not found and continue to failure.
- Page URL malformed or unsupported host: ignored during metadata normalization.
- GUI closed while resolution is pending: resolution may finish, but browser opening is checked
  against the current screen/state before replacing it.
- Browser page navigation failure: Rinku displays the Chromium error page; screen remains usable.

## 9. Testing

### Automated

Common tests:

- `ModPageUrlsTest`:
  - recognizes CurseForge and Modrinth URLs;
  - normalizes trailing slashes/versions paths to the main project page;
  - ignores GitHub and unrelated homepages;
  - produces expected slug candidates from display names and mod IDs.
- `ModPageResolverTest`:
  - CurseForge metadata beats Modrinth metadata;
  - Modrinth metadata is returned when no CurseForge URL exists;
  - empty metadata returns no page;
  - unsupported URLs are ignored.
- Existing `SearchFilterTest` covers search behavior.

Build verification:

- Rinku nested-jar extraction task runs before compile.
- Full `./gradlew build` passes with 103 existing common tests plus the new tests.
- Final jar contains no Rinku/Chromium classes.

### Manual client checks

- List contains installed mods, no `minecraft`/`neoforge`/`fml`.
- Search filters by display name and mod ID.
- Biomes O' Plenty opens its metadata CurseForge page without a key.
- AsyncLocator-Refined opens its metadata Modrinth page.
- A mod with no supported URL reports the not-found status.
- Back/Forward/Reload work; Close returns to the game.
- Remove/disable Rinku and verify the tool shows the Rinku notice instead of crashing.
- Add a test CurseForge API key and verify the API path is attempted without exposing the key.

## 10. Limitations

- CurseForge-first behavior is exact only when a valid API key is configured.
- Without a key, a dual-platform mod whose metadata points to Modrinth opens Modrinth.
- Mods with no CurseForge/Modrinth URL and no Modrinth project cannot be opened.
- Rinku's Chromium runtime is downloaded and managed by Rinku, not Creator Utils.
- Pages that block embedded browsers or require external authentication may not render correctly.
