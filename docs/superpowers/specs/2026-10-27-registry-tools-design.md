# Registry Tools Design

**Date:** 2026-10-27
**Status:** Draft for review
**Target:** Minecraft 1.21.1, NeoForge 21.1.265
**Branch:** `feat/registry-tools`

## 1. Purpose

Add four registry-driven Creator Utils tools:

- **Biomes** — browse mods and their biomes, then teleport to a biome.
- **Structures** — browse mods and their structures, then teleport to a structure.
- **Items** — browse mods and their items, give a selected item, clear the player inventory,
  or clear dropped item entities.
- **Mobs** — browse mods and their mobs, summon a selected mob, or kill all non-player entities.

The tools follow the existing Creator Utils pattern: a left navigation list and a right tool panel,
client-side command dispatch, session persistence, and loader-neutral logic in `common`.

## 2. Scope

### In scope

- Four new `ToolId` and `ToolCatalog` entries.
- Mod and entry dropdowns built from live Minecraft registries.
- Grouping entries by registry namespace, with a display name for the owning mod.
- Biome and structure teleport through vanilla `/locate` plus the clickable `/tp` command returned in
  vanilla chat feedback.
- Item give with a validated count of 1 through 512.
- Clear inventory, clear world item entities, summon mob, and kill all non-player entities actions.
- Session persistence for the new tool selections.
- Unit tests for grouping, validation, command strings, and session state.

### Out of scope

- Fabric implementation and newer Minecraft versions.
- A server-side custom locate/teleport command.
- Searching unloaded chunks locally.
- Per-world or on-disk persistence.
- NBT or data-component customization for give/summon.
- Structure placement preview or biome map rendering.
- Permission management beyond surfacing command failures.
- Pagination or search for very large item/mob lists; LDLib2 scrollable selectors are sufficient.

## 3. Decisions

- **Teleport mechanism:** send `/locate biome|structure <id>`, wait for vanilla's success chat
  message, extract the clickable `/tp @s ...` command from the message component, and execute it.
  This works on servers and uses vanilla search behavior. It requires permission level 2, can take a
  few seconds, and structure teleports retain the player's current Y through vanilla's relative `~`.
- **Mod grouping:** group by registry namespace, include `minecraft` as “Minecraft”, and show the
  owning mod's display name when available. Unknown namespaces fall back to the namespace string.
- **Mob filter:** include entity types whose `MobCategory` is not `MISC`, excluding the player.
- **Persistence:** all new selections persist in memory for the current client session, matching the
  existing Camera and World Time behavior. Restarting the game resets them.
- **Panel reuse:** one shared `RegistryPicker` component builds the mod and entry dropdowns; four thin
  panels add their own action controls.

## 4. Architecture

### 4.1 Common module

New loader-neutral classes under `com.izakji.mccreatorutils`:

- `tool.ToolId`
  - Add `BIOMES("biomes")`, `STRUCTURES("structures")`, `ITEMS("items")`, `MOBS("mobs")`.
- `tool.ToolCatalog`
  - Add descriptors with translation keys:
    - `tool.mccreatorutils.biomes`
    - `tool.mccreatorutils.structures`
    - `tool.mccreatorutils.items`
    - `tool.mccreatorutils.mobs`
  - No required mod id for any of the four.
  - Order after `WORLD_TIME`.
- `registry.RegistryGroup`
  - Record: `String namespace`, `List<ResourceLocation> entries`.
- `registry.RegistryGrouping`
  - `List<RegistryGroup> group(Collection<ResourceLocation> ids)`.
  - Groups by namespace, drops empty groups, sorts entries by path.
  - Does not resolve display names; that is loader-specific.
- `items.ItemCountInput`
  - Digits only, at most three characters, numeric range 1 through 512.
  - `OptionalInt parse(String)`, `boolean isValid(String)`, `boolean isAllowedCharacter(char)`.
- `items.GiveCommands`
  - `give(ResourceLocation item, int count)` → `give @s <id> <count>`.
  - `clearInventory()` → `clear`.
  - `clearWorldItems()` → `kill @e[type=item]`.
- `mobs.MobCommands`
  - `summon(ResourceLocation entityType)` → `summon <id>`.
  - `killAllExceptPlayers()` → `kill @e[type=!minecraft:player]`.
- `locate.LocateCommands`
  - `biome(ResourceLocation biome)` → `locate biome <id>`.
  - `structure(ResourceLocation structure)` → `locate structure <id>`.
- `session.CreatorToolSession`
  - Add nullable selections:
    - `biomeSelection()`
    - `structureSelection()`
    - `itemSelection()`
    - `mobSelection()`
  - Add `itemCountRaw()`, default `"1"`; null normalizes to `"1"`.
  - Selection setters accept null to clear.
  - Session singleton continues to reset on game restart.

### 4.2 NeoForge module

New client classes under `com.izakji.mccreatorutils.client`:

- `registry.RegistryKind`
  - Enum: `BIOME`, `STRUCTURE`, `ITEM`, `MOB`.
  - Loads registry entries and resolves entry labels:
    - Biome: `Component.translatableWithFallback("biome.<namespace>.<path>", prettyPath)`.
    - Structure: `prettyPath` (for example `ancient_city` → `Ancient City`).
    - Item: `Item.getDescription()`, look up via `BuiltInRegistries.ITEM`.
    - Mob: `EntityType.getDescription()`, look up via `BuiltInRegistries.ENTITY_TYPE`.
- `registry.RegistryCatalog`
  - `List<RegistryGroup> groups(RegistryKind kind)`.
  - Item entries: `BuiltInRegistries.ITEM.keySet()`, excluding `minecraft:air`.
  - Mob entries: `BuiltInRegistries.ENTITY_TYPE.keySet()`, excluding `MobCategory.MISC` and `EntityType.PLAYER`.
  - Biome entries: current level's `RegistryAccess` registry `Registries.BIOME`.
  - Structure entries: current level's `RegistryAccess` registry `Registries.STRUCTURE`.
  - Sort groups by display name with Minecraft first; entries remain path-sorted.
  - Resolve mod display names through `ModList.get().getModContainerById(namespace)`.
- `registry.RegistryPicker`
  - Shared UI component: mod selector, entry selector, entry callback.
  - Mod selector displays mod display names; entry selector displays `RegistryKind` labels.
  - Entry selector starts disabled and empty until a mod is selected.
  - Mod change clears the entry selection and notifies the callback with null.
  - Restoring a saved entry finds its group, selects that mod, and selects the entry.
  - Missing saved entries leave both selectors unselected.
  - Exposes the selected `ResourceLocation`.
- `BiomesToolPanel`, `StructuresToolPanel`
  - Picker plus **Teleport** button and status label.
  - Teleport disabled until an entry is selected and while a locate request is pending.
  - Click sends the locate command, sets status to “Searching…”, and calls
    `ClientLocateTeleporter.requestBiome/requestStructure`.
- `ItemsToolPanel`
  - Picker, count field, **Give**, **Clear Inventory**, **Clear World Items**, status label.
  - Count field enabled only when an item is selected; default `"1"`; digits only, 1–512.
  - Give enabled only when an item is selected and the count is valid.
  - Give sends `give @s <item> <count>`.
  - Clear Inventory and Clear World Items are always enabled.
- `MobsToolPanel`
  - Picker, **Summon**, **Kill All Entities**, status label.
  - Summon enabled only when a mob is selected; sends `summon <id>`.
  - Kill All Entities is always enabled; sends `kill @e[type=!minecraft:player]`.
- `ClientLocateTeleporter`
  - Game-bus listener for `ClientChatReceivedEvent` and `ClientTickEvent.Post`.
  - One pending request at a time:
    - target display name,
    - expected success translation key (`commands.locate.biome.success` or
      `commands.locate.structure.success`),
    - failure translation keys (`commands.locate.biome.not_found` or
      `commands.locate.structure.not_found`),
    - expiry timestamp (15 seconds),
    - callback.
  - `requestBiome(ResourceLocation, Component targetName, Consumer<CommandResult>)`
    and `requestStructure(...)`.
  - Sends the corresponding locate command through `ClientCommandRunner`.
  - On a matching chat message:
    - Recursively walk the component tree, including translatable arguments.
    - Find a `SUGGEST_COMMAND` click event whose value starts with `/tp @s `.
    - Execute the command without its leading slash through `ClientCommandRunner`.
    - Report success with a “Teleported to <name>” status.
  - On a matching failure key, callback failure.
  - On timeout, callback failure and clear the pending request.
  - Ignore unrelated chat messages; do not cancel any chat message.
- `CommandStatusLabel`
  - Add helpers for info, success, and failure messages so locate progress can be shown without
    going through `runCommand`.
- `CreatorToolsUi`
  - Add panel cases for the four new tools.
  - Left navigation automatically grows through `ToolCatalog`.

## 5. User Interface

### 5.1 Navigation

- Left pane shows, in order: Camera, World Time, Biomes, Structures, Items, Mobs.
- The existing scrollable list remains; six 16-pixel buttons fit without scrolling at normal sizes.

### 5.2 Shared picker layout

Each registry panel begins with:

1. Mod selector row.
2. Entry selector row.

The entry selector is disabled until a mod is selected. Both selectors use the existing LDLib2
`Selector` widget, which provides a scroller for large candidate lists.

### 5.3 Biomes and Structures panels

- Description label.
- Picker.
- **Teleport** button.
- Status label.
- Status states: empty, “Searching…”, “Teleported to <name>”, and failure.

### 5.4 Items panel

- Description label.
- Picker.
- Count row: digit field with placeholder `1-512`; field disabled until an item is selected.
- **Give** button.
- Row with **Clear Inventory** and **Clear World Items**.
- Status label.

### 5.5 Mobs panel

- Description label.
- Picker.
- **Summon** button.
- **Kill All Entities** button.
- Status label.

All panels fit the existing 360 × 240 window; the Items panel is the tallest at roughly 150 pixels of
content inside an approximately 191-pixel content area.

## 6. Persistence

`CreatorToolSession` stores:

- `biomeSelection`: nullable `ResourceLocation`.
- `structureSelection`: nullable `ResourceLocation`.
- `itemSelection`: nullable `ResourceLocation`.
- `itemCountRaw`: string, default `"1"`.
- `mobSelection`: nullable `ResourceLocation`.

Panels write selections as they change and read them when constructed. The shared picker restores a
selection by matching its namespace and entry. If the entry is no longer present, the picker shows no
selection and the panel action stays disabled.

## 7. Error Handling

- No world or registry access: panel shows an empty picker and disabled action; no command is sent.
- No mod selected or no entry selected: action disabled.
- Invalid item count: Give disabled; click-time parsing prevents execution.
- Locate command dispatch failure: status failure, pending cleared.
- Locate not found or permission failure: failure chat key or timeout reports failure; Teleport
  re-enabled.
- Locate success without a usable `/tp` click event: failure status; no teleport.
- Player disconnect: existing command runner reports no player; vanilla handles screen replacement.
- Missing mod entry after restart is impossible because session state resets; missing entry within a
  session (registry reload) clears the picker selection.
- Unknown command failures remain visible in chat, as with existing tools.

## 8. Testing

### Automated

New common tests:

- `RegistryGroupingTest`:
  - groups multiple namespaces,
  - preserves `minecraft`,
  - drops empty groups,
  - sorts entries by path.
- `ItemCountInputTest`:
  - accepts `"1"`, `"64"`, `"512"`,
  - rejects `""`, `"0"`, `"513"`, `"12a"`, `" 5"`, `"-1"`, `"1.5"`, null.
- `GiveCommandsTest`:
  - exact `give`, `clear`, and `kill @e[type=item]` strings,
  - count range rejection and null item rejection.
- `MobCommandsTest`:
  - exact `summon <id>` and `kill @e[type=!minecraft:player]` strings,
  - null rejection.
- `LocateCommandsTest`:
  - exact biome and structure locate strings,
  - null rejection.
- `CreatorToolSessionTest` additions:
  - new defaults,
  - selection round-trips,
  - item count default/normalization.
- `ToolCatalogTest`:
  - new tools present exactly once,
  - order Camera, World Time, Biomes, Structures, Items, Mobs.

### Manual

- Open each tool; mod dropdown lists Minecraft and installed mods that have entries.
- Selecting a mod populates entries; selecting an entry enables the action.
- Items: count field enables after selection; 1–512 accepted; 0/513 disable Give; Give works.
- Clear Inventory, Clear World Items, Summon, Kill All Entities dispatch correctly.
- Biomes/Structures: Teleport shows Searching, then teleports via locate feedback; not-found and
  timeout paths show failure.
- Close/reopen restores each tool’s mod/entry/count selection.
- `.`, ESC, and X still close the GUI.

## 9. Acceptance Criteria

- Four new navigation buttons appear in the specified order.
- Mod dropdowns group live registry entries by namespace and include Minecraft.
- Entry dropdowns populate from the selected mod and show readable labels.
- Biomes and Structures teleport using `/locate` success feedback and `/tp`.
- Items give 1–512 of the selected item; Clear Inventory and Clear World Items use the exact commands.
- Mobs summon the selected entity type; Kill All Entities uses the exact command.
- Actions disable until required selections are valid.
- New selections persist for the client session after closing/reopening the GUI.
- `./gradlew build` passes; all common tests pass.
- `common` contains no NeoForge, LDLib2, or Fabric imports.
