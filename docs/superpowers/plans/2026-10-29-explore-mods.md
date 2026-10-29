# Explore Mods Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an Explore Mods tool that searches installed mods and opens their CurseForge or Modrinth page in an optional Rinku in-game browser.

**Architecture:** `common` gains pure page URL/platform selection rules and JSON result parsing. NeoForge gains the installed-mod catalog, client config, asynchronous page resolver, Explore Mods panel, and a Rinku-backed browser screen isolated behind a launcher so Creator Utils still loads without Rinku. Rinku is an optional client dependency and is never embedded.

**Tech Stack:** Java 21, Gradle 9, NeoForge 21.1.265, LDLib2 2.2.44, Rinku 3.0.4, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-29-explore-mods-design.md`

## Global Constraints

- Minecraft `1.21.1`; NeoForge `21.1.265`; Rinku `3.0.4-1.21.1`; Java 21.
- `common` must not import `net.neoforged.*`, `com.lowdragmc.*`, `de.keksuccino.*`, or `net.fabricmc.*`.
- Rinku is optional at runtime; no Rinku or Chromium classes may be embedded in the Creator Utils jar.
- Rinku-specific classes stay isolated in `ModBrowserLauncher` and `ModBrowserScreen`.
- Page preference with a configured key: CurseForge API, metadata CurseForge, metadata Modrinth, Modrinth API.
- Page preference without a configured key: metadata CurseForge, metadata Modrinth, Modrinth API.
- Exclude system mods `minecraft`, `neoforge`, and `fml` from the Explore Mods list.
- Search matches both display name and mod ID.
- No network I/O on the render thread. Network timeout: 8 seconds.
- CurseForge API key is client-only, blank by default, never logged, and never committed.
- Only exact normalized slug or case-insensitive title/slug matches may open a network result.
- Every public type gets Javadoc with `@author Izakji`.
- Use `./gradlew` for verification.
- Commit after every task, ending commit messages with `Co-Authored-By: Claude Code <noreply@anthropic.com>`.

## Review Focus

1. A fuzzy CurseForge or Modrinth result must not open the wrong mod. Exact match behavior is tested in Task 5.
2. Metadata URLs often include `/files`, `/versions`, query strings, or trailing slashes. Normalization must still produce the canonical project page; tested in Task 1.
3. Rinku's published jar is a wrapper with the real mod nested inside. Compile must use the extracted nested jar while development runtime keeps the wrapper; verified in Task 3.
4. Creator Utils must load when Rinku is absent. Tool-panel code must not resolve Rinku types unless the launcher is invoked; manual verification in Task 8.
5. An async lookup that finishes after the panel is gone must not unexpectedly open a browser screen. The panel checks the current screen before opening; manual verification in Task 8.

---

### Task 1: Common Explore Models and URL Rules

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/explore/ModPlatform.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/explore/InstalledMod.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/explore/ResolvedModPage.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/explore/ModPageUrls.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/explore/ModPageResolver.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/explore/InstalledModTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/explore/ModPageUrlsTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/explore/ModPageResolverTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `enum ModPlatform { CURSEFORGE, MODRINTH }`.
  - `record InstalledMod(String modId, String displayName, String version, List<String> metadataUrls)`
    with null normalization, an immutable URL list, and `String searchText()` returning
    `displayName + " " + modId`.
  - `record ResolvedModPage(ModPlatform platform, URI uri)`.
  - `ModPageUrls.parse(String url) -> Optional<ResolvedModPage>`.
  - `ModPageUrls.slugCandidates(InstalledMod mod) -> List<String>`.
  - `ModPageResolver.fromMetadata(InstalledMod mod) -> Optional<ResolvedModPage>`.

- [ ] **Step 1: Write the failing model and URL tests**

```java
class InstalledModTest {
    @Test
    void normalizesNullsAndSearchText() {
        var mod = new InstalledMod(null, null, null, null);
        assertEquals("", mod.modId());
        assertEquals("", mod.displayName());
        assertEquals("", mod.version());
        assertTrue(mod.metadataUrls().isEmpty());

        var named = new InstalledMod("example", "Example Mod", "1.0", List.of("https://modrinth.com/mod/example"));
        assertEquals("Example Mod example", named.searchText());
        assertThrows(UnsupportedOperationException.class,
                () -> named.metadataUrls().add("https://example.invalid"));
    }
}

class ModPageUrlsTest {
    @Test
    void normalizesCurseForgeProjectPaths() {
        assertEquals(
                new ResolvedModPage(ModPlatform.CURSEFORGE,
                        URI.create("https://www.curseforge.com/minecraft/mc-mods/jei")),
                ModPageUrls.parse("https://www.curseforge.com/minecraft/mc-mods/jei/files?gameVersion=1.21.1")
                        .orElseThrow());
    }

    @Test
    void normalizesModrinthProjectPaths() {
        assertEquals(
                new ResolvedModPage(ModPlatform.MODRINTH,
                        URI.create("https://modrinth.com/mod/async-locator-refined")),
                ModPageUrls.parse("https://modrinth.com/mod/async-locator-refined/versions")
                        .orElseThrow());
    }

    @Test
    void ignoresUnsupportedUrls() {
        assertTrue(ModPageUrls.parse("https://github.com/TheIllusiveC4/Curios").isEmpty());
        assertTrue(ModPageUrls.parse("not a url").isEmpty());
        assertTrue(ModPageUrls.parse(null).isEmpty());
    }

    @Test
    void createsDeterministicSlugCandidates() {
        var mod = new InstalledMod("a_man_with_plushies", "A Man With Plushies", "5.0.3", List.of());
        assertEquals(List.of("a-man-with-plushies"), ModPageUrls.slugCandidates(mod));
    }
}

class ModPageResolverTest {
    @Test
    void prefersCurseForgeMetadata() {
        var mod = new InstalledMod("example", "Example", "1.0", List.of(
                "https://modrinth.com/mod/example",
                "https://www.curseforge.com/minecraft/mc-mods/example"));
        assertEquals(ModPlatform.CURSEFORGE, ModPageResolver.fromMetadata(mod).orElseThrow().platform());
    }

    @Test
    void fallsBackToModrinthMetadata() {
        var mod = new InstalledMod("example", "Example", "1.0",
                List.of("https://github.com/owner/example", "https://modrinth.com/mod/example"));
        assertEquals(ModPlatform.MODRINTH, ModPageResolver.fromMetadata(mod).orElseThrow().platform());
    }

    @Test
    void returnsEmptyWhenNoPlatformMetadataExists() {
        var mod = new InstalledMod("example", "Example", "1.0",
                List.of("https://github.com/owner/example"));
        assertTrue(ModPageResolver.fromMetadata(mod).isEmpty());
    }
}
```

- [ ] **Step 2: Run tests to verify RED**

Run: `./gradlew :common:test --tests "com.izakji.mccreatorutils.explore.*"`

Expected: compilation failure because the explore types do not exist.

- [ ] **Step 3: Implement the common explore rules**

- `InstalledMod` normalizes nulls and uses `List.copyOf`.
- `ModPageUrls.parse` accepts only:
  - hosts `curseforge.com`, `www.curseforge.com`, path containing `minecraft/mc-mods/<slug>`;
  - hosts `modrinth.com`, `www.modrinth.com`, path starting `/mod/<slug>`.
- Canonicalize CurseForge to `https://www.curseforge.com/minecraft/mc-mods/<slug>` and Modrinth to
  `https://modrinth.com/mod/<slug>`; drop trailing segments, queries, and fragments.
- `slugCandidates` lowercases, removes apostrophes, replaces non-alphanumeric runs with `-`, trims
  hyphens, and returns the distinct mod-ID and display-name candidates in that order.
- `ModPageResolver.fromMetadata` scans in two passes: all CurseForge URLs first, then all Modrinth
  URLs.
- Add Javadoc with `@author Izakji` to every public type.

- [ ] **Step 4: Run tests to verify GREEN**

Run: `./gradlew :common:test --tests "com.izakji.mccreatorutils.explore.*"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/explore \
        common/src/test/java/com/izakji/mccreatorutils/explore
git commit -m "feat: add mod page selection rules" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 2: Tool Catalog Entry

**Files:**
- Modify: `common/src/main/java/com/izakji/mccreatorutils/tool/ToolId.java`
- Modify: `common/src/main/java/com/izakji/mccreatorutils/tool/ToolCatalog.java`
- Modify: `common/src/test/java/com/izakji/mccreatorutils/tool/ToolCatalogTest.java`
- Modify: `neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json`

**Interfaces:**
- Consumes: existing `ToolDescriptor`, `ToolCatalog`.
- Produces:
  - `ToolId.EXPLORE_MODS` with id `"explore_mods"`.
  - `ToolCatalog.EXPLORE_MODS` with translation key `tool.mccreatorutils.explore_mods` and required
    mod id `"rinku"`.
  - Tool list order: Camera, World Time, Biomes, Structures, Dimensions, Items, Mobs, Player,
    Explore Mods.

- [ ] **Step 1: Write the failing catalog test**

Update the expected order in `ToolCatalogTest.catalogPreservesDeclarationOrder` to include
`ToolId.EXPLORE_MODS` last, and add:

```java
@Test
void exploreModsDescriptorRequiresRinku() {
    var explore = ToolCatalog.EXPLORE_MODS;
    assertEquals(ToolId.EXPLORE_MODS, explore.id());
    assertEquals("explore_mods", explore.id().id());
    assertEquals("tool.mccreatorutils.explore_mods", explore.translationKey());
    assertEquals("rinku", explore.requiredModId());
    assertTrue(explore.hasRequiredMod());
}
```

- [ ] **Step 2: Run the test to verify RED**

Run: `./gradlew :common:test --tests "com.izakji.mccreatorutils.tool.ToolCatalogTest"`

Expected: compilation failure for missing `EXPLORE_MODS`.

- [ ] **Step 3: Implement the catalog entry and translation**

Add the enum constant, descriptor, and list entry. Add:

```json
"tool.mccreatorutils.explore_mods": "Explore Mods",
```

Keep the rest of the language file unchanged.

- [ ] **Step 4: Run the test to verify GREEN**

Run: `./gradlew :common:test --tests "com.izakji.mccreatorutils.tool.ToolCatalogTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/tool \
        common/src/test/java/com/izakji/mccreatorutils/tool/ToolCatalogTest.java \
        neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json
git commit -m "feat: add explore mods tool entry" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 3: Rinku Build Integration

**Files:**
- Modify: `gradle.properties`
- Modify: `neoforge/build.gradle`
- Modify: `neoforge/src/main/templates/META-INF/neoforge.mods.toml`

**Interfaces:**
- Consumes: Rinku Maven artifact `de.keksuccino:rinku-neoforge:3.0.4-1.21.1`.
- Produces:
  - `build/rinku/rinku-mod.jar` containing Rinku API classes for compile-time use.
  - Development runtime dependency on the Rinku wrapper.
  - Optional client dependency `rinku` in mod metadata.

- [ ] **Step 1: Add the Rinku repository, version property, and extraction task**

Add `rinku_version=3.0.4-1.21.1` to `gradle.properties`.

Add to `neoforge/build.gradle`:

```groovy
repositories {
    maven {
        name = 'Keksuccino'
        url = uri('https://keksuccino.github.io/maven/')
    }
}

configurations {
    rinkuDist {
        canBeConsumed = false
        canBeResolved = true
        transitive = false
    }
}

dependencies {
    implementation "de.keksuccino:rinku-neoforge:${rinku_version}"
    rinkuDist "de.keksuccino:rinku-neoforge:${rinku_version}"
}

def rinkuModJar = layout.buildDirectory.file('rinku/rinku-mod.jar')
def extractRinkuMod = tasks.register('extractRinkuMod', Copy) {
    inputs.files(configurations.rinkuDist)
    from({ zipTree(configurations.rinkuDist.singleFile) }) {
        include 'META-INF/jarjar/*-mod.jar'
        eachFile { details -> details.path = 'rinku-mod.jar' }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir('rinku'))
}

dependencies {
    compileOnly files(rinkuModJar)
}

tasks.named('compileJava') {
    dependsOn extractRinkuMod
}
```

Add the Maven repository to the existing `repositories` block rather than creating a duplicate block
if Gradle reports one.

- [ ] **Step 2: Run extraction and verify the nested jar**

Run: `./gradlew :neoforge:extractRinkuMod`

Expected: `neoforge/build/rinku/rinku-mod.jar` exists and contains
`de/keksuccino/rinku/Rinku.class`.

Run: `unzip -l neoforge/build/rinku/rinku-mod.jar | grep 'de/keksuccino/rinku/Rinku.class'`

Expected: one matching class entry.

- [ ] **Step 3: Declare Rinku as an optional client dependency**

Add to `neoforge/src/main/templates/META-INF/neoforge.mods.toml`:

```toml
[[dependencies.${mod_id}]]
    modId="rinku"
    type="optional"
    versionRange="[3.0.4,)"
    ordering="NONE"
    side="CLIENT"
```

- [ ] **Step 4: Verify compile after build integration**

Run: `./gradlew :neoforge:compileJava`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add gradle.properties neoforge/build.gradle neoforge/src/main/templates/META-INF/neoforge.mods.toml
git commit -m "build: add optional rinku dependency" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 4: Installed Mod Catalog and Client Config

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/explore/ExploreModsConfig.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/explore/InstalledModCatalog.java`
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/McCreatorUtilsNeoForge.java`

**Interfaces:**
- Consumes: `InstalledMod`, `ModList`, `IModInfo`, `IModFileInfo`.
- Produces:
  - `ExploreModsConfig.curseForgeApiKey() -> String`; trimmed, empty when config is not loaded.
  - `InstalledModCatalog.load() -> List<InstalledMod>`; immutable, sorted by display name.
  - Client config file containing `exploreMods.curseForgeApiKey` default `""`.

- [ ] **Step 1: Implement the client config**

Create a `ModConfigSpec` with `builder.push("exploreMods")` and
`define("curseForgeApiKey", "")`. The comment must say the key is optional, client-only, and never
logged. Register it in `McCreaTorUtilsNeoForge` with:

```java
modContainer.registerConfig(ModConfig.Type.CLIENT, ExploreModsConfig.SPEC);
```

The accessor returns `""` if `ConfigValue.get()` throws `IllegalStateException`.

- [ ] **Step 2: Implement the installed mod catalog**

Use `ModList.get().getMods()`. Skip IDs `minecraft`, `neoforge`, and `fml`.

For each remaining `IModInfo`:

- read `displayName`, `getModId()`, and `getVersion().toString()`;
- collect metadata URLs from:
  - `info.getConfig().<String>getConfigElement("displayURL")`;
  - `info.getOwningFile().getConfig().<String>getConfigElement("displayURL")`;
  - `info.getModURL().map(URL::toString)`;
- deduplicate URLs while preserving order;
- sort entries case-insensitively by display name;
- return `List.copyOf`.

- [ ] **Step 3: Verify compile**

Run: `./gradlew :neoforge:compileJava`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client/explore \
        neoforge/src/main/java/com/izakji/mccreatorutils/McCreatorUtilsNeoForge.java
git commit -m "feat: add installed mod catalog and config" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 5: JSON Result Parsing and Async Resolver

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/explore/ModApiJson.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/explore/ModApiJsonTest.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/explore/ModLinkResolver.java`

**Interfaces:**
- Consumes: `InstalledMod`, `ModPageUrls`, `ModPageResolver`, `ExploreModsConfig`.
- Produces:
  - `ModApiJson.curseForgeResult(String json, InstalledMod mod) -> Optional<ResolvedModPage>`.
  - `ModApiJson.modrinthProjectResult(String json) -> Optional<ResolvedModPage>`.
  - `ModApiJson.modrinthSearchResult(String json, InstalledMod mod) -> Optional<ResolvedModPage>`.
  - `ModLinkResolver.resolve(InstalledMod mod) -> CompletableFuture<Optional<ResolvedModPage>>`.
  - Successful resolutions cached by mod ID; failures are not cached.

- [ ] **Step 1: Write failing JSON parser tests**

```java
class ModApiJsonTest {
    private static final InstalledMod MOD =
            new InstalledMod("example", "Example Mod", "1.0", List.of());

    @Test
    void curseForgeRequiresExactSlugOrNameMatch() {
        String matching = """
                {"data":[{"name":"Example Mod","slug":"example-mod",
                "links":{"websiteUrl":"https://www.curseforge.com/minecraft/mc-mods/example-mod"}}]}
                """;
        String fuzzy = """
                {"data":[{"name":"Example Mod Extra","slug":"example-mod-extra",
                "links":{"websiteUrl":"https://www.curseforge.com/minecraft/mc-mods/example-mod-extra"}}]}
                """;
        assertEquals(ModPlatform.CURSEFORGE,
                ModApiJson.curseForgeResult(matching, MOD).orElseThrow().platform());
        assertTrue(ModApiJson.curseForgeResult(fuzzy, MOD).isEmpty());
    }

    @Test
    void modrinthProjectUsesSlug() {
        String json = """
                {"title":"Example Mod","slug":"example-mod"}
                """;
        assertEquals(URI.create("https://modrinth.com/mod/example-mod"),
                ModApiJson.modrinthProjectResult(json).orElseThrow().uri());
    }

    @Test
    void modrinthSearchRequiresExactTitleOrSlug() {
        String exact = """
                {"hits":[{"title":"Example Mod","slug":"example-mod"}]}
                """;
        String fuzzy = """
                {"hits":[{"title":"Example Mod Extra","slug":"example-mod-extra"}]}
                """;
        assertEquals(ModPlatform.MODRINTH,
                ModApiJson.modrinthSearchResult(exact, MOD).orElseThrow().platform());
        assertTrue(ModApiJson.modrinthSearchResult(fuzzy, MOD).isEmpty());
    }

    @Test
    void malformedJsonReturnsEmpty() {
        assertTrue(ModApiJson.curseForgeResult("not json", MOD).isEmpty());
        assertTrue(ModApiJson.modrinthProjectResult("").isEmpty());
        assertTrue(ModApiJson.modrinthSearchResult("{}", MOD).isEmpty());
    }
}
```

- [ ] **Step 2: Run tests to verify RED**

Run: `./gradlew :common:test --tests "com.izakji.mccreatorutils.explore.ModApiJsonTest"`

Expected: compilation failure because `ModApiJson` does not exist.

- [ ] **Step 3: Implement `ModApiJson`**

Use Gson `JsonParser`/`JsonObject`. Match CurseForge results only when `slug` is one of
`ModPageUrls.slugCandidates(mod)` or `name` equals `displayName` ignoring case. Match Modrinth
search hits only when `slug` is a candidate or `title` equals `displayName` ignoring case. Build
canonical URLs with `ModPageUrls.parse`; return empty for malformed JSON, missing fields, or
unsupported URLs.

- [ ] **Step 4: Run tests to verify GREEN**

Run: `./gradlew :common:test --tests "com.izakji.mccreatorutils.explore.ModApiJsonTest"`

Expected: PASS.

- [ ] **Step 5: Implement `ModLinkResolver`**

Add a static `HttpClient` with 5-second connect timeout and redirects; a 2-thread daemon executor;
and a `ConcurrentHashMap<String, CompletableFuture<Optional<ResolvedModPage>>>` success cache.

`resolve` executes off-thread and follows this order:

1. `ExploreModsConfig.curseForgeApiKey()`; if non-blank:
   - call
     `GET https://api.curseforge.com/v1/mods/search?gameId=432&classId=6&pageSize=20&searchFilter=<encoded displayName>`
     with header `x-api-key`;
   - if no exact result, query each slug candidate with
     `GET https://api.curseforge.com/v1/mods/search?gameId=432&classId=6&slug=<encoded slug>`;
   - a CF failure, rejected key, or rate limit falls through to the remaining steps.
2. `ModPageResolver.fromMetadata(mod)`.
3. Modrinth project lookup for each `ModPageUrls.slugCandidates(mod)`:
   `GET https://api.modrinth.com/v2/project/<encoded slug>`.
4. Modrinth search fallback:
   `GET https://api.modrinth.com/v2/search?query=<encoded displayName>&limit=10&facets=%5B%5B%22project_type:mod%22%5D%5D`.
5. `Optional.empty()`.

Every request uses `Accept: application/json` and User-Agent
`mccreatorutils/${mod_version} (Minecraft 1.21.1)`, a `Duration.ofSeconds(8)` request timeout, and
never logs the API key. Check HTTP status before parsing; 404 continues to the next lookup, and
network/API failures return empty so the caller can report the not-found state. Remove failed
futures from the cache; keep successes.

- [ ] **Step 6: Verify compile**

Run: `./gradlew :neoforge:compileJava`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/explore/ModApiJson.java \
        common/src/test/java/com/izakji/mccreatorutils/explore/ModApiJsonTest.java \
        neoforge/src/main/java/com/izakji/mccreatorutils/client/explore/ModLinkResolver.java
git commit -m "feat: resolve mod pages through metadata and APIs" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 6: Rinku Browser Screen

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/explore/ModBrowserLauncher.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/explore/ModBrowserScreen.java`
- Modify: `neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json`

**Interfaces:**
- Consumes: `ResolvedModPage`, Rinku API classes from `build/rinku/rinku-mod.jar`.
- Produces:
  - `ModBrowserLauncher.open(ResolvedModPage page)`; no-op when Rinku is absent.
  - `ModBrowserScreen` with Back, Forward, Reload, Close, loading, and browser input forwarding.

- [ ] **Step 1: Implement `ModBrowserLauncher`**

The launcher imports only Minecraft/NeoForge types. It checks
`ModList.get().isLoaded("rinku")`, then calls:

```java
Minecraft.getInstance().setScreen(new ModBrowserScreen(page.uri().toString()));
```

Do not reference `Rinku` or `ModBrowserScreen` in any field or signature that the Explore panel
loads. The browser screen class is loaded only by this method.

- [ ] **Step 2: Implement `ModBrowserScreen`**

Extend `Screen`. Fields:

- `String initialUrl`
- `RinkuBrowser browser`
- `Button backButton`, `forwardButton`, `reloadButton`
- `Component failure`

Behavior:

- `init`: add navigation buttons; if `browser == null`, check `Rinku.isInitialized()`, then
  `Rinku.createBrowser(initialUrl, false)` and `browser.useBrowserControls(true)`. Catch
  `RuntimeException` into `failure`.
- Layout: 24x20 buttons at `x=10, y=24`; browser below `y=50` with 10-pixel margins.
- `render`: draw a dark background, centered title, buttons, “Loading…” until
  `browser.isTextureReady()`, then `RinkuBrowserTextureBlitter.blit`.
- `tick`: update Back/Forward active states from the browser.
- `onClose`: close the browser, clear the field, then call `super.onClose()`.
- Copy the input forwarding contract from Rinku `ExampleScreen`: mouse press/release/move/wheel
  inside browser bounds; keyboard press/release/typed when the browser has focus; do not forward
  events consumed by navigation widgets.
- `resize`: `browser.resize((int) (browserWidth * guiScale), (int) (browserHeight * guiScale))`.
- If `failure` is set, render its message and keep Close usable; never throw.

Add language keys for `screen.mccreatorutils.mod_browser`, `...loading`, `...back`, `...forward`,
`...reload`, and `...close`.

- [ ] **Step 3: Verify compile**

Run: `./gradlew :neoforge:compileJava`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client/explore \
        neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json
git commit -m "feat: add rinku mod browser screen" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 7: Explore Mods Panel and Navigation

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/explore/ExploreModsToolPanel.java`
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/client/CreatorToolsUi.java`
- Modify: `neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json`

**Interfaces:**
- Consumes: `InstalledModCatalog`, `ModLinkResolver`, `ModBrowserLauncher`, `ComboSelector`,
  `CommandStatusLabel`.
- Produces:
  - `ExploreModsToolPanel` with searchable mod selector, `Open Page`, status, and Rinku notice.
  - `CreatorToolsUi` case for `ToolId.EXPLORE_MODS`.

- [ ] **Step 1: Implement the panel**

Fields:

- `CommandStatusLabel status`
- `ComboSelector<InstalledMod> modSelector`
- `Button openButton`
- `boolean rinkuAvailable = ModList.get().isLoaded("rinku")`
- `boolean removed`

Override `onRemoved()` to set `removed = true` before calling `super.onRemoved()`.

Constructor:

- Panel layout matches other tools: column, full width/height, 5-pixel gaps.
- Description label from `tool.mccreatorutils.explore_mods.description`.
- Add a yellow `notice.mccreatorutils.rinku_missing` label when Rinku is absent.
- Build `ComboSelector` from `InstalledModCatalog.load()`. Label function:
  `Component.literal(mod.displayName() + " (" + mod.modId() + ")")`.
- Add `Open Page` button. Start inactive.
- Add `status` with empty text.
- On selector change: clear status; activate the button only when a mod is selected and
  `rinkuAvailable`.
- On click:
  1. Disable the button; `status.showInfo(Component.translatable("status.mccreatorutils.looking_up"))`.
  2. Call `ModLinkResolver.resolve(mod)`.
  3. In `Minecraft.getInstance().execute`, re-enable the button.
  4. If `!removed`, `Minecraft.getInstance().screen instanceof CreatorToolsScreen`, and a page
     exists: set status to “Opening CurseForge…” or “Opening Modrinth…” and call
     `ModBrowserLauncher.open(page)`.
  5. If empty: `status.showError(Component.translatable("status.mccreatorutils.no_mod_page"))`.
  6. If exceptional: `status.showError(Component.translatable("status.mccreatorutils.lookup_failed"))`.

Add language keys:

```json
"tool.mccreatorutils.explore_mods.description": "Search installed mods and open their CurseForge or Modrinth page.",
"tool.mccreatorutils.explore_mods.open": "Open Page",
"notice.mccreatorutils.rinku_missing": "Rinku is required to open the in-game browser.",
"status.mccreatorutils.looking_up": "Looking up page...",
"status.mccreatorutils.no_mod_page": "No CurseForge or Modrinth page found.",
"status.mccreatorutils.lookup_failed": "Could not look up a mod page.",
"status.mccreatorutils.opening_curseforge": "Opening CurseForge...",
"status.mccreatorutils.opening_modrinth": "Opening Modrinth..."
```

- [ ] **Step 2: Wire the tool into navigation**

In `CreatorToolsUi.selectTool`, add a branch for `ToolId.EXPLORE_MODS` that adds
`new ExploreModsToolPanel()`. Leave the existing camera/world/session panel ordering unchanged.

- [ ] **Step 3: Verify compile and formatting**

Run: `./gradlew :neoforge:compileJava`

Expected: `BUILD SUCCESSFUL`.

Run: `./gradlew spotlessApply`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client/explore \
        neoforge/src/main/java/com/izakji/mccreatorutils/client/CreatorToolsUi.java \
        neoforge/src/main/java/com/izakji/mccreatorutils/client/CommandStatusLabel.java \
        neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json
git commit -m "feat: add explore mods tool" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 8: Final Verification

**Files:**
- No source changes expected.

**Interfaces:**
- Consumes: all previous tasks.
- Produces: verified build, test result, final jar inspection, and client checks.

- [ ] **Step 1: Run the full build and all tests**

Run: `./gradlew build`

Expected: `BUILD SUCCESSFUL`; all existing and new tests pass.

- [ ] **Step 2: Verify the final jar does not embed Rinku**

Run:

```bash
unzip -l neoforge/build/libs/mccreatorutils-1.0.0.jar | grep -E 'de/keksuccino|org/cef' || true
```

Expected: no matches.

- [ ] **Step 3: Manual client checks with Rinku installed**

Run: `./gradlew :neoforge:runClient`

Verify:

- Explore Mods appears last in the left navigation.
- The list excludes `minecraft`, `neoforge`, and `fml`.
- Search matches display name and mod ID.
- Biomes O' Plenty resolves to its metadata CurseForge URL.
- AsyncLocator-Refined resolves to its metadata Modrinth URL.
- A mod with no supported metadata reports “No CurseForge or Modrinth page found.”
- The browser opens, renders, navigates with Back/Forward/Reload, and closes cleanly.
- Switching tools while the panel is open does not crash.

- [ ] **Step 4: Manual client check without Rinku**

Temporarily remove the Rinku `implementation` dependency from the development runtime, rebuild, and
launch the client. Verify Creator Utils loads, Explore Mods shows the Rinku notice, and no
`NoClassDefFoundError` occurs. Restore the dependency afterward.

- [ ] **Step 5: Commit verification fixes only when needed**

If verification required source changes, run `git status --short`, stage the changed source files
explicitly, and commit with:

```bash
git commit -m "fix: finish explore mods integration" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

If no source changes are needed, create no empty commit.

## Plan Self-Review

- Spec coverage: tool/navigation, catalog, metadata, config, CurseForge API path, Modrinth fallback,
  async resolution, Rinku browser, optional dependency, tests, and manual checks each have a task.
- Type consistency: `InstalledMod`, `ResolvedModPage`, `ModPageUrls`, `ModPageResolver`,
  `ModApiJson`, and `ModLinkResolver` names are used consistently across tasks.
- Scope: no Fabric, no Rinku embedding, no address-bar editing, and no mod installation functionality.
