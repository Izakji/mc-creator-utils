# Registry Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Biomes, Structures, Items, and Mobs tools with dynamic mod/entry dropdowns, teleport/give/summon actions, destructive utility buttons, and session persistence.

**Architecture:** `common` gains grouping, validation, command builders, and locate-feedback parsing. `neoforge` adds a registry catalog, a shared registry picker, four thin panels, and a chat-driven locate/teleport coordinator. Existing `ClientCommandRunner`, `CommandStatusLabel`, and `CreatorToolSession` are reused.

**Tech Stack:** Java 21, Gradle 9, NeoForge 21.1.265, LDLib2 2.2.44, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-27-registry-tools-design.md`

## Global Constraints

- Minecraft `1.21.1`; NeoForge `21.1.265`; LDLib2 `2.2.44`; Java 21.
- `common` must not import `net.neoforged.*`, `com.lowdragmc.*`, or `net.fabricmc.*`.
- Group by registry namespace; include `minecraft`; show mod display names with namespace fallback.
- Mobs: exclude `MobCategory.MISC` and `EntityType.PLAYER`.
- Items: exclude `minecraft:air`.
- Item count: digits only, 1–512, max 3 characters.
- Exact commands:
  - `give @s <id> <count>`
  - `clear`
  - `kill @e[type=item]`
  - `summon <id>`
  - `kill @e[type=!minecraft:player]`
  - `locate biome <id>`
  - `locate structure <id>`
- Locate timeout: 15 seconds. One pending locate at a time. Never cancel the vanilla chat message.
- New selections persist only in `CreatorToolSession`; restart resets.
- No new dependencies.
- Every public type gets Javadoc with `@author Izakji`.
- Commit messages end with `Co-Authored-By: Claude Code <noreply@anthropic.com>`.

## Review Focus

1. A chat message can contain a `/tp` click event in a nested translatable argument. The locate parser must find it, ignore unrelated chat, and reject a success message with no usable `/tp` command.
2. A saved entry can disappear after a registry reload or mismatch. Restoring must clear the selection and keep the action disabled rather than sending a stale ID.
3. Item and mob registries can contain hundreds of entries. The shared selector must scroll and must not crash or freeze on large candidate lists.
4. Item count boundaries: `1`, `512` valid; `0`, `513`, empty, non-digits invalid. The Give button must disable and click-time parsing must block execution.
5. Destructive commands must be exact: `kill @e[type=item]` must not kill players, and `kill @e[type=!minecraft:player]` must exclude every player. `clear` must target only the local player.

---

### Task 1: Tool catalog and session fields

**Files:**
- Modify: `common/src/main/java/com/izakji/mccreatorutils/tool/ToolId.java`
- Modify: `common/src/main/java/com/izakji/mccreatorutils/tool/ToolCatalog.java`
- Modify: `common/src/main/java/com/izakji/mccreatorutils/session/CreatorToolSession.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/tool/ToolCatalogTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/session/CreatorToolSessionTest.java`

**Interfaces:**
- Consumes: existing `ToolDescriptor`, `ToolCatalog`, `CreatorToolSession`.
- Produces:
  - `ToolId.BIOMES`, `STRUCTURES`, `ITEMS`, `MOBS`.
  - `ToolCatalog.BIOMES`, `STRUCTURES`, `ITEMS`, `MOBS`.
  - `CreatorToolSession.biomeSelection()`, `structureSelection()`, `itemSelection()`, `mobSelection()` returning nullable `ResourceLocation`; setters accept null.
  - `CreatorToolSession.itemCountRaw()` default `"1"`; setter normalizes null to `"1"`.

- [ ] **Step 1: Write failing catalog and session tests**

Add to `ToolCatalogTest`:

```java
@Test
void registryToolsAppearInOrder() {
    assertEquals(
            List.of(
                    ToolId.CAMERA,
                    ToolId.WORLD_TIME,
                    ToolId.BIOMES,
                    ToolId.STRUCTURES,
                    ToolId.ITEMS,
                    ToolId.MOBS),
            ToolCatalog.tools().stream().map(ToolDescriptor::id).toList());
}

@Test
void registryToolsRequireNoMod() {
    for (ToolDescriptor tool : List.of(
            ToolCatalog.BIOMES, ToolCatalog.STRUCTURES, ToolCatalog.ITEMS, ToolCatalog.MOBS)) {
        assertFalse(tool.hasRequiredMod());
    }
}
```

Add to `CreatorToolSessionTest`:

```java
@Test
void registryDefaultsAreEmpty() {
    var session = new CreatorToolSession();
    assertNull(session.biomeSelection());
    assertNull(session.structureSelection());
    assertNull(session.itemSelection());
    assertNull(session.mobSelection());
    assertEquals("1", session.itemCountRaw());
}

@Test
void storesRegistrySelections() {
    var session = new CreatorToolSession();
    var id = ResourceLocation.parse("minecraft:plains");
    session.setBiomeSelection(id);
    session.setStructureSelection(id);
    session.setItemSelection(id);
    session.setMobSelection(id);
    session.setItemCountRaw("64");
    assertEquals(id, session.biomeSelection());
    assertEquals(id, session.structureSelection());
    assertEquals(id, session.itemSelection());
    assertEquals(id, session.mobSelection());
    assertEquals("64", session.itemCountRaw());
}

@Test
void nullRegistryValuesNormalize() {
    var session = new CreatorToolSession();
    session.setBiomeSelection(ResourceLocation.parse("minecraft:plains"));
    session.setBiomeSelection(null);
    session.setItemCountRaw(null);
    assertNull(session.biomeSelection());
    assertEquals("1", session.itemCountRaw());
}
```

- [ ] **Step 2: Run tests to verify RED**

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.tool.ToolCatalogTest" --tests "com.izakji.mccreatorutils.session.CreatorToolSessionTest"
```

Expected: compilation failure for missing `ToolId` constants and session methods.

- [ ] **Step 3: Implement catalog and session fields**

Add enum ids `biomes`, `structures`, `items`, `mobs` and descriptors with translation keys
`tool.mccreatorutils.<id>`, required mod id `""`.

Session adds nullable `ResourceLocation` fields, null-accepting selection setters, and
`itemCountRaw = "1"` with a null-normalizing setter.

- [ ] **Step 4: Run tests to verify GREEN**

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.tool.ToolCatalogTest" --tests "com.izakji.mccreatorutils.session.CreatorToolSessionTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/tool common/src/main/java/com/izakji/mccreatorutils/session common/src/test/java/com/izakji/mccreatorutils/tool/ToolCatalogTest.java common/src/test/java/com/izakji/mccreatorutils/session/CreatorToolSessionTest.java
git commit -m "feat: add registry tool ids and session selections" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 2: Registry grouping

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/registry/RegistryGroup.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/registry/RegistryGrouping.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/registry/RegistryGroupingTest.java`

**Interfaces:**
- Consumes: `ResourceLocation`.
- Produces:
  - `record RegistryGroup(String namespace, List<ResourceLocation> entries)`.
  - `static List<RegistryGroup> RegistryGrouping.group(Collection<ResourceLocation> ids)`.
  - Groups sorted `minecraft` first, then namespace; entries sorted by path; empty groups dropped.

- [ ] **Step 1: Write the failing grouping test**

```java
@Test
void groupsAndSortsEntries() {
    var groups = RegistryGrouping.group(List.of(
            ResourceLocation.parse("mod_b:beta"),
            ResourceLocation.parse("minecraft:plains"),
            ResourceLocation.parse("mod_a:zulu"),
            ResourceLocation.parse("minecraft:forest"),
            ResourceLocation.parse("mod_a:alpha")));

    assertEquals(List.of("minecraft", "mod_a", "mod_b"),
            groups.stream().map(RegistryGroup::namespace).toList());
    assertEquals(
            List.of(ResourceLocation.parse("minecraft:forest"),
                    ResourceLocation.parse("minecraft:plains")),
            groups.get(0).entries());
    assertEquals(
            List.of(ResourceLocation.parse("mod_a:alpha"),
                    ResourceLocation.parse("mod_a:zulu")),
            groups.get(1).entries());
}

@Test
void emptyInputYieldsNoGroups() {
    assertTrue(RegistryGrouping.group(List.of()).isEmpty());
}
```

- [ ] **Step 2: Run test to verify RED**

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.registry.RegistryGroupingTest"
```

Expected: compilation failure for missing `RegistryGrouping`.

- [ ] **Step 3: Implement grouping**

Use a `TreeMap<String, List<ResourceLocation>>`; sort keys with `minecraft` forced first, then
natural string order; sort each list by `getPath()`. Return immutable records.

- [ ] **Step 4: Run test to verify GREEN**

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.registry.RegistryGroupingTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/registry common/src/test/java/com/izakji/mccreatorutils/registry
git commit -m "feat: group registry entries by namespace" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 3: Item count and give commands

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/items/ItemCountInput.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/items/GiveCommands.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/items/ItemCountInputTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/items/GiveCommandsTest.java`

**Interfaces:**
- Consumes: `ResourceLocation`.
- Produces:
  - `ItemCountInput.MIN_VALUE = 1`, `MAX_VALUE = 512`, `MAX_DIGITS = 3`;
    `OptionalInt parse(String)`, `boolean isValid(String)`, `boolean isAllowedCharacter(char)`.
  - `GiveCommands.give(ResourceLocation item, int count)`,
    `GiveCommands.clearInventory()`, `GiveCommands.clearWorldItems()`.

- [ ] **Step 1: Write failing tests**

`ItemCountInputTest` covers `1`, `64`, `512` accepted; `""`, `"0"`, `"513"`, `"12a"`, `" 5"`,
`"-1"`, `"1.5"`, null rejected; only ASCII digits pass the char validator.

`GiveCommandsTest`:

```java
@Test
void buildsExactCommands() {
    assertEquals("give @s minecraft:stone 64",
            GiveCommands.give(ResourceLocation.parse("minecraft:stone"), 64));
    assertEquals("clear", GiveCommands.clearInventory());
    assertEquals("kill @e[type=item]", GiveCommands.clearWorldItems());
}

@Test
void rejectsInvalidInputs() {
    assertThrows(NullPointerException.class,
            () -> GiveCommands.give(null, 1));
    assertThrows(IllegalArgumentException.class,
            () -> GiveCommands.give(ResourceLocation.parse("minecraft:stone"), 0));
    assertThrows(IllegalArgumentException.class,
            () -> GiveCommands.give(ResourceLocation.parse("minecraft:stone"), 513));
}
```

- [ ] **Step 2: Run tests to verify RED**

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.items.*"
```

Expected: compilation failure.

- [ ] **Step 3: Implement input and commands**

`ItemCountInput` mirrors existing digit validators with range 1–512 and three characters.
`GiveCommands` requires a non-null item and validates the count range.

- [ ] **Step 4: Run tests to verify GREEN**

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.items.*"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/items common/src/test/java/com/izakji/mccreatorutils/items
git commit -m "feat: add item count and give commands" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 4: Mob commands, locate commands, and locate feedback parsing

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/mobs/MobCommands.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/LocateCommands.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/LocateFeedback.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/mobs/MobCommandsTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/LocateCommandsTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/LocateFeedbackTest.java`

**Interfaces:**
- Consumes: `Component`, `ClickEvent`, `TranslatableContents`, `ResourceLocation`.
- Produces:
  - `MobCommands.summon(ResourceLocation)`, `MobCommands.killAllExceptPlayers()`.
  - `LocateCommands.biome(ResourceLocation)`, `LocateCommands.structure(ResourceLocation)`.
  - `LocateFeedback.containsTranslation(Component message, String key)`.
  - `LocateFeedback.findTeleportCommand(Component message)` returning `Optional<String>`.

- [ ] **Step 1: Write failing command tests**

```java
@Test
void buildsMobCommands() {
    assertEquals("summon minecraft:zombie",
            MobCommands.summon(ResourceLocation.parse("minecraft:zombie")));
    assertEquals("kill @e[type=!minecraft:player]", MobCommands.killAllExceptPlayers());
}
```

`LocateCommandsTest` asserts `locate biome minecraft:plains` and
`locate structure minecraft:village_plains`, plus null rejection.

`LocateFeedbackTest` builds a component shaped like vanilla locate output:

```java
var tp = Component.literal("[10, 64, -20]").withStyle(style -> style.withClickEvent(
        new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s 10 64 -20")));
var message = Component.translatable("commands.locate.biome.success", "minecraft:plains", tp, 42);
assertTrue(LocateFeedback.containsTranslation(message, "commands.locate.biome.success"));
assertEquals(Optional.of("/tp @s 10 64 -20"), LocateFeedback.findTeleportCommand(message));
```

Add cases:
- a sibling component contains the click;
- a nested translated argument contains the click;
- an unrelated message returns false/empty;
- a click value not starting with `/tp @s ` is ignored.

- [ ] **Step 2: Run tests to verify RED**

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.mobs.*" --tests "com.izakji.mccreatorutils.locate.*"
```

Expected: compilation failure.

- [ ] **Step 3: Implement commands and recursive parser**

`LocateFeedback` recursively checks `getStyle().getClickEvent()`, `getSiblings()`, and
`TranslatableContents.getArgs()`. Accept `SUGGEST_COMMAND` and `RUN_COMMAND` values whose
trimmed value starts with `/tp @s `; `findTeleportCommand` returns the value as-is.

- [ ] **Step 4: Run tests to verify GREEN**

```bash
./gradlew :common:test --tests "com.izakji.mccreatorutils.mobs.*" --tests "com.izakji.mccreatorutils.locate.*"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/mobs common/src/main/java/com/izakji/mccreatorutils/locate common/src/test/java/com/izakji/mccreatorutils/mobs common/src/test/java/com/izakji/mccreatorutils/locate
git commit -m "feat: add mob commands and locate feedback parser" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 5: NeoForge registry catalog

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/registry/RegistryKind.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/registry/RegistryCatalog.java`

**Interfaces:**
- Consumes: `RegistryGroup`, `RegistryGrouping`, `ResourceLocation`, `Component`, `BuiltInRegistries`, `RegistryAccess`, `ModList`.
- Produces:
  - `enum RegistryKind { BIOME, STRUCTURE, ITEM, MOB }`.
  - `RegistryCatalog.groups(RegistryKind kind) -> List<RegistryGroup>`.
  - `RegistryCatalog.label(RegistryKind kind, ResourceLocation id) -> Component`.
  - `RegistryCatalog.modName(String namespace) -> Component`.

- [ ] **Step 1: Implement registry sources**

`RegistryCatalog`:
- Items: `BuiltInRegistries.ITEM.keySet()`, remove `minecraft:air`.
- Mobs: `BuiltInRegistries.ENTITY_TYPE.keySet()`, keep `EntityType.getCategory() != MobCategory.MISC`,
  remove `EntityType.PLAYER`.
- Biomes/structures: `Minecraft.getInstance().level.registryAccess().registryOrThrow(...).keySet()`;
  return empty when `level` is null.
- Group through `RegistryGrouping.group`.
- Sort groups by display-name string with `minecraft` first.
- Mod name:
  - `minecraft` -> “Minecraft”.
  - `ModList.get().getModContainerById(namespace).map(c -> Component.literal(c.getModInfo().getDisplayName()))`.
  - fallback `Component.literal(namespace)`.

`RegistryKind` label:
- Item -> `BuiltInRegistries.ITEM.get(id).getDescription()`.
- Mob -> `BuiltInRegistries.ENTITY_TYPE.get(id).getDescription()`.
- Biome -> `Component.translatableWithFallback("biome." + namespace + "." + path, prettyPath(path))`.
- Structure -> `Component.literal(prettyPath(path))`.
- `prettyPath`: underscores to spaces, title case each word.

- [ ] **Step 2: Compile**

```bash
./gradlew :neoforge:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client/registry
git commit -m "feat: add client registry catalog" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 6: Shared registry picker and status helpers

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/registry/RegistryPicker.java`
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/client/CommandStatusLabel.java`

**Interfaces:**
- Consumes: `RegistryKind`, `RegistryCatalog`, `RegistryGroup`, `Selector`, `UIElementProvider`, `CreatorToolSession`.
- Produces:
  - `RegistryPicker(RegistryKind kind, ResourceLocation initialSelection, Consumer<ResourceLocation> onSelectionChanged)`.
  - `ResourceLocation selectedEntry()`.
  - `CommandStatusLabel.showInfo(Component)`, `showResult(CommandResult)`, `showError(Component)`.

- [ ] **Step 1: Extend `CommandStatusLabel`**

Add:
- `showInfo(Component message)` — yellow text.
- `showResult(CommandResult result)` — green success / red failure.
- `showError(Component message)` — red text.
- Keep `runCommand(String)` delegating to `showResult(ClientCommandRunner.execute(command))`.

- [ ] **Step 2: Implement `RegistryPicker`**

Constructor builds:
- Mod `Selector<RegistryGroup>` with candidates from `RegistryCatalog.groups(kind)`; display
  `RegistryCatalog.modName(group.namespace())`; null preview `tool.mccreatorutils.registry.mod.select`.
- Entry `Selector<ResourceLocation>`; disabled and empty initially.
- On mod change: clear entry, populate entry candidates, enable the entry selector, notify callback
  with null.
- On entry change: notify callback with the selected id.
- Restore `initialSelection` before callbacks are registered: find its group, select the group,
  populate entries, select the entry.
- `selectedEntry()` reads the entry selector value.

- [ ] **Step 3: Compile**

```bash
./gradlew :neoforge:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client/registry/RegistryPicker.java neoforge/src/main/java/com/izakji/mccreatorutils/client/CommandStatusLabel.java
git commit -m "feat: add shared registry picker" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 7: Locate teleporter and biome/structure panels

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/ClientLocateTeleporter.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/BiomesToolPanel.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/StructuresToolPanel.java`

**Interfaces:**
- Consumes: `LocateFeedback`, `LocateCommands`, `ClientCommandRunner`, `CommandStatusLabel`, `RegistryPicker`.
- Produces:
  - `ClientLocateTeleporter.requestBiome(ResourceLocation, Component, Consumer<CommandResult>)`.
  - `ClientLocateTeleporter.requestStructure(ResourceLocation, Component, Consumer<CommandResult>)`.
  - `BiomesToolPanel()` and `StructuresToolPanel()` UI elements.

- [ ] **Step 1: Implement `ClientLocateTeleporter`**

Use `@EventBusSubscriber(modid = McCreatorUtils.MOD_ID, value = Dist.CLIENT)`.
- Pending record: target name, success key, failure key, expiresAt, callback.
- `requestBiome`/`requestStructure` reject when a request is pending with a failure callback;
  otherwise set pending, send `LocateCommands...`, and on dispatch failure clear pending and callback.
- `@SubscribeEvent onChat(ClientChatReceivedEvent)`:
  - Ignore when no pending or expired.
  - If message contains the success key, find the `/tp @s` command with `LocateFeedback`.
    Strip the leading slash and execute it through `ClientCommandRunner`; callback success with
    `status.mccreatorutils.teleported` and target name, or failure if absent.
  - If message contains the failure key, callback failure with
    `status.mccreatorutils.locate_failed` and target name.
  - Never cancel the event.
- `@SubscribeEvent onClientTick(ClientTickEvent.Post)` expires pending after 15 seconds and
  calls the failure callback with `status.mccreatorutils.locate_failed`.

- [ ] **Step 2: Implement biome and structure panels**

Both panels:
- Column layout, description label, `RegistryPicker`, **Teleport** button, status.
- Restore session selection into the picker; write entry changes to session.
- Initialize the Teleport button state from `picker.selectedEntry()` after construction, then keep it
  synchronized through the picker callback. Teleport starts disabled if no selection.
- On click: set status searching via `showInfo`, disable the button, call
  `ClientLocateTeleporter.request...`; callback re-enables the button and calls
  `status.showResult` (success/failure). The success text is generated by the teleporter.
- Frame/panel layout values: width/height `100%`, gap `5`; button height `16`, width `100%`.

- [ ] **Step 3: Compile**

```bash
./gradlew :neoforge:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client/ClientLocateTeleporter.java neoforge/src/main/java/com/izakji/mccreatorutils/client/BiomesToolPanel.java neoforge/src/main/java/com/izakji/mccreatorutils/client/StructuresToolPanel.java
git commit -m "feat: add locate teleporter and biome panels" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 8: Items and mobs panels

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/ItemsToolPanel.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/client/MobsToolPanel.java`

**Interfaces:**
- Consumes: `RegistryPicker`, `ItemCountInput`, `GiveCommands`, `MobCommands`, `CommandStatusLabel`, `CreatorToolSession`.
- Produces: `ItemsToolPanel()` and `MobsToolPanel()` UI elements.

- [ ] **Step 1: Implement `ItemsToolPanel`**

Layout:
- Description label.
- `RegistryPicker(RegistryKind.ITEM, session.itemSelection(), session::setItemSelection)`.
- Count row: `TextField` with `ItemCountInput` char/text validators, placeholder `1-512`, width flex 1.
  - Disabled until an item is selected.
  - Restore `session.itemCountRaw()`; persist valid text via responder and raw text each tick.
- Initialize the count field and Give button from `picker.selectedEntry()` after construction.
- **Give** button, disabled until item selected and count valid; click sends
  `GiveCommands.give(item, parsedCount)`.
- Row with **Clear Inventory** and **Clear World Items**; always enabled.
- Status label.

- [ ] **Step 2: Implement `MobsToolPanel`**

Layout:
- Description label.
- `RegistryPicker(RegistryKind.MOB, session.mobSelection(), session::setMobSelection)`.
- Initialize the Summon button state from `picker.selectedEntry()` after construction, then keep it
  synchronized through the picker callback.
- **Summon** button, disabled until selection; click sends `MobCommands.summon(id)`.
- **Kill All Entities** button, always enabled; sends `MobCommands.killAllExceptPlayers()`.
- Status label.

- [ ] **Step 3: Compile**

```bash
./gradlew :neoforge:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/client/ItemsToolPanel.java neoforge/src/main/java/com/izakji/mccreatorutils/client/MobsToolPanel.java
git commit -m "feat: add items and mobs panels" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

---

### Task 9: UI wiring, translations, and acceptance verification

**Files:**
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/client/CreatorToolsUi.java`
- Modify: `neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json`
- Modify only if verification finds defects.

**Interfaces:**
- Consumes: the four panels and existing `ToolId` cases.
- Produces: complete navigation and localized UI.

- [ ] **Step 1: Wire the four panels into `CreatorToolsUi.selectTool`**

Add cases for `BIOMES`, `STRUCTURES`, `ITEMS`, `MOBS` creating the corresponding panel.

- [ ] **Step 2: Add translations**

Add keys for:
- Tool names and descriptions: `tool.mccreatorutils.biomes`, `.structures`, `.items`, `.mobs` and
  `.description` variants.
- Shared picker: `tool.mccreatorutils.registry.mod`, `.mod.select`, `.entry`, `.entry.select`,
  `.minecraft`.
- Actions: `.biomes.teleport`, `.structures.teleport`, `.items.count`, `.items.count.placeholder`,
  `.items.give`, `.items.clear_inventory`, `.items.clear_world_items`, `.mobs.summon`,
  `.mobs.kill_all`.
- Status: `status.mccreatorutils.searching`, `.teleported`, `.locate_failed`, `.busy`.

- [ ] **Step 3: Format and run the full build**

```bash
./gradlew spotlessApply clean build
```

Expected: BUILD SUCCESSFUL; all common tests pass.

- [ ] **Step 4: Verify loader isolation**

```bash
grep -R -n -E "net\.neoforged|com\.lowdragmc|net\.fabricmc" common/src/main/java common/src/test/java
```

Expected: no output.

- [ ] **Step 5: Launch the client and verify manually**

```bash
./gradlew :neoforge:runClient
```

Check:
- Six navigation buttons appear in order; selectors scroll with large item/mob lists.
- Mod dropdown includes Minecraft and installed mods; entry dropdown populates.
- Items: Give 1/512; 0/513 disables; Give works; Clear Inventory and Clear World Items work.
- Mobs: Summon works; Kill All Entities excludes the player.
- Biomes/Structures: Teleport shows Searching; success teleports; not-found/timeout shows failure.
- Close/reopen restores all four tool selections.
- `.`, ESC, and X still close the GUI.

- [ ] **Step 6: Commit verification fixes**

If files changed:

```bash
git add -A
git commit -m "fix: address registry tools verification findings" -m "Co-Authored-By: Claude Code <noreply@anthropic.com>"
```

If no files changed, report the clean tree; do not create an empty commit.
