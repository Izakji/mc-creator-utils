# Creator Utils Foundation and Camera Tool Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the starter project into a `common` + `neoforge` multi-project build, then deliver the LDLib2 two-pane Creator Utils GUI and the CMDCam mode/duration tool.

**Architecture:** `common` holds loader-neutral tool metadata, camera enums, duration validation, and command construction. `neoforge` holds the `@Mod` entrypoint, keybind, LDLib2 screen, tool panels, CMDCam detection, and client command dispatch. A future Fabric module can reuse `common` unchanged.

**Tech Stack:** Java 21, Gradle 9, ModDevGradle 2.0.148, NeoForge 21.1.265, Parchment 2024.11.17, LDLib2 2.2.44, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-27-creator-utils-foundation-design.md`

## Global Constraints

- Minecraft `1.21.1`; NeoForge `21.1.265`; NeoForm `1.21.1-20240808.144430`; Parchment `2024.11.17`.
- Java toolchain 21; UTF-8 compilation; Unix line endings after Spotless.
- `mod_id=mccreatorutils`; `mod_group_id=com.izakji.mccreatorutils`; `mod_version=1.0.0`; display name `Creator Utils`.
- LDLib2 dependency: `com.lowdragmc.ldlib2:ldlib2-neoforge-1.21.1:2.2.44:all` from `https://maven.firstdark.dev/snapshots`.
- LDLib2 is required on the client side; CMDCam is optional on the client side.
- `common` must not import `net.neoforged.*`, `com.lowdragmc.*`, or `net.fabricmc.*`.
- Keybind default is `.` using `GLFW.GLFW_KEY_PERIOD`.
- Duration input accepts ASCII digits only, at most three characters, numeric range `1` through `999`.
- GUI target size is `360 x 240` GUI-scaled pixels, clamped with a `20`-pixel margin to the current screen.
- Use LDLib2 built-in sprites and widget defaults; no custom textures.
- Every public type gets a Javadoc block with `@author Izakji`, matching the reference project.
- Commit messages end with `Co-Authored-By: Claude Code <noreply@anthropic.com>`.

## Review Focus

1. A `TextField` can hold invalid raw text while its last valid value stays cached. The duration Run button must derive validity from `getRawText()` on every tick, not from the last notified value.
2. Unit suffixes are easy to confuse: `ms`, `m`, and `s` must produce exactly `500ms`, `2m`, and `20s`. Exact command strings are tested.
3. Arbitrary values could reach a command string. Mode values come only from an enum, and duration values pass `DurationInput` and `CameraCommands` range checks; invalid `0` and `1000` throw.
4. CMDCam may be absent, or the player may disconnect while the screen is open. No command may be sent; controls disable or the screen closes.
5. A `Selector` starts with a null value. Mode Run must stay disabled until a non-null mode is selected, and `CameraCommands.mode(null)` must throw rather than build `cam mode null`.

---

### Task 1: Multi-project build, cleanup, and dependency metadata

**Files:**
- Modify: `settings.gradle`
- Modify: `build.gradle`
- Modify: `gradle.properties`
- Create: `common/build.gradle`
- Create: `neoforge/build.gradle`
- Move: `src/main/templates/META-INF/neoforge.mods.toml` -> `neoforge/src/main/templates/META-INF/neoforge.mods.toml`
- Move: `src/main/resources/assets/mccreatorutils/lang/en_us.json` -> `neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json`
- Delete: `src/main/java/com/izakji/mccreatorutils/Config.java`
- Delete: `src/main/java/com/izakji/mccreatorutils/mccreatorutils.java`
- Delete: `src/main/java/com/izakji/mccreatorutils/mccreatorutilsClient.java`
- Delete after moves: empty `src/` tree

**Interfaces:**
- Consumes: existing Gradle wrapper and starter files.
- Produces: Gradle projects `:common` and `:neoforge`; `commonJava` and `commonResources` configurations; root `build`, `runClient`, `runServer`, `runData`, `spotlessApply`, and `spotlessCheck` aliases.

- [ ] **Step 1: Initialize Git and commit the starter baseline**

Run:

```bash
if [ ! -d .git ]; then git init; fi
git add .
git commit -m "chore: initialize project baseline" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

Expected: commit created; `git status` clean.

- [ ] **Step 2: Replace `settings.gradle`**

Set the root name and include both modules. Use the plugin repositories and foojay resolver shown below.

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = 'NeoForged'
            url = uri('https://maven.neoforged.net/releases')
        }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}

rootProject.name = 'mc-creator-utils'
include 'common'
include 'neoforge'
```

- [ ] **Step 3: Replace the root `build.gradle`**

Use the reference structure from `C:/Users/izakj/Documents/minecraft modding/stuff-ds/build.gradle`. Required content:

```groovy
plugins {
    id 'com.diffplug.spotless' version '8.10.4' apply false
    id 'net.neoforged.moddev' version '2.0.148' apply false
}

allprojects {
    group = mod_group_id
    version = mod_version
}

subprojects {
    apply plugin: 'java-library'
    apply plugin: 'com.diffplug.spotless'

    repositories {
        mavenCentral()
    }

    java.toolchain.languageVersion = JavaLanguageVersion.of(21)

    spotless {
        java {
            target 'src/main/java/**/*.java', 'src/test/java/**/*.java'
            lineEndings 'UNIX'
            forbidWildcardImports()
            removeUnusedImports()
            palantirJavaFormat('2.102.0')
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    tasks.withType(JavaCompile).configureEach {
        options.encoding = 'UTF-8'
    }
}

tasks.named('wrapper', Wrapper).configure {
    distributionType = Wrapper.DistributionType.BIN
}

tasks.register('build') { dependsOn ':common:build', ':neoforge:build' }
tasks.register('spotlessApply') { dependsOn ':common:spotlessApply', ':neoforge:spotlessApply' }
tasks.register('spotlessCheck') { dependsOn ':common:spotlessCheck', ':neoforge:spotlessCheck' }
tasks.register('runClient') { dependsOn ':neoforge:runClient' }
tasks.register('runServer') { dependsOn ':neoforge:runServer' }
tasks.register('runData') { dependsOn ':neoforge:runData' }
```

- [ ] **Step 4: Add `common/build.gradle`**

```groovy
plugins {
    id 'java-library'
    id 'net.neoforged.moddev'
}

base {
    archivesName = "${mod_id}-common"
}

neoForge {
    neoFormVersion = project.neo_form_version

    parchment {
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }
}

configurations {
    commonJava {
        canBeResolved = false
        canBeConsumed = true
    }
    commonResources {
        canBeResolved = false
        canBeConsumed = true
    }
}

artifacts {
    commonJava sourceSets.main.java.sourceDirectories.singleFile
    commonResources sourceSets.main.resources.sourceDirectories.singleFile
}

dependencies {
    testImplementation platform('org.junit:junit-bom:5.11.4')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
}
```

- [ ] **Step 5: Add `neoforge/build.gradle`**

```groovy
plugins {
    id 'java-library'
    id 'maven-publish'
    id 'idea'
    id 'net.neoforged.moddev'
}

base {
    archivesName = mod_id
}

neoForge {
    version = project.neo_version

    parchment {
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }

    runs {
        client {
            client()
            ideName.set('Client')
            ideFolderName.set('NeoForge')
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }

        server {
            server()
            ideName.set('Server')
            ideFolderName.set('NeoForge')
            programArgument '--nogui'
            systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        }

        data {
            data()
            ideName.set('Data')
            ideFolderName.set('NeoForge')
            programArguments.addAll '--mod', project.mod_id, '--all',
                    '--output', file('src/generated/resources/').absolutePath,
                    '--existing', file('src/main/resources/').absolutePath,
                    '--existing', rootProject.file('common/src/main/resources/').absolutePath
        }

        configureEach {
            systemProperty 'forge.logging.markers', 'REGISTRIES'
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        "${mod_id}" {
            sourceSet(sourceSets.main)
        }
    }
}

repositories {
    maven {
        name = 'FirstDark Snapshots'
        url = uri('https://maven.firstdark.dev/snapshots')
    }
}

configurations {
    commonJava { canBeResolved = true }
    commonResources { canBeResolved = true }
}

dependencies {
    implementation "com.lowdragmc.ldlib2:ldlib2-neoforge-${minecraft_version}:${ldlib2_version}:all"

    commonJava project(path: ':common', configuration: 'commonJava')
    commonResources project(path: ':common', configuration: 'commonResources')
}

tasks.named('compileJava', JavaCompile) {
    dependsOn(configurations.commonJava)
    source(configurations.commonJava)
}

processResources {
    dependsOn(configurations.commonResources)
    from(configurations.commonResources)
}

var generateModMetadata = tasks.register("generateModMetadata", ProcessResources) {
    var replaceProperties = [
            minecraft_version      : minecraft_version,
            minecraft_version_range: minecraft_version_range,
            neo_version            : neo_version,
            loader_version_range   : loader_version_range,
            mod_id                 : mod_id,
            mod_name               : mod_name,
            mod_license            : mod_license,
            mod_version            : mod_version,
            description            : mod_description,
            ldlib2_version         : ldlib2_version,
    ]
    inputs.properties replaceProperties
    expand replaceProperties
    from 'src/main/templates'
    into 'build/generated/sources/modMetadata'
}
sourceSets.main.resources.srcDir generateModMetadata
neoForge.ideSyncTask generateModMetadata

publishing {
    publications {
        register('mavenJava', MavenPublication) {
            from components.java
        }
    }
    repositories {
        maven {
            url "file://${rootProject.projectDir}/repo"
        }
    }
}

idea {
    module {
        downloadSources = true
        downloadJavadoc = true
    }
}
```

- [ ] **Step 6: Update `gradle.properties`**

Change or add:

```properties
neo_form_version=1.21.1-20240808.144430
ldlib2_version=2.2.44
mod_name=Creator Utils
mod_description=Tools and utilities for Minecraft creators.
```

Keep existing `mod_id`, `mod_group_id`, `mod_version`, `neo_version`, Parchment, and Minecraft values.

- [ ] **Step 7: Move metadata and language files, delete starter Java**

Move the template and language file to the `neoforge` module. Delete the three starter Java files and the old root `src` directory. Update `neoforge/src/main/templates/META-INF/neoforge.mods.toml`:

- Replace the description with `${description}`.
- Keep `authors="Izakji"`.
- Add the LDLib2 dependency:

```toml
[[dependencies.${mod_id}]]
    modId="ldlib2"
    type="required"
    versionRange="[${ldlib2_version},)"
    ordering="NONE"
    side="CLIENT"
```

- Add the CMDCam dependency:

```toml
[[dependencies.${mod_id}]]
    modId="cmdcam"
    type="optional"
    versionRange="[2.0.0,)"
    ordering="NONE"
    side="CLIENT"
```

- [ ] **Step 8: Run the build**

Run:

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL`; `common/build/libs/mccreatorutils-common-1.0.0.jar` and `neoforge/build/libs/mccreatorutils-1.0.0.jar` exist. The NeoForge jar must contain `META-INF/neoforge.mods.toml` and `assets/mccreatorutils/lang/en_us.json`.

- [ ] **Step 9: Commit**

```bash
git add settings.gradle build.gradle gradle.properties common neoforge
git rm -r src
git commit -m "chore: split project into common and neoforge modules" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 2: Shared constants, tool identifiers, and catalog

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/McCreatorUtils.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/tool/ToolId.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/tool/ToolDescriptor.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/tool/ToolCatalog.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/tool/ToolCatalogTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks except the `common` test configuration.
- Produces:
  - `McCreatorUtils.MOD_ID` and `McCreatorUtils.LOGGER`.
  - `ToolId.CAMERA` with `String id()`.
  - `ToolDescriptor(ToolId id, String translationKey, String requiredModId)` record with `boolean hasRequiredMod()`.
  - `ToolCatalog.CAMERA` and `List<ToolDescriptor> tools()`.

- [ ] **Step 1: Write the failing catalog test**

Create `ToolCatalogTest`:

```java
package com.izakji.mccreatorutils.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ToolCatalogTest {

    @Test
    void catalogContainsCameraExactlyOnce() {
        assertEquals(1, ToolCatalog.tools().stream()
                .filter(tool -> tool.id() == ToolId.CAMERA)
                .count());
    }

    @Test
    void cameraDescriptorHasExpectedMetadata() {
        var camera = ToolCatalog.CAMERA;
        assertEquals(ToolId.CAMERA, camera.id());
        assertEquals("camera", camera.id().id());
        assertEquals("tool.mccreatorutils.camera", camera.translationKey());
        assertEquals("cmdcam", camera.requiredModId());
        assertTrue(camera.hasRequiredMod());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.tool.ToolCatalogTest"
```

Expected: compilation failure because `ToolCatalog`, `ToolId`, and `ToolDescriptor` do not exist.

- [ ] **Step 3: Implement the common types**

`McCreatorUtils`:

```java
public final class McCreatorUtils {
    public static final String MOD_ID = "mccreatorutils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private McCreatorUtils() {}
}
```

`ToolId` enum with `CAMERA("camera")`, a final `String id` field, and `public String id()`.

`ToolDescriptor` record:

```java
public record ToolDescriptor(ToolId id, String translationKey, String requiredModId) {
    public ToolDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(translationKey, "translationKey");
        requiredModId = requiredModId == null ? "" : requiredModId;
    }

    public boolean hasRequiredMod() {
        return !requiredModId.isEmpty();
    }
}
```

`ToolCatalog`:

```java
public static final ToolDescriptor CAMERA =
        new ToolDescriptor(ToolId.CAMERA, "tool.mccreatorutils.camera", "cmdcam");
private static final List<ToolDescriptor> TOOLS = List.of(CAMERA);
public static List<ToolDescriptor> tools() { return TOOLS; }
```

Add Javadoc with `@author Izakji` to every public type.

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.tool.ToolCatalogTest"
```

Expected: PASS, 2 tests.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/McCreatorUtils.java common/src/main/java/com/izakji/mccreatorutils/tool common/src/test/java/com/izakji/mccreatorutils/tool
git commit -m "feat: add shared tool catalog" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 3: Camera enums and duration validation

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/camera/CamMode.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/camera/DurationUnit.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/camera/DurationInput.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/camera/DurationInputTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/camera/CameraEnumTest.java`

**Interfaces:**
- Consumes: JUnit 5 from Task 1.
- Produces:
  - `CamMode.DEFAULT` and `CamMode.OUTSIDE`; `String id()`; `String translationKey()`.
  - `DurationUnit.MILLISECONDS`, `SECONDS`, `MINUTES`, `HOURS`, `DAYS`; `String suffix()`; `String translationKey()`.
  - `DurationInput.MIN_VALUE`, `MAX_VALUE`, `MAX_DIGITS`; `boolean isAllowedCharacter(char)`; `OptionalInt parse(String)`; `boolean isValid(String)`.

- [ ] **Step 1: Write the failing tests**

`DurationInputTest` must cover the exact acceptance list:

```java
@Test
void acceptsValidValues() {
    assertEquals(OptionalInt.of(1), DurationInput.parse("1"));
    assertEquals(OptionalInt.of(20), DurationInput.parse("20"));
    assertEquals(OptionalInt.of(999), DurationInput.parse("999"));
}

@Test
void rejectsInvalidValues() {
    for (String raw : List.of("", "0", "000", "1000", "12a", " 20", "-5", "1.5")) {
        assertTrue(DurationInput.parse(raw).isEmpty(), raw);
    }
    assertTrue(DurationInput.parse(null).isEmpty());
}

@Test
void characterValidatorAllowsOnlyAsciiDigits() {
    assertTrue(DurationInput.isAllowedCharacter('0'));
    assertTrue(DurationInput.isAllowedCharacter('9'));
    assertFalse(DurationInput.isAllowedCharacter('a'));
    assertFalse(DurationInput.isAllowedCharacter('-'));
    assertFalse(DurationInput.isAllowedCharacter('.'));
    assertFalse(DurationInput.isAllowedCharacter(' '));
}
```

`CameraEnumTest`:

```java
@Test
void modesUseCmdcamIdentifiers() {
    assertEquals("default", CamMode.DEFAULT.id());
    assertEquals("outside", CamMode.OUTSIDE.id());
}

@Test
void unitsUseExactCmdcamSuffixes() {
    assertEquals("ms", DurationUnit.MILLISECONDS.suffix());
    assertEquals("s", DurationUnit.SECONDS.suffix());
    assertEquals("m", DurationUnit.MINUTES.suffix());
    assertEquals("h", DurationUnit.HOURS.suffix());
    assertEquals("d", DurationUnit.DAYS.suffix());
}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.camera.*"
```

Expected: compilation failure because the camera types do not exist.

- [ ] **Step 3: Implement the camera enums and validator**

`CamMode`:

```java
public enum CamMode {
    DEFAULT("default"),
    OUTSIDE("outside");

    private final String id;

    CamMode(String id) { this.id = id; }
    public String id() { return id; }

    public String translationKey() {
        return "tool.mccreatorutils.camera.mode." + id;
    }
}
```

`DurationUnit`:

```java
public enum DurationUnit {
    MILLISECONDS("ms"),
    SECONDS("s"),
    MINUTES("m"),
    HOURS("h"),
    DAYS("d");

    private final String suffix;

    DurationUnit(String suffix) { this.suffix = suffix; }
    public String suffix() { return suffix; }

    public String translationKey() {
        return "tool.mccreatorutils.camera.unit." + suffix;
    }
}
```

`DurationInput`:

```java
public static final int MIN_VALUE = 1;
public static final int MAX_VALUE = 999;
public static final int MAX_DIGITS = 3;

public static boolean isAllowedCharacter(char value) {
    return value >= '0' && value <= '9';
}

public static OptionalInt parse(String raw) {
    if (raw == null || raw.isEmpty() || raw.length() > MAX_DIGITS) {
        return OptionalInt.empty();
    }
    for (int index = 0; index < raw.length(); index++) {
        if (!isAllowedCharacter(raw.charAt(index))) {
            return OptionalInt.empty();
        }
    }
    int value = Integer.parseInt(raw);
    return value >= MIN_VALUE && value <= MAX_VALUE ? OptionalInt.of(value) : OptionalInt.empty();
}

public static boolean isValid(String raw) {
    return parse(raw).isPresent();
}
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.camera.*"
```

Expected: PASS, 5 tests.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/camera common/src/test/java/com/izakji/mccreatorutils/camera
git commit -m "feat: add camera values and duration validation" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 4: Camera command construction

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/camera/CameraCommands.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/camera/CameraCommandsTest.java`

**Interfaces:**
- Consumes: `CamMode`, `DurationUnit`, `DurationInput` from Task 3.
- Produces:
  - `String CameraCommands.mode(CamMode mode)`
  - `String CameraCommands.duration(int value, DurationUnit unit)`

- [ ] **Step 1: Write the failing command test**

```java
package com.izakji.mccreatorutils.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CameraCommandsTest {

    @Test
    void buildsModeCommands() {
        assertEquals("cam mode default", CameraCommands.mode(CamMode.DEFAULT));
        assertEquals("cam mode outside", CameraCommands.mode(CamMode.OUTSIDE));
    }

    @Test
    void buildsDurationCommandsWithoutSpaceBeforeUnit() {
        assertEquals("cam duration 20s", CameraCommands.duration(20, DurationUnit.SECONDS));
        assertEquals("cam duration 500ms", CameraCommands.duration(500, DurationUnit.MILLISECONDS));
        assertEquals("cam duration 2m", CameraCommands.duration(2, DurationUnit.MINUTES));
        assertEquals("cam duration 1h", CameraCommands.duration(1, DurationUnit.HOURS));
        assertEquals("cam duration 3d", CameraCommands.duration(3, DurationUnit.DAYS));
    }

    @Test
    void rejectsNullMode() {
        assertThrows(NullPointerException.class, () -> CameraCommands.mode(null));
    }

    @Test
    void rejectsDurationOutsideRange() {
        assertThrows(IllegalArgumentException.class,
                () -> CameraCommands.duration(0, DurationUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> CameraCommands.duration(1000, DurationUnit.SECONDS));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.camera.CameraCommandsTest"
```

Expected: compilation failure because `CameraCommands` does not exist.

- [ ] **Step 3: Implement `CameraCommands`**

```java
public static String mode(CamMode mode) {
    Objects.requireNonNull(mode, "mode");
    return "cam mode " + mode.id();
}

public static String duration(int value, DurationUnit unit) {
    Objects.requireNonNull(unit, "unit");
    if (value < DurationInput.MIN_VALUE || value > DurationInput.MAX_VALUE) {
        throw new IllegalArgumentException("Duration must be between 1 and 999");
    }
    return "cam duration " + value + unit.suffix();
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.camera.CameraCommandsTest"
```

Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/camera/CameraCommands.java common/src/test/java/com/izakji/mccreatorutils/camera/CameraCommandsTest.java
git commit -m "feat: build camera command strings" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 5: NeoForge entrypoint, keybind, and two-pane GUI shell

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/McCreatorUtilsNeoForge.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/McCreatorUtilsClient.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/CreatorToolsScreen.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/CreatorToolsUi.java`
- Modify: `neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json`

**Interfaces:**
- Consumes: `McCreatorUtils.MOD_ID`, `ToolCatalog.tools()`, `ToolDescriptor`, `ToolId`.
- Produces:
  - `CreatorToolsScreen()` no-arg constructor opening the modular screen.
  - `CreatorToolsUi.create()` returning a `ModularUI`.
  - Keybind constant `McCreatorUtilsClient.OPEN_GUI`.
  - Placeholder right-pane content for the Camera tool, replaced in Task 6.

- [ ] **Step 1: Add the common `@Mod` entrypoint**

```java
@Mod(McCreatorUtils.MOD_ID)
public final class McCreatorUtilsNeoForge {
    public McCreatorUtilsNeoForge(IEventBus modEventBus) {
        // Common-side initialization is intentionally empty for this milestone.
    }
}
```

- [ ] **Step 2: Add the client entrypoint and keybind**

Use `@Mod(value = McCreatorUtils.MOD_ID, dist = Dist.CLIENT)` and `@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)`.

```java
public static final KeyMapping OPEN_GUI = new KeyMapping(
        "key.mccreatorutils.open_gui",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_PERIOD,
        "key.categories.mccreatorutils");

@SubscribeEvent
static void registerKeyMappings(RegisterKeyMappingsEvent event) {
    event.register(OPEN_GUI);
}

@SubscribeEvent
static void onClientTick(ClientTickEvent.Post event) {
    Minecraft minecraft = Minecraft.getInstance();
    while (OPEN_GUI.consumeClick()) {
        if (minecraft.player != null && minecraft.screen == null) {
            minecraft.setScreen(new CreatorToolsScreen());
        }
    }
}
```

- [ ] **Step 3: Add the screen shell**

```java
public final class CreatorToolsScreen extends ModularUIScreen {
    public CreatorToolsScreen() {
        super(CreatorToolsUi.create(),
                Component.translatable("screen.mccreatorutils.creator_tools"));
    }
}
```

- [ ] **Step 4: Build the two-pane shell in `CreatorToolsUi`**

Use a `UI.DynamicSizeProvider` and `UI.of(root, provider)`:

```java
int width = Math.max(1, Math.min(360, screenSize.getWidth() - 20));
int height = Math.max(1, Math.min(240, screenSize.getHeight() - 20));
return Size.of(width, height);
```

Required element structure:

- Root: row layout, padding `6`, gap `6`, background `Sprites.BORDER`.
- Left pane: width `104`, height `100%`, column layout, padding `4`, gap `4`, background `Sprites.RECT_DARK`.
- `ScrollerView` tool list: width `100%`, flex `1`; vertical scroll `AUTO`, horizontal scroll `NEVER`.
- One `Button` per `ToolCatalog.tools()` entry, height `16`, width `100%`.
- Selected button base texture `Sprites.RECT_RD_LIGHT`; unselected `Sprites.RECT_RD`.
- Right pane: flex `1`, height `100%`, padding `6`, column layout, gap `4`, background `Sprites.RECT_DARK`.
- Right heading `Label` height `12`; divider `UIElement` height `1` with `ColorPattern.DARK_GRAY.color` background;
  content container flex `1`.
- Selecting the Camera tool sets the heading to `Component.translatable(ToolCatalog.CAMERA.translationKey())`
  and replaces content with a `Label` using `tool.mccreatorutils.camera.placeholder`.

Store tool buttons in `EnumMap<ToolId, Button>` and keep the selected tool in a field. Call the selection
method once at the end of `create()` so the first tool is selected on open.

- [ ] **Step 5: Add translations**

Add these keys to `en_us.json`:

```json
"key.categories.mccreatorutils": "Creator Utils",
"key.mccreatorutils.open_gui": "Open Creator Utils",
"screen.mccreatorutils.creator_tools": "Creator Utils",
"tool.mccreatorutils.camera": "Camera",
"tool.mccreatorutils.camera.placeholder": "Camera controls are being prepared."
```

- [ ] **Step 6: Compile and inspect the jar**

```bash
./gradlew :neoforge:compileJava :neoforge:processResources
jar tf neoforge/build/libs/mccreatorutils-1.0.0.jar | grep -E "neoforge.mods.toml|en_us.json"
```

Expected: compilation succeeds; both resources are present in the jar.

- [ ] **Step 7: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json
git commit -m "feat: add keybind and two-pane creator GUI" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 6: Camera tool panel, command dispatch, and disabled state

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/ClientCommandRunner.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/CameraToolPanel.java`
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/client/CreatorToolsUi.java`
- Modify: `neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json`

**Interfaces:**
- Consumes: `CamMode`, `DurationUnit`, `DurationInput`, `CameraCommands`, `ToolCatalog.CAMERA`, `McCreatorUtils.LOGGER`.
- Produces:
  - `ClientCommandRunner.CommandResult(boolean success, Component message)`.
  - `ClientCommandRunner.execute(String command) -> CommandResult`.
  - `CameraToolPanel()` UI element used by `CreatorToolsUi` for the Camera tool.

- [ ] **Step 1: Implement `ClientCommandRunner`**

```java
@OnlyIn(Dist.CLIENT)
public final class ClientCommandRunner {
    public record CommandResult(boolean success, Component message) {}

    public static CommandResult execute(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return new CommandResult(false,
                    Component.translatable("status.mccreatorutils.no_player"));
        }
        try {
            player.connection.sendCommand(command);
            return new CommandResult(true,
                    Component.translatable("status.mccreatorutils.ran", command));
        } catch (RuntimeException exception) {
            McCreatorUtils.LOGGER.error("Failed to execute client command: {}", command, exception);
            return new CommandResult(false,
                    Component.translatable("status.mccreatorutils.failed", command));
        }
    }
}
```

- [ ] **Step 2: Build `CameraToolPanel`**

`CameraToolPanel` extends `UIElement` and uses a column layout, width/height `100%`, gap `5`.

Required children in order:

1. Description `Label`: `tool.mccreatorutils.camera.description`.
2. Mode section:
   - `Label` `tool.mccreatorutils.camera.mode`.
   - Row: `Selector<CamMode>` flex `1`, height `14`; Run `Button` width `72`, height `16`.
   - Selector candidates `List.of(CamMode.values())`; display text from each mode's `translationKey()`;
     null preview text `tool.mccreatorutils.camera.mode.select`.
   - Run button text `tool.mccreatorutils.camera.set_mode`; initially `setActive(false)`.
   - On mode change, enable only when the value is non-null.
   - On click, call `runCommand(CameraCommands.mode(modeSelector.getValue()))` only when non-null.
3. Duration section:
   - `Label` `tool.mccreatorutils.camera.duration`.
   - Row: `TextField` flex `1`, height `14`; `Selector<DurationUnit>` width `58`, height `14`.
   - Row: Run `Button` width `100%`, height `16`, text `tool.mccreatorutils.camera.set_duration`.
   - Text field: `setCharValidator(DurationInput::isAllowedCharacter)`,
     `setTextValidator(DurationInput::isValid)`, placeholder `tool.mccreatorutils.camera.duration.placeholder`.
   - Unit selector: candidates `List.of(DurationUnit.values())`; display via `translationKey()` with a
     null-safe preview; selected default `DurationUnit.SECONDS`.
   - Text field placeholder is set through `textFieldStyle(style -> style.placeholder(...))`; there is
     no direct `setPlaceholder` method.
   - Duration Run begins disabled.
   - Add a `UIEvents.TICK` listener to the text field that recomputes the button state from
     `durationField.getRawText()` and `unitSelector.getValue()`. This is required because an invalid
     fourth digit does not notify value listeners.
   - On click, parse `getRawText()` and call `runCommand(CameraCommands.duration(value, unit))`.
4. Status `Label`: initially `Component.empty()`; update from `ClientCommandRunner.execute`.
5. CMDCam notice `Label`: only when `ModList.get().isLoaded(ToolCatalog.CAMERA.requiredModId())` is false.

When CMDCam is absent:

- Build the controls inside a `controls` container and call `controls.setActive(false)`.
- Add the notice after disabling controls, using `notice.mccreatorutils.cmdcam_missing`.
- Do not construct command actions that can run while disabled; the active-hierarchy checks and
  selector guards must both prevent dispatch.

`runCommand(String command)`:

```java
ClientCommandRunner.CommandResult result = ClientCommandRunner.execute(command);
status.setText(result.message());
status.textStyle(style -> style.textColor(
        result.success() ? ColorPattern.GREEN.color : ColorPattern.RED.color));
```

- [ ] **Step 3: Wire the panel into `CreatorToolsUi`**

In the selection method, replace the Camera placeholder with:

```java
if (descriptor.id() == ToolId.CAMERA) {
    content.addChild(new CameraToolPanel());
}
```

- [ ] **Step 4: Add the remaining translations**

```json
"tool.mccreatorutils.camera.description": "Automates CMDCam scene commands.",
"tool.mccreatorutils.camera.mode": "Camera mode",
"tool.mccreatorutils.camera.mode.select": "Select mode",
"tool.mccreatorutils.camera.mode.default": "Default",
"tool.mccreatorutils.camera.mode.outside": "Outside",
"tool.mccreatorutils.camera.duration": "Scene duration",
"tool.mccreatorutils.camera.duration.placeholder": "1-999",
"tool.mccreatorutils.camera.unit.ms": "Milliseconds",
"tool.mccreatorutils.camera.unit.s": "Seconds",
"tool.mccreatorutils.camera.unit.m": "Minutes",
"tool.mccreatorutils.camera.unit.h": "Hours",
"tool.mccreatorutils.camera.unit.d": "Days",
"tool.mccreatorutils.camera.set_mode": "Set mode",
"tool.mccreatorutils.camera.set_duration": "Set duration",
"status.mccreatorutils.ran": "Ran: /%s",
"status.mccreatorutils.no_player": "No player available.",
"status.mccreatorutils.failed": "Command failed: /%s",
"notice.mccreatorutils.cmdcam_missing": "CMDCam is required for camera tools."
```

- [ ] **Step 5: Compile and run tests**

```bash
./gradlew :common:test :neoforge:compileJava
```

Expected: all common tests pass; NeoForge compilation succeeds.

- [ ] **Step 6: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json
git commit -m "feat: add camera mode and duration tools" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 7: Acceptance verification and cleanup

**Files:**
- Modify only if verification exposes a defect.

**Interfaces:**
- Consumes: all prior tasks.
- Produces: verified milestone.

- [ ] **Step 1: Format and run the full build**

```bash
./gradlew spotlessApply
./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`; all common tests pass.

- [ ] **Step 2: Verify loader isolation**

```bash
grep -R -n -E "net\.neoforged|com\.lowdragmc|net\.fabricmc" common/src/main/java common/src/test/java
```

Expected: no output.

- [ ] **Step 3: Inspect mod metadata and resources**

```bash
jar tf neoforge/build/libs/mccreatorutils-1.0.0.jar | grep -E "neoforge.mods.toml|lang/en_us.json|CameraToolPanel|CreatorToolsScreen"
```

Expected: all entries present; `neoforge.mods.toml` is under `META-INF/`.

- [ ] **Step 4: Launch the client**

```bash
./gradlew :neoforge:runClient
```

Manual checklist:

- Press `.` in a world; the Creator Utils window opens, centered, at no larger than `360 x 240`.
- The left pane shows the Camera button selected; the right pane shows the Camera heading and description.
- Resize the Minecraft window or change GUI scale; the window stays inside the screen with margins.
- Confirm the Camera controls are disabled and the notice says CMDCam is required when CMDCam is absent.
- If a CMDCam 1.21.1 jar is placed in the dev run's `mods` folder, relaunch and verify:
  - Mode Run stays disabled until a mode is selected; selecting `outside` then clicking runs
    `cam mode outside`.
  - Duration Run stays disabled for empty, `0`, and `1000`; `20` with `s` runs `cam duration 20s`.
  - Typing a fourth digit does not leave the Run button enabled.
  - The GUI status label reports the command.
- Open Options > Controls and confirm the keybind appears under Creator Utils and can be rebound.

- [ ] **Step 5: Commit any verification fixes**

If files changed:

```bash
git add -A
git commit -m "fix: address acceptance verification findings" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

If no files changed, report that the working tree is clean; do not create an empty commit.
