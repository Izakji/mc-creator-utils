# Creator Utils Foundation and Camera Tool Design

**Date:** 2026-10-27
**Status:** Draft for review
**Target:** Minecraft 1.21.1, NeoForge 21.1.265
**Future targets:** Fabric, newer Minecraft versions

## 1. Purpose

Creator Utils is a Minecraft mod that gives creators a single in-game control panel for common
content-creation tasks. The first milestone establishes the multiplatform project structure, the
central two-pane graphical interface, and the Camera tool that automates two CMDCam commands.

The mod must be structured so that a Fabric module and later Minecraft versions can be added without
rewriting the loader-independent tool logic.

## 2. Scope

### In scope

- Convert the starter NeoForge project into a Gradle multi-project build with `common` and
  `neoforge` modules.
- Remove all starter example content: example block, example item, creative tab, and example config.
- Add LDLib2 as a NeoForge dependency and use it to build the main GUI.
- Add CMDCam as an optional client-side runtime dependency.
- Register a rebindable keybind with default `.` (`GLFW_KEY_PERIOD`) that opens the GUI.
- Build a two-pane GUI:
  - Left pane: scrollable list of available tools.
  - Right pane: content for the selected tool.
- Build the Camera tool with:
  - `/cam mode` automation through a mode dropdown (`default`, `outside`) and a Run button.
  - `/cam duration` automation through a digits-only number field (1 through 999), a unit dropdown
    (`ms`, `s`, `m`, `h`, `d`), and a Run button.
- Show a clear disabled state when CMDCam is not installed.
- Add unit tests for loader-independent command construction and duration validation.

### Out of scope

- Fabric implementation. The `common` module must remain free of loader APIs so Fabric can reuse it.
- Additional creator tools such as time, dimension, structure location, biome location, entity
  spawning, and recording controls.
- Server-side tool execution or synchronized tool state.
- Custom GUI textures, themes, or a full visual redesign.
- Additional camera commands beyond `mode` and `duration`.

## 3. Architectural Decisions

### 3.1 Multi-project layout

The repository uses a Gradle multi-project build:

```
mc-creator-utils/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── common/
│   ├── build.gradle
│   └── src/main/java/com/izakji/mccreatorutils/...
└── neoforge/
    ├── build.gradle
    └── src/main/...
```

The root build applies shared Java 21 toolchain and formatting configuration. The `common` module
compiles against the NeoForm Minecraft artifact, matching the reference `stuff-ds` project. It exposes
its Java sources and resources through the `commonJava` and `commonResources` configurations. The
`neoforge` module consumes those configurations and combines them with NeoForge-specific sources and
resources.

The `common` module must not import NeoForge, Fabric, or LDLib2 classes. It may contain only plain
Java and, where necessary, loader-neutral Minecraft classes.

### 3.2 Responsibility split

- `common` owns:
  - Mod identifier and shared constants.
  - Tool catalog metadata.
  - Camera mode and duration unit enumerations.
  - Duration validation rules.
  - Camera command string construction.
- `neoforge` owns:
  - The `@Mod` entrypoint and client entrypoint.
  - LDLib2 screen construction.
  - Keybind registration and client tick handling.
  - CMDCam presence detection through the NeoForge mod list.
  - Client command dispatch through the Minecraft connection.

Fabric will later provide its own loader entrypoint and UI rendering, while reusing the `common`
catalog, validation, and command construction.

### 3.3 Dependencies

- LDLib2 version `2.2.44` from `https://maven.firstdark.dev/snapshots`.
- LDLib2 is a required dependency in `neoforge.mods.toml` because the GUI cannot function without it.
- CMDCam is an optional client-side dependency. It is not a compile dependency because Creator Utils
  only sends command strings through Minecraft's command system.
- CreativeCore is not declared separately. CMDCam already requires it.

### 3.4 Command execution

Commands execute on the client through:

```java
Minecraft.getInstance().player.connection.sendCommand(commandWithoutLeadingSlash);
```

NeoForge routes this call through `ClientCommandHandler`, which executes registered client-side
commands before forwarding unknown commands to the server. CMDCam registers `/cam` as a client
command, so this path executes CMDCam locally and preserves its chat feedback.

Creator Utils never sends commands while the local player or connection is absent. It catches runtime
failures from command dispatch, logs them, and surfaces a failure status in the GUI.

## 4. Package and Class Design

### 4.1 Common module

Package root: `com.izakji.mccreatorutils`

- `McCreatorUtils`
  - Holds `MOD_ID`, display constants, and the shared logger.
- `tool.ToolId`
  - Enumeration of tool identifiers. Initial value: `CAMERA`.
- `tool.ToolDescriptor`
  - Immutable record containing:
    - `ToolId id`
    - `String translationKey`
    - `String requiredModId` (nullable or empty when no dependency is required)
- `tool.ToolCatalog`
  - Provides the ordered list of tools. Initial list contains only the Camera tool.
  - Ordering is stable and is the order shown in the GUI.
- `camera.CamMode`
  - Enumeration with command value and translation key:
    - `DEFAULT("default")`
    - `OUTSIDE("outside")`
- `camera.DurationUnit`
  - Enumeration with command suffix and translation key:
    - `MILLISECONDS("ms")`
    - `SECONDS("s")`
    - `MINUTES("m")`
    - `HOURS("h")`
    - `DAYS("d")`
- `camera.DurationInput`
  - Pure validation helper.
  - Accepts only strings composed of ASCII digits `0` through `9`.
  - Rejects empty strings.
  - Rejects strings longer than three characters.
  - Parses the value and accepts only `1` through `999`.
  - Returns an optional parsed integer or a validation result suitable for GUI enabling.
- `camera.CameraCommands`
  - Builds exact command strings without a leading slash:
    - `cam mode <mode>`
    - `cam duration <value><unit>`, for example `cam duration 20s`
  - Rejects invalid inputs by construction, using the validated value type.

### 4.2 NeoForge module

Package root: `com.izakji.mccreatorutils`

- `McCreatorUtilsNeoForge`
  - `@Mod(McCreatorUtils.MOD_ID)` entrypoint.
  - Registers the client entrypoint only on the physical client.
- `client.McCreatorUtilsClient`
  - `@Mod(value = McCreatorUtils.MOD_ID, dist = Dist.CLIENT)`.
  - Registers the keybind through `RegisterKeyMappingsEvent`.
  - Listens to `ClientTickEvent.Post` and consumes key presses.
  - Opens `CreatorToolsScreen` when the key is pressed.
- `client.CreatorToolsScreen`
  - Extends LDLib2 `ModularUIScreen`.
  - Hosts `CreatorToolsUi`.
- `client.CreatorToolsUi`
  - Builds a `ModularUI` with dynamic sizing.
  - Target size: `360 x 240` GUI-scaled pixels, clamped to the screen with a small margin.
  - Root layout is a horizontal split with a fixed-width left pane and a flexible right pane.
  - Left pane uses `ScrollerView` so additional tools remain reachable.
  - Left pane contains one button per `ToolDescriptor`.
  - Right pane displays the selected tool panel and swaps content when selection changes.
  - Initial selection is the first tool in `ToolCatalog`.
- `client.CameraToolPanel`
  - Builds the Camera tool controls.
  - Mode section:
    - A label.
    - A `Selector<CamMode>` populated from `CamMode.values()`.
    - A Run button disabled until a mode is selected.
  - Duration section:
    - A label.
    - A `TextField` with a custom character validator allowing only ASCII digits and a text
      validator enforcing 1 through 999 with at most three characters.
    - A `Selector<DurationUnit>` populated from `DurationUnit.values()`, defaulting to seconds.
    - A Run button enabled only when the number field is valid and a unit is selected.
  - Command buttons call `ClientCommandRunner`.
  - A status label shows the last executed command or a short failure message.
  - When CMDCam is absent, all camera controls are disabled and a visible notice explains that
    CMDCam is required.
- `client.ClientCommandRunner`
  - Checks that a local player and connection exist.
  - Dispatches the command through `player.connection.sendCommand`.
  - Returns a result or reports failures to the caller for status display.

## 5. User Interface Requirements

### 5.1 Main window

- Title: "Creator Utils".
- Visual style: LDLib2 built-in sprites and widget defaults, with light custom layout styling.
- Window is centered and does not pause the game.
- `Escape` closes the window through the normal screen behavior.
- Left pane width: approximately 100-110 GUI-scaled pixels.
- Right pane: remaining width, with padding.
- Left pane tools are vertically stacked buttons.
- Selected tool button is visually distinguishable.
- Right pane has a heading with the selected tool name and a divider below it.

### 5.2 Camera tool layout

- Heading: "Camera".
- Short description: "Automates CMDCam scene commands."
- Mode subsection:
  - Label: "Camera mode".
  - Dropdown placeholder: "Select mode".
  - Run button label: "Set mode".
- Duration subsection:
  - Label: "Scene duration".
  - Number field placeholder: "1-999".
  - Unit dropdown default: `s`.
  - Run button label: "Set duration".
- Status area:
  - Empty before any command.
  - Shows `Ran: /cam ...` after a successful dispatch.
  - Shows a short failure message when dispatch fails.
  - Shows "CMDCam is required for camera tools." when CMDCam is absent.

### 5.3 Input rules

- Number field accepts only ASCII digits.
- Number field rejects:
  - Empty input.
  - More than three digits.
  - Zero.
  - Values above 999.
- Mode Run button is disabled until the user picks a mode.
- Duration Run button is disabled until the number is valid and a unit is selected.
- Disabled controls do not execute commands.

## 6. Localization

All user-facing text uses translation keys under the `mccreatorutils` namespace. The initial
`en_us.json` includes at least:

- Keybind name and category.
- Screen title.
- Tool names.
- Camera panel labels, placeholders, button labels, status messages, and dependency notice.

## 7. Error Handling

- Missing CMDCam: mod loads normally; Camera controls are disabled with an explanatory notice.
- Missing player or connection: no command is sent; screen closes if the player is no longer available.
- Command dispatch exception: caught, logged, and reported in the status area.
- Invalid duration input: Run button stays disabled; no exception is thrown.
- LDLib2 missing: the game refuses to load because LDLib2 is a required dependency. This is intentional
  and documented in mod metadata.

## 8. Testing and Verification

### Automated

- Add JUnit 5 tests for `common`:
  - `DurationInput` accepts `"1"`, `"20"`, `"999"`.
  - `DurationInput` rejects `""`, `"0"`, `"000"`, `"1000"`, `"12a"`, `" 20"`, `"-5"`, `"1.5"`.
  - `DurationInput` rejects values longer than three digits.
  - `CameraCommands.mode` produces exactly `cam mode default` and `cam mode outside`.
  - `CameraCommands.duration` produces exactly `cam duration 20s` and `cam duration 500ms`.
  - `ToolCatalog` contains the Camera tool exactly once and preserves declaration order.
- Run `./gradlew build` and require success.

### Manual

- Run `./gradlew :neoforge:runClient`.
- Press `.` in a world and confirm the GUI opens.
- Confirm left-pane Camera selection and right-pane Camera content.
- Confirm mode Run is disabled until a mode is selected, then executes `cam mode ...`.
- Confirm duration Run is disabled for empty, `0`, and values above 999, and executes
  `cam duration ...` for valid values.
- Confirm CMDCam absence produces the disabled notice and no commands are sent.
- Confirm the keybind appears in Options > Controls and can be rebound.

## 9. Acceptance Criteria

- The repository builds with `./gradlew build`.
- The game launches on NeoForge 1.21.1 with the mod and LDLib2 installed.
- Pressing `.` opens the Creator Utils GUI.
- The GUI shows a two-pane layout with a scrollable tool list and tool content pane.
- The Camera tool is present and selected by default.
- The mode control lists `default` and `outside`; its Run button follows the enabling rules.
- The duration control enforces digits only and 1 through 999; its Run button follows the enabling
  rules.
- Valid actions execute the correct client-side CMDCam command.
- Missing CMDCam produces a visible disabled state rather than a crash.
- `common` contains no loader-specific imports, preserving the future Fabric path.
