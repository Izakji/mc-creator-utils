# Creator Utils

A tool for Minecraft content creators.

Creator Utils adds an in-game control panel for the commands creators use while filming,
building scenes, and testing content. It targets Minecraft 1.21.1 on NeoForge and keeps its
loader-neutral logic in a reusable `common` module.

## Features

- **Camera** — CMDCam scene mode and duration controls (requires CMDCam).
- **World Time** — time presets, exact ticks, freeze time, and weather.
- **Registry tools** — browse and search Biomes, Structures, Dimensions, Items, and Mobs.
- **Async locate** — biome and structure searches run off the server tick with cancellation,
  timeouts, caching, and modded-biome support.
- **Player** — invincibility, water breathing, effect clearing, and experience commands.
- **Model previews** — draggable, zoomable item and mob previews.
- **Explore Mods** — search installed mods and open their CurseForge or Modrinth page.
- **Browser workspace** — a floating, resizable, multi-tab Chromium browser with a session
  snapshot and an in-game fullscreen mode (requires Rinku).

## Requirements

| Dependency | Required | Purpose |
| --- | --- | --- |
| Minecraft 1.21.1 | Yes | Target game version |
| NeoForge 21.1.265+ | Yes | Mod loader |
| LDLib2 2.2.44+ | Yes | User interface |
| Rinku 3.0.4+ | Optional | In-game browser |
| CMDCam 2.0.0+ | Optional | Camera tool |
| CreativeCore 2.13.32+ | Optional | CMDCam dependency |

Creator Utils loads normally when optional dependencies are absent; the affected tool shows a
notice instead of failing.

## Controls

| Key | Action |
| --- | --- |
| `.` | Open or close the Creator Utils panel |
| `]` | Toggle the floating browser |
| `B` | Focus the browser and release the cursor |

All keybinds are configurable in Minecraft's Controls screen.

## Building

```bash
./gradlew build
```

The NeoForge jar is written to `neoforge/build/libs/`. Rinku and Chromium are never embedded in
the Creator Utils jar.

## Project layout

- `common/` — loader-neutral rules, commands, search, layout, and tests.
- `neoforge/` — NeoForge client/server integration, UI panels, mixins, and Rinku adapter.

## License

See the mod metadata for the configured license.
