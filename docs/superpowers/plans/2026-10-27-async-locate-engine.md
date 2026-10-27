# Async Locate Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an asynchronous, modded-aware locate engine that replaces player `/locate biome` and `/locate structure` searches while returning vanilla-compatible results.

**Architecture:** Keep the search and envelope logic loader-neutral in `common`, with a bounded request service. NeoForge supplies the command mixin, structure-check synchronization, config, lifecycle, TerraBlender adapter, and world access. Biome sampling and structure candidate generation run off the server tick; structure checks run on one serialized lane.

**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge 21.1.265, ModDevGradle, Mixin, TerraBlender compileOnly, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-27-async-locate-engine-design.md`

## Global Constraints

- Minecraft 1.21.1 and NeoForge 21.1.265 only.
- `common` must not import NeoForge APIs.
- No compile or runtime dependency on Async Locator Refined or BiomeSpy.
- TerraBlender is `compileOnly` and guarded by `ModList.isLoaded("terrablender")`.
- Terralith and Lithostitched require no compile dependency.
- Do not copy BiomeSpy or Async Locator source.
- Preserve vanilla search order and search bounds. Do not use BiomeSpy's doubled biome radius.
- Player-issued `/locate` only; console and command blocks stay vanilla.
- No new client/server packet protocol.
- Defaults: biome threads `2`, queued searches `32`, timeout `60` seconds, structure prefilter on, margin `1` chunk, result cache `30` seconds, debug stats off.
- Use `./gradlew.bat :common:test` and `./gradlew.bat build` for verification.
- Commit after every task.

## Review Focus

- A modded biome source that does not expose the target in its parameter list must not produce a false not-found. Test: Task 2 and Task 3.
- A structure whose biome-check position is outside the candidate chunk must not be skipped by the prefilter. Test: Task 4.
- Structure checks running beside normal chunk generation must not corrupt `StructureCheck` state or deadlock. Verify: Task 9 and Task 11.
- Queue limits, duplicate requests, timeouts, cancellation, and shutdown must bound resource use. Test: Task 6.
- TerraBlender absent or not initialized must not load TerraBlender classes or remove the base fast path. Verify: Task 8 and Task 11.

---

### Task 1: Common Locate Contracts

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/LocateType.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/LocateFailure.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/LocateResult.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/LocateRequestKey.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/CancellationToken.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/LocateTask.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/LocateSettings.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/LocateStats.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/engine/LocateRequestKeyTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/engine/LocateResultTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/engine/LocateSettingsTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `enum LocateType { BIOME, STRUCTURE }`
  - `enum LocateFailure { QUEUE_FULL, TIMEOUT, CANCELLED, NOT_FOUND, ERROR }`
  - `record LocateResult(BlockPos position, Holder<?> holder, LocateFailure failure, long elapsedMillis)`
    with `found(BlockPos, Holder<?>, long)`, `notFound(long)`, `failed(LocateFailure, long)`, and
    `boolean success()`.
  - `record LocateRequestKey(ResourceKey<Level> dimension, LocateType type, List<ResourceLocation> targets, BlockPos origin, int radius, boolean skipKnownStructures)`
    with target ids distinct, sorted, and immutable, and an immutable origin.
  - `interface CancellationToken { boolean isCancelled(); default void throwIfCancelled(); }`
  - `@FunctionalInterface interface LocateTask { LocateResult run(CancellationToken token) throws Exception; }`
  - `record LocateSettings(boolean enabled, int biomeThreads, int maxQueuedSearches, int timeoutSeconds, boolean structurePrefilter, int structurePrefilterMarginChunks, int resultCacheSeconds, boolean debugStats)`
    with `static LocateSettings defaults()`.
  - `final class LocateStats` with mutable counters for columns, envelope rejects, exact biome
    lookups, candidate chunks, prefilter skips, presence checks, and chunk loads, plus
    `String summary()` for debug logging.

- [ ] **Step 1: Write the failing tests**

```java
class LocateRequestKeyTest {
    @Test
    void canonicalizesTargetsAndCopiesOrigin() {
        var origin = new BlockPos(1, 2, 3);
        var key = new LocateRequestKey(
                Level.OVERWORLD, LocateType.BIOME,
                List.of(ResourceLocation.parse("minecraft:plains"),
                        ResourceLocation.parse("minecraft:forest"),
                        ResourceLocation.parse("minecraft:plains")),
                origin, 6400, false);
        assertEquals(List.of(ResourceLocation.parse("minecraft:forest"),
                ResourceLocation.parse("minecraft:plains")), key.targets());
        assertThrows(UnsupportedOperationException.class,
                () -> key.targets().add(ResourceLocation.parse("minecraft:desert")));
    }
}

class LocateResultTest {
    @Test
    void reportsSuccessAndFailures() {
        var holder = Holder.<String>direct("plains");
        var found = LocateResult.found(BlockPos.ZERO, holder, 12);
        assertTrue(found.success());
        assertEquals(BlockPos.ZERO, found.position());
        assertEquals(holder, found.holder());
        assertEquals(12, found.elapsedMillis());

        assertFalse(LocateResult.notFound(4).success());
        assertEquals(LocateFailure.NOT_FOUND, LocateResult.notFound(4).failure());
        assertFalse(LocateResult.failed(LocateFailure.TIMEOUT, 9).success());
    }
}

class LocateSettingsTest {
    @Test
    void defaultsMatchDesign() {
        var settings = LocateSettings.defaults();
        assertTrue(settings.enabled());
        assertEquals(2, settings.biomeThreads());
        assertEquals(32, settings.maxQueuedSearches());
        assertEquals(60, settings.timeoutSeconds());
        assertTrue(settings.structurePrefilter());
        assertEquals(1, settings.structurePrefilterMarginChunks());
        assertEquals(30, settings.resultCacheSeconds());
        assertFalse(settings.debugStats());
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew.bat :common:test --tests "com.izakji.mccreatorutils.locate.engine.*"`

Expected: compilation failure because the new types do not exist.

- [ ] **Step 3: Implement the contracts**

- `LocateRequestKey` compact constructor: `origin = origin.immutable()`, `targets = targets.stream().distinct().sorted().toList()`.
- `LocateResult.success()`: `position != null && failure == null`.
- `CancellationToken.throwIfCancelled()` throws `java.util.concurrent.CancellationException`.
- `LocateSettings.defaults()` returns the exact values in the test.
- `LocateStats` uses plain `long` counters; a request has one writer thread, so atomics are not
  required.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew.bat :common:test --tests "com.izakji.mccreatorutils.locate.engine.*"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/locate/engine \
        common/src/test/java/com/izakji/mccreatorutils/locate/engine
git commit -m "feat: add async locate contracts"
```

---

### Task 2: Climate Envelope Index

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/ClimateAxis.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/BiomeEnvelope.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/BiomeEnvelopeIndex.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/BiomeEnvelopeProvider.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/ParameterListEnvelopeProvider.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/engine/BiomeEnvelopeTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/engine/BiomeEnvelopeIndexTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/engine/TestClimate.java`

**Interfaces:**
- Consumes: `ClimateAxis` from this task.
- Produces:
  - `enum ClimateAxis { TEMPERATURE, HUMIDITY, CONTINENTALNESS, EROSION, WEIRDNESS }`
  - `final class BiomeEnvelope`:
    - `static BiomeEnvelope impossible()`
    - `static BiomeEnvelope of(Collection<Climate.ParameterPoint> points)`
    - `static BiomeEnvelope union(BiomeEnvelope first, BiomeEnvelope second)`
    - `boolean isImpossible()`
    - `boolean contains(ClimateAxis axis, long value)`
    - `boolean contains(long temperature, long humidity, long continentalness, long erosion, long weirdness)`
    - `List<ClimateAxis> selectivityOrder()`
  - `final class BiomeEnvelopeIndex<T>`:
    - `static <T> BiomeEnvelopeIndex<T> from(Climate.ParameterList<Holder<T>> parameters)`
    - `static <T> BiomeEnvelopeIndex<T> fromValues(List<Pair<Climate.ParameterPoint, Holder<T>>> values)`
    - `Optional<BiomeEnvelope> envelopeFor(Holder<T> target)`
    - `Optional<BiomeEnvelope> unionFor(Set<Holder<T>> targets)`
  - `interface BiomeEnvelopeProvider<T>`:
    - `EnvelopeLookup<T> forTargets(Set<Holder<T>> targets)`
    - nested `interface EnvelopeLookup<T> { Optional<BiomeEnvelope> at(int quartX, int quartZ); }`
  - `final class ParameterListEnvelopeProvider<T> implements BiomeEnvelopeProvider<T>`:
    - constructor `ParameterListEnvelopeProvider(BiomeEnvelopeIndex<T> index)`
  - A provider that returns an empty lookup when any target is unrepresented. The empty lookup
    means "do not prune"; it does not mean "impossible".

- [ ] **Step 1: Write the failing tests**

```java
class BiomeEnvelopeTest {
    @Test
    void boundsAndFixedAxesAreValid() {
        var envelope = BiomeEnvelope.of(List.of(
                point(0, 100, 0, 100, 0, 100, 0, 100, 0, 100),
                point(20, 30, 40, 50, 60, 70, 80, 90, 100, 200)));
        assertTrue(envelope.contains(20, 40, 60, 80, 100));
        assertFalse(envelope.contains(201, 100, 100, 100, 100));
        assertFalse(envelope.isImpossible());
        assertEquals(ClimateAxis.TEMPERATURE, envelope.selectivityOrder().get(0));
    }

    @Test
    void emptyEnvelopeIsImpossibleAndUnionExpands() {
        assertTrue(BiomeEnvelope.of(List.of()).isImpossible());
        var union = BiomeEnvelope.union(
                BiomeEnvelope.of(List.of(point(0, 10, 0, 10, 0, 10, 0, 10, 0, 10))),
                BiomeEnvelope.of(List.of(point(20, 30, 20, 30, 20, 30, 20, 30, 20, 30))));
        assertTrue(union.contains(5, 5, 5, 5, 5));
        assertTrue(union.contains(25, 25, 25, 25, 25));
        assertTrue(union.contains(15, 15, 15, 15, 15));
        assertFalse(union.contains(31, 5, 5, 5, 5));
    }
}

class BiomeEnvelopeIndexTest {
    @Test
    void groupsTargetsAndDisablesPruningForMissingTargets() {
        var plains = Holder.<String>direct("plains");
        var forest = Holder.<String>direct("forest");
        var parameters = new Climate.ParameterList<>(List.of(
                Pair.of(point(0, 100, 0, 100, 0, 100, 0, 100, 0, 100), plains),
                Pair.of(point(0, 100, 0, 100, 0, 100, 0, 100, 0, 100), forest)));
        var provider = new ParameterListEnvelopeProvider<>(BiomeEnvelopeIndex.from(parameters));
        assertTrue(provider.forTargets(Set.of(plains)).at(0, 0).isPresent());
        assertTrue(provider.forTargets(Set.of(plains, Holder.<String>direct("missing")))
                .at(0, 0).isEmpty());
    }
}
```

Test helper `TestClimate.point(t0,t1,h0,h1,c0,c1,e0,e1,w0,w1)` creates a `Climate.ParameterPoint`
with six `new Climate.Parameter(min, max)` values, a fixed depth interval `-100..100`, and `0L`
offset. `TestClimate.envelope(min,max)` creates an envelope accepting `min..max` on all five axes.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew.bat :common:test --tests "*BiomeEnvelope*"`

Expected: compilation failure because the envelope types do not exist.

- [ ] **Step 3: Implement the envelope and index**

- `BiomeEnvelope.of` returns `impossible()` when the collection is empty.
- Fixed-axis intervals remain valid; only an empty point collection is impossible.
- `union` returns the other envelope when one side is impossible.
- `selectivityOrder` sorts axes by `(max - min)` ascending, then enum order; fixed axes come first.
- `BiomeEnvelopeIndex` uses `HashMap<Holder<T>, BiomeEnvelope>`.
- `unionFor` returns `Optional.empty()` if any target is missing; otherwise unions the boxes.
- `ParameterListEnvelopeProvider.forTargets` returns an `EnvelopeLookup` that returns the union
  envelope at every quart position, or an empty `Optional` when the index cannot represent the
  target set.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew.bat :common:test --tests "*BiomeEnvelope*"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/locate/engine \
        common/src/test/java/com/izakji/mccreatorutils/locate/engine
git commit -m "feat: add climate envelope indexing"
```

---

### Task 3: Biome Search

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/ClimateSampler.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/BiomeSearch.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/engine/BiomeSearchTest.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/engine/TestClimateSampler.java`

**Interfaces:**
- Consumes: `BiomeEnvelope`, `BiomeEnvelopeProvider`, `CancellationToken`, `ClimateAxis`.
- Produces:
  - `interface ClimateSampler<T>`:
    - `Column column(int blockX, int blockZ)`
    - `Holder<T> resolve(Climate.TargetPoint point, int blockX, int blockY, int blockZ)`
    - nested `interface Column { long value(ClimateAxis axis); long depth(int blockY); }`
  - `record BiomeSearchInput<T>(BlockPos origin, int radius, int horizontalStep, int verticalStep, int minBuildHeight, int maxBuildHeight, Set<Holder<T>> targets, BiomeEnvelopeProvider.EnvelopeLookup<T> envelopes, ClimateSampler<T> sampler, LocateStats stats, CancellationToken cancellationToken)`
  - `record BiomeHit<T>(BlockPos position, Holder<T> biome)`
  - `final class BiomeSearch`:
    - `static <T> Optional<BiomeHit<T>> findClosest(BiomeSearchInput<T> input)`

- [ ] **Step 1: Write the failing tests**

```java
class BiomeSearchTest {
    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;

    @Test
    void findsFirstTargetAndSamplesEachAxisOncePerColumn() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.putValues(0, 0, 100, 100, 100, 100, 100);
        sampler.putHit(0, 64, 0, target);
        var input = input(Set.of(target), enclosingEnvelope(), sampler);
        var hit = BiomeSearch.findClosest(input).orElseThrow();
        assertEquals(new BlockPos(0, 64, 0), hit.position());
        assertEquals(target, hit.biome());
        assertTrue(sampler.duplicateAxisSamples() == 0);
    }

    @Test
    void envelopeRejectsColumnBeforeResolve() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.putValues(0, 0, 1000, 1000, 1000, 1000, 1000);
        sampler.putHit(0, 64, 0, target);
        var input = input(Set.of(target),
                targets -> (qx, qz) -> Optional.of(
                        BiomeEnvelope.of(List.of(point(0, 100, 0, 100, 0, 100, 0, 100, 0, 100)))),
                sampler);
        assertTrue(BiomeSearch.findClosest(input).isEmpty());
        assertEquals(0, sampler.resolveCalls());
    }

    @Test
    void noEnvelopeFallsBackToExactSearch() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.putValues(0, 0, 100, 100, 100, 100, 100);
        sampler.putHit(0, 64, 0, target);
        var input = input(Set.of(target), targets -> (qx, qz) -> Optional.empty(), sampler);
        assertEquals(target, BiomeSearch.findClosest(input).orElseThrow().biome());
    }

    @Test
    void emptyTargetsReturnWithoutSampling() {
        var sampler = new TestClimateSampler();
        var input = input(Set.of(), targets -> (qx, qz) -> Optional.empty(), sampler);
        assertTrue(BiomeSearch.findClosest(input).isEmpty());
        assertEquals(0, sampler.columnCalls());
    }

    @Test
    void cancellationStopsSearch() {
        var sampler = new TestClimateSampler();
        var input = input(Set.of(Holder.direct("target")),
                targets -> (qx, qz) -> Optional.empty(), sampler, () -> true);
        assertThrows(CancellationException.class, () -> BiomeSearch.findClosest(input));
    }
}
```

`TestClimateSampler` stores five axis values per `(x,z)`, returns zero depth, stores a holder per
`BlockPos`, and counts `column()` calls, `resolve()` calls, and repeated axis calls. Unset columns
default to five values of `100`; unset resolve positions return a non-target holder. The test
`input` helper passes a fresh `LocateStats` into every `BiomeSearchInput`; it uses
`ORIGIN = (0,64,0)`, radius `64`, horizontal step `32`, vertical step `32`, minimum Y `-64`, and
maximum Y `320`. `enclosingEnvelope()` returns a lookup whose envelope accepts the five values
`0..200`. The `input` helper has a second overload that accepts a `CancellationToken`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew.bat :common:test --tests "*BiomeSearchTest"`

Expected: compilation failure because `BiomeSearch` and `ClimateSampler` do not exist.

- [ ] **Step 3: Implement `BiomeSearch`**

- Validate constants: `i = Math.floorDiv(radius, horizontalStep)`; do not double the radius.
- Build `heights` with `Mth.outFromOrigin(origin.getY(), minBuildHeight + 1, maxBuildHeight, verticalStep)`.
- Iterate `BlockPos.spiralAround(BlockPos.ZERO, i, Direction.EAST, Direction.SOUTH)`.
- Convert block X/Z to quart X/Z with `QuartPos.fromBlock`.
- Call `cancellationToken.throwIfCancelled()` at each ring/column boundary.
- If the envelope lookup returns an `Optional.empty`, do not prune. If it returns an impossible
  envelope, skip the column. If present, evaluate the five axes in `selectivityOrder()` and stop
  on the first outside value.
- Cache axis values inside the `Column` implementation. Compute depth only after the envelope
  passes.
- Increment `stats.columns` once per visited column, `stats.envelopeRejects` when an envelope
  skips a column, and `stats.exactBiomeLookups` for each `resolve` call.
- Build `Climate.TargetPoint` from the five values and depth in the target-point field order.
- Resolve with `sampler.resolve(point, x, y, z)`, compare against `targets`, and return the first
  hit.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew.bat :common:test --tests "*BiomeSearchTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/locate/engine \
        common/src/test/java/com/izakji/mccreatorutils/locate/engine
git commit -m "feat: implement climate-pruned biome search"
```

---

### Task 4: Structure Biome Prefilter

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/StructureBiomeFilter.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/StructurePrefilter.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/engine/StructurePrefilterTest.java`

**Interfaces:**
- Consumes: `BiomeEnvelopeProvider`, `ClimateSampler`, `ClimateAxis`.
- Produces:
  - `@FunctionalInterface interface StructureBiomeFilter { boolean mayMatch(Holder<Structure> structure, ChunkPos chunk); }`
  - `final class StructurePrefilter<T> implements StructureBiomeFilter`:
    - constructor `StructurePrefilter(BiomeEnvelopeProvider<T> provider, ClimateSampler<T> sampler, Function<Holder<Structure>, Set<Holder<T>>> biomes, int marginChunks)`
    - `static <T> StructureBiomeFilter disabled()`

- [ ] **Step 1: Write the failing tests**

```java
class StructurePrefilterTest {
    @Test
    void skipsChunkWhenEverySampleIsOutsideEnvelope() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.fillValues(1000);
        var filter = new StructurePrefilter<>(
                targets -> (qx, qz) -> Optional.of(envelope(0, 100)),
                sampler, structure -> Set.of(target), 0);
        assertFalse(filter.mayMatch(Holder.<Structure>direct(null), new ChunkPos(0, 0)));
    }

    @Test
    void marginFindsBiomeInAdjacentChunk() {
        var target = Holder.<String>direct("target");
        var sampler = new TestClimateSampler();
        sampler.fillValues(1000);
        sampler.putValues(-16, 0, 50, 50, 50, 50, 50);
        var noMargin = new StructurePrefilter<>(
                targets -> (qx, qz) -> Optional.of(envelope(0, 100)),
                sampler, structure -> Set.of(target), 0);
        var withMargin = new StructurePrefilter<>(
                targets -> (qx, qz) -> Optional.of(envelope(0, 100)),
                sampler, structure -> Set.of(target), 1);
        assertFalse(noMargin.mayMatch(Holder.<Structure>direct(null), new ChunkPos(0, 0)));
        assertTrue(withMargin.mayMatch(Holder.<Structure>direct(null), new ChunkPos(0, 0)));
    }

    @Test
    void missingEnvelopeOrBiomesDisablesFiltering() {
        var sampler = new TestClimateSampler();
        sampler.fillValues(1000);
        var noEnvelope = new StructurePrefilter<String>(
                targets -> (qx, qz) -> Optional.empty(), sampler, structure -> Set.of(), 0);
        assertTrue(noEnvelope.mayMatch(Holder.<Structure>direct(null), new ChunkPos(0, 0)));
        assertTrue(StructurePrefilter.disabled().mayMatch(
                Holder.<Structure>direct(null), new ChunkPos(0, 0)));
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew.bat :common:test --tests "*StructurePrefilterTest"`

Expected: compilation failure because the filter types do not exist.

- [ ] **Step 3: Implement the prefilter**

- Cache one `EnvelopeLookup<T>` per `Holder<Structure>` in a `HashMap`.
- Return `true` when the lookup is empty or the structure's biome set is empty.
- Sample every quart X/Z position from
  `QuartPos.fromBlock(chunk.getMinBlockX() - marginChunks * 16)` through
  `QuartPos.fromBlock(chunk.getMaxBlockX() + marginChunks * 16)`, inclusive, and the matching Z
  range. The default margin `1` therefore produces a 12 by 12 quart grid, 144 samples.
- For each sample, get the envelope at that quart position. An absent envelope returns `true`; an
  impossible envelope contributes no match. If every sampled position is outside the envelope,
  return `false`.
- Use `ClimateSampler.column(x, z)` and the five envelope axes. Do not sample depth.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew.bat :common:test --tests "*StructurePrefilterTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/locate/engine \
        common/src/test/java/com/izakji/mccreatorutils/locate/engine
git commit -m "feat: add structure biome prefilter"
```

---

### Task 5: Structure Search Ordering

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/StructureProbe.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/StructurePlacementGroup.java`
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/StructureSearch.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/engine/StructureSearchTest.java`

**Interfaces:**
- Consumes: `CancellationToken`, `StructureBiomeFilter` indirectly through the production probe.
- Produces:
  - `record StructureHit(BlockPos position, Holder<Structure> structure)`
  - `@FunctionalInterface interface StructureProbe { Optional<StructureHit> probe(Set<Holder<Structure>> structures, ChunkPos chunk, StructurePlacement placement, boolean skipKnownStructures); }`
  - `record StructurePlacementGroup(StructurePlacement placement, Set<Holder<Structure>> structures)`
  - `record StructureSearchInput(BlockPos origin, int searchRadius, long seed, boolean skipKnownStructures, List<StructurePlacementGroup> placements, Map<ConcentricRingsStructurePlacement, List<ChunkPos>> concentricPositions, StructureProbe probe, LocateStats stats, CancellationToken cancellationToken)`
  - `final class StructureSearch { static Optional<StructureHit> findNearest(StructureSearchInput input); }`

- [ ] **Step 1: Write the failing tests**

```java
class StructureSearchTest {
    @Test
    void visitsRandomSpreadCandidatesInVanillaRingOrder() {
        var placement = new RandomSpreadStructurePlacement(4, 1, RandomSpreadType.LINEAR, 7);
        var structure = Holder.<Structure>direct(null);
        var observed = new ArrayList<ChunkPos>();
        var input = new StructureSearchInput(
                BlockPos.ZERO, 1, 42L, false,
                List.of(new StructurePlacementGroup(placement, Set.of(structure))),
                Map.of(),
                (structures, chunk, p, skip) -> { observed.add(chunk); return Optional.empty(); },
                new LocateStats(), () -> false);
        assertTrue(StructureSearch.findNearest(input).isEmpty());
        assertEquals(expectedRandomSpreadChunks(placement, 42L, 1), observed);
    }

    @Test
    void returnsFirstHitFromFirstSuccessfulRing() {
        var placement = new RandomSpreadStructurePlacement(4, 1, RandomSpreadType.LINEAR, 7);
        var structure = Holder.<Structure>direct(null);
        var calls = new AtomicInteger();
        var hitChunk = placement.getPotentialStructureChunk(42L, 0, 0);
        var input = new StructureSearchInput(
                BlockPos.ZERO, 100, 42L, false,
                List.of(new StructurePlacementGroup(placement, Set.of(structure))),
                Map.of(),
                (structures, chunk, p, skip) -> {
                    calls.incrementAndGet();
                    return chunk.equals(hitChunk)
                            ? Optional.of(new StructureHit(BlockPos.ZERO, structure))
                            : Optional.empty();
                },
                new LocateStats(), () -> false);
        assertEquals(structure, StructureSearch.findNearest(input).orElseThrow().structure());
        assertEquals(1, calls.get());
    }

    @Test
    void cancellationStopsBeforeProbe() {
        var placement = new RandomSpreadStructurePlacement(4, 1, RandomSpreadType.LINEAR, 7);
        var structure = Holder.<Structure>direct(null);
        var input = new StructureSearchInput(
                BlockPos.ZERO, 100, 42L, false,
                List.of(new StructurePlacementGroup(placement, Set.of(structure))),
                Map.of(), (structures, chunk, p, skip) -> Optional.empty(),
                new LocateStats(), () -> true);
        assertThrows(CancellationException.class, () -> StructureSearch.findNearest(input));
    }

    @Test
    void probesConcentricRingPositions() {
        var placement = new ConcentricRingsStructurePlacement(1, 1, 1, HolderSet.direct());
        var structure = Holder.<Structure>direct(null);
        var ring = new ChunkPos(4, 4);
        var input = new StructureSearchInput(
                BlockPos.ZERO, 100, 42L, false, List.of(),
                Map.of(placement, List.of(ring)),
                (structures, chunk, p, skip) -> Optional.of(
                        new StructureHit(new BlockPos(40, 32, 40), structure)),
                new LocateStats(), () -> false);
        assertEquals(structure, StructureSearch.findNearest(input).orElseThrow().structure());
    }

    @Test
    void choosesClosestHitAcrossPlacementsInSameRing() {
        var first = new RandomSpreadStructurePlacement(4, 1, RandomSpreadType.LINEAR, 7);
        var second = new RandomSpreadStructurePlacement(4, 1, RandomSpreadType.LINEAR, 11);
        var structure = Holder.<Structure>direct(null);
        var groups = List.of(
                new StructurePlacementGroup(first, Set.of(structure)),
                new StructurePlacementGroup(second, Set.of(structure)));
        var input = new StructureSearchInput(
                BlockPos.ZERO, 1, 42L, false, groups, Map.of(),
                (structures, chunk, placement, skip) -> Optional.of(
                        new StructureHit(placement == first ? new BlockPos(100, 0, 0)
                                : new BlockPos(10, 0, 0), structure)),
                new LocateStats(), () -> false);
        assertEquals(new BlockPos(10, 0, 0), StructureSearch.findNearest(input).orElseThrow().position());
    }
}
```

`expectedRandomSpreadChunks` enumerates rings `k = 0..radius`, `j = -k..k`, `l = -k..k`, keeps
the perimeter, and maps each region coordinate to
`placement.getPotentialStructureChunk(seed, regionX, regionZ)`. It is the reference ordering.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew.bat :common:test --tests "*StructureSearchTest"`

Expected: compilation failure because the search types do not exist.

- [ ] **Step 3: Implement `StructureSearch`**

- Split input placements into concentric and random-spread groups. Ignore other placement types,
  matching vanilla.
- Replicate vanilla's concentric pass: iterate the captured ring positions, compute the candidate
  block position at section Y `32`, and only probe when the candidate is closer than the current
  best. A hit updates the best pair and squared distance.
- Replicate vanilla's random-spread pass: for `k = 0..searchRadius`, enumerate the perimeter with
  the `j`-outer, `l`-inner loops; probe each candidate; keep a flag for any hit in the ring; after
  all groups, return the best pair when the flag is set.
- Pass `skipKnownStructures` from the input to every probe call.
- Increment `stats.candidates` for every probe call.
- Check cancellation at each ring and candidate. Do no reordering.
- Use `SectionPos.blockToSectionCoord(origin.getX())` and `origin.getZ()` for the center region.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew.bat :common:test --tests "*StructureSearchTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/locate/engine \
        common/src/test/java/com/izakji/mccreatorutils/locate/engine
git commit -m "feat: reproduce vanilla structure search ordering"
```

---

### Task 6: Bounded Async Locate Service

**Files:**
- Create: `common/src/main/java/com/izakji/mccreatorutils/locate/engine/LocateService.java`
- Test: `common/src/test/java/com/izakji/mccreatorutils/locate/engine/LocateServiceTest.java`

**Interfaces:**
- Consumes: `LocateRequestKey`, `LocateTask`, `LocateResult`, `LocateFailure`, `CancellationToken`.
- Produces:
  - `final class LocateService implements AutoCloseable`:
    - constructor `LocateService(int biomeThreads, int maxQueuedSearches, Duration resultCacheTtl, LongSupplier clockMillis)`
    - `CompletableFuture<LocateResult> submit(LocateRequestKey key, LocateTask task, long timeoutMillis)`
    - `void tick(long nowMillis)`
    - `void clearCache()`
    - `void cancelAll()`
    - `int cancelDimension(ResourceKey<Level> dimension)`
    - `int activeCount()`
    - `int queuedCount()`
    - `void close()`

- [ ] **Step 1: Write the failing tests**

```java
class LocateServiceTest {
    @Test
    void deduplicatesIdenticalRequests() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var runs = new AtomicInteger();
        try (var service = service(1, 8)) {
            LocateTask task = token -> {
                runs.incrementAndGet();
                started.countDown();
                release.await();
                return LocateResult.found(BlockPos.ZERO, Holder.direct(null), 0);
            };
            var key = key("minecraft:plains");
            var first = service.submit(key, task, 5_000);
            assertTrue(started.await(1, TimeUnit.SECONDS));
            var second = service.submit(key, task, 5_000);
            release.countDown();
            assertEquals(first.get(1, TimeUnit.SECONDS).position(), second.get(1, TimeUnit.SECONDS).position());
            assertEquals(1, runs.get());
        }
    }

    @Test
    void rejectsWhenQueueIsFull() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var service = service(1, 0)) {
            var first = service.submit(key("minecraft:plains"), token -> {
                started.countDown();
                release.await();
                return LocateResult.notFound(0);
            }, 5_000);
            assertTrue(started.await(1, TimeUnit.SECONDS));
            var second = service.submit(key("minecraft:forest"),
                    token -> LocateResult.notFound(0), 5_000);
            assertEquals(LocateFailure.QUEUE_FULL, second.join().failure());
            release.countDown();
            first.get(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void timesOutAndCachesOnlyCompletedResults() throws Exception {
        var clock = new AtomicLong(1_000);
        var runs = new AtomicInteger();
        try (var service = new LocateService(1, 2, Duration.ofSeconds(30), clock::get)) {
            var key = key("minecraft:plains");
            var result = service.submit(key, token -> {
                runs.incrementAndGet();
                return LocateResult.found(BlockPos.ZERO, Holder.direct(null), 25);
            }, 1_000).get(1, TimeUnit.SECONDS);
            assertEquals(25, result.elapsedMillis());
            service.submit(key, token -> {
                runs.incrementAndGet();
                return LocateResult.notFound(0);
            }, 1_000).get(1, TimeUnit.SECONDS);
            assertEquals(1, runs.get());
            clock.addAndGet(31_000);
            service.submit(key, token -> {
                runs.incrementAndGet();
                return LocateResult.notFound(0);
            }, 1_000).get(1, TimeUnit.SECONDS);
            assertEquals(2, runs.get());
        }
    }
}
```

Add a timeout test using a task that waits until its token is cancelled, then calls
`service.tick(now + timeout + 1)` and asserts `LocateFailure.TIMEOUT`. Add a `cancelAll` test that
asserts `LocateFailure.CANCELLED`. `service(threads, queued)` creates a service with a 30-second
cache and a manual clock.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew.bat :common:test --tests "*LocateServiceTest"`

Expected: compilation failure because `LocateService` does not exist.

- [ ] **Step 3: Implement the service**

- Use a fixed daemon `ThreadPoolExecutor` for `biomeThreads` and a separate single-thread daemon
  executor for structures. Name threads `mccreatorutils-locate-biome-N` and
  `mccreatorutils-locate-structure`.
- Keep a `ConcurrentHashMap<LocateRequestKey, SharedRequest>` for active deduplication. A
  `SharedRequest` holds the underlying future, deadline, cancellation token, and caller reference
  count. Return a copy of the underlying future to each caller.
- Admission checks the active/queued cap before starting a task. `maxQueuedSearches=0` still allows
  one running biome task and one running structure task.
- Cache only `LocateResult` values with `success()` or `failure() == NOT_FOUND`, keyed by
  `LocateRequestKey`. Expiry uses the supplied clock and `Duration`.
- `tick` cancels and completes expired requests with `TIMEOUT`. `cancelAll` completes active and
  queued requests with `CANCELLED` and interrupts workers. `cancelDimension` removes matching keys.
- Convert thrown exceptions to `LocateResult.failed(ERROR, elapsed)` and log the exception at error
  level with the request key.
- `close` stops admission, cancels everything, shuts down both executors, and awaits briefly.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew.bat :common:test --tests "*LocateServiceTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/izakji/mccreatorutils/locate/engine \
        common/src/test/java/com/izakji/mccreatorutils/locate/engine
git commit -m "feat: add bounded async locate service"
```

---

### Task 7: NeoForge Build, Config, and Lifecycle

**Files:**
- Modify: `gradle.properties`
- Modify: `neoforge/build.gradle`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/locate/LocateConfig.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/locate/LocateLifecycle.java`
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/McCreatorUtilsNeoForge.java`
- Create: `neoforge/src/main/resources/mccreatorutils.mixins.json`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/mixin/MixinPlugin.java`
- Modify: `neoforge/src/main/templates/META-INF/neoforge.mods.toml`

**Interfaces:**
- Consumes: `LocateService`, `LocateSettings`.
- Produces:
  - `LocateConfig.SPEC` and `LocateConfig.settings()` returning `LocateSettings`.
  - `LocateLifecycle.service()` returning the active `LocateService` or `null`.
  - `LocateLifecycle.start(ServerLevel)` is not used; the service is server-wide.
  - `MixinPlugin` returns false for all locate mixins when `asynclocator` or `biomespy` is loaded.

- [ ] **Step 1: Add TerraBlender compile-only dependency**

Append to `gradle.properties`:

```properties
terrablender_version=1.21.1-4.1.0.8
```

Add to `neoforge/build.gradle`:

```groovy
repositories {
    maven {
        name = 'MinecraftForge'
        url = uri('https://maven.minecraftforge.net/')
    }
}
```

Add to `dependencies`:

```groovy
compileOnly "com.github.glitchfiend:TerraBlender-neoforge:${terrablender_version}"
```

- [ ] **Step 2: Implement config and lifecycle**

- `LocateConfig` builds a `ModConfigSpec` with the defaults from `LocateSettings`.
  `settings()` reads the spec values each call and constructs `LocateSettings`.
- `LocateLifecycle` subscribes to `ServerStartedEvent`, `ServerStoppingEvent`,
  `ServerTickEvent.Post`, `LevelEvent.Unload`, and `OnDatapackSyncEvent`.
- On server start: `service = new LocateService(settings.biomeThreads(), settings.maxQueuedSearches(), Duration.ofSeconds(settings.resultCacheSeconds()), System::currentTimeMillis)`.
- On tick: `service.tick(System.currentTimeMillis())`.
- On stop: `service.close()` and set it to null.
- On level unload: `service.cancelDimension(level.dimension())`.
- On datapack sync: `service.clearCache()`.
- Guard every handler against a null service.

- [ ] **Step 3: Register config**

Change `McCreatorUtilsNeoForge` to accept `IEventBus modEventBus, ModContainer modContainer` and
call:

```java
modContainer.registerConfig(ModConfig.Type.SERVER, LocateConfig.SPEC);
```

Leave the existing common-side initialization comment.

- [ ] **Step 4: Add mixin metadata and conflict plugin**

Create `mccreatorutils.mixins.json` with package `com.izakji.mccreatorutils.mixin`,
`compatibilityLevel: "JAVA_21"`, `plugin: "com.izakji.mccreatorutils.mixin.MixinPlugin"`, an empty
`mixins` array for now, and `injectors.defaultRequire: 1`. Add the `[[mixins]]` block to
`neoforge.mods.toml`:

```toml
[[mixins]]
config="mccreatorutils.mixins.json"
```

`MixinPlugin` checks `FMLLoader.getLoadingModList().getModFileById("asynclocator")` and
`getModFileById("biomespy")`; return false for our mixin package when either is present.

- [ ] **Step 5: Compile**

Run: `./gradlew.bat :neoforge:compileJava`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add gradle.properties neoforge/build.gradle \
        neoforge/src/main/java/com/izakji/mccreatorutils \
        neoforge/src/main/resources/mccreatorutils.mixins.json \
        neoforge/src/main/templates/META-INF/neoforge.mods.toml
git commit -m "feat: add locate config and lifecycle"
```

---

### Task 8: Minecraft Biome Backend and TerraBlender Adapter

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/locate/BiomeBackend.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/locate/MinecraftClimateSampler.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/locate/BiomeBackendFactory.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/locate/TerraBlenderCompat.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/mixin/MultiNoiseBiomeSourceAccessor.java`

**Interfaces:**
- Consumes: `BiomeEnvelopeProvider`, `ClimateSampler`, `BiomeEnvelopeIndex`, `ParameterListEnvelopeProvider`.
- Produces:
  - `record BiomeBackend(BiomeEnvelopeProvider<Biome> envelopes, ClimateSampler<Biome> sampler)`
  - `final class MinecraftClimateSampler implements ClimateSampler<Biome>` with
    `MinecraftClimateSampler(Climate.Sampler sampler, BiomeResolver resolver)` and a nested
    `@FunctionalInterface BiomeResolver { Holder<Biome> resolve(Climate.TargetPoint point, int blockX, int blockY, int blockZ); }`.
  - `final class BiomeBackendFactory { static BiomeBackend create(ServerLevel level); }`
  - `MultiNoiseBiomeSourceAccessor` with `@Invoker("parameters") Climate.ParameterList<Holder<Biome>> invokeParameters();`
  - `TerraBlenderCompat` with `static BiomeEnvelopeProvider<Biome> provider(MultiNoiseBiomeSource source, Climate.ParameterList<Holder<Biome>> parameters, ServerLevel level)` and `static Holder<Biome> resolve(Climate.ParameterList<Holder<Biome>> parameters, Climate.TargetPoint point, int blockX, int blockY, int blockZ)`.

- [ ] **Step 1: Implement `MinecraftClimateSampler`**

- `column(x,z)` creates one `DensityFunction.SinglePointContext(x, 0, z)` and caches a `Long[]`
  for the five axes.
- `value(axis)` calls the matching `Climate.Sampler` density function and applies
  `Climate.quantizeCoord((float) value)`.
- `depth(y)` creates a `SinglePointContext(x, y, z)` and computes `sampler.depth()`.
- `resolve(point, x, y, z)` delegates to the supplied `BiomeResolver`.

- [ ] **Step 2: Implement `BiomeBackendFactory`**

- Read `BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource()` and
  `Climate.Sampler sampler = level.getChunkSource().randomState().sampler()`.
- If `source instanceof MultiNoiseBiomeSource multi`:
  - Read parameters through `MultiNoiseBiomeSourceAccessor`.
  - Build the base `BiomeEnvelopeIndex` and `ParameterListEnvelopeProvider`.
  - Cache the base index by parameter-list identity in an `IdentityHashMap` guarded by the
    factory lock. Cache the TerraBlender provider by parameter-list identity and region type.
    Expose `static void clearCache()` for datapack reloads.
  - If `ModList.get().isLoaded("terrablender")` and TerraBlender is initialized for the level:
    call `TerraBlenderCompat.provider(multi, parameters, level)`. Otherwise keep the base provider.
  - Resolver:
    - Base path: `multi.getNoiseBiome(point)`.
    - TerraBlender path: `TerraBlenderCompat.resolve(parameters, point, x, y, z)`.
- If the source is not a `MultiNoiseBiomeSource`, use a provider whose lookup always returns
  `Optional.empty()` and resolver `source.getNoiseBiome(qx, qy, qz, sampler)`.
- Never classload `TerraBlenderCompat` unless the mod is loaded; keep the call in a separate
  guarded static holder or method.

- [ ] **Step 3: Implement `TerraBlenderCompat`**

- Determine `RegionType` with `LevelUtils.getRegionTypeForDimension(level.dimensionTypeRegistration())`.
- Require `parameters instanceof IExtendedParameterList`, `isInitialized()`, and a non-null region
  type; otherwise return the base provider.
- Build base indexes and region indexes:
  - Region zero uses `parameters.values()`.
  - Other regions call `region.addBiomes(registry, pair -> ...)`, resolve holders with
    `registry.getHolderOrThrow(pair.getSecond())`, and build an index with
    `BiomeEnvelopeIndex.fromValues`.
  - Include the base index in every region's target union.
- `forTargets` returns an empty lookup if any target is absent from all known indexes; otherwise
  `getUniqueness(quartX, 0, quartZ)` selects the region.
- `resolve` calls `IExtendedParameterList.findValuePositional(point, qx, qy, qz)`.

- [ ] **Step 4: Add the accessor mixin**

```java
@Mixin(MultiNoiseBiomeSource.class)
public interface MultiNoiseBiomeSourceAccessor {
    @Invoker("parameters")
    Climate.ParameterList<Holder<Biome>> invokeParameters();
}
```

- [ ] **Step 5: Compile**

Run: `./gradlew.bat :neoforge:compileJava`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add gradle.properties neoforge/build.gradle \
        neoforge/src/main/java/com/izakji/mccreatorutils/locate \
        neoforge/src/main/java/com/izakji/mccreatorutils/mixin
git commit -m "feat: add modded biome backend"
```

---

### Task 9: Structure Probe, Coordinator, and Synchronization Mixins

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/locate/StructureProbeImpl.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/locate/LocateCoordinator.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/mixin/StructureCheckMixin.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/mixin/ChunkGeneratorMixin.java`
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/locate/LocateLifecycle.java`
- Modify: `neoforge/src/main/resources/mccreatorutils.mixins.json`

**Interfaces:**
- Consumes: `BiomeBackendFactory`, `StructurePrefilter`, `StructureSearch`, `StructureSearchInput`, `LocateService`, `LocateLifecycle`, `LocateSettings`, `LocateStats`.
- Produces:
  - `StructureProbeImpl(ServerLevel level, StructureBiomeFilter filter, LocateStats stats)` implementing
    `StructureProbe`.
  - `LocateCoordinator` with:
    - `OptionalInt submitBiome(CommandSourceStack source, ResourceOrTagArgument.Result<Biome> target)`
    - `OptionalInt submitStructure(CommandSourceStack source, HolderSet<Structure> targets, ResourceOrTagKeyArgument.Result<Structure> printable)`
    - `static LocateCoordinator current()` or an instance held by `LocateLifecycle`.
  - `StructureCheckMixin` and `ChunkGeneratorMixin` synchronization.
  - `BiomeBackendFactory.clearCache()` is called from the datapack-sync handler in
    `LocateLifecycle`.

- [ ] **Step 1: Implement the structure probe**

- `StructureProbeImpl.probe` runs on the structure lane:
  - Increment `LocateStats.presenceChecks` for every `checkStructurePresence` call and increment
    `LocateStats.prefilterSkips` when `filter.mayMatch` rejects a structure.
  - For each `Holder<Structure>`:
    - Skip when `filter.mayMatch(holder, chunk)` is false.
    - Call `level.structureManager().checkStructurePresence(chunk, holder.value(), placement, skipKnownStructures)`.
    - Return `StructureHit(placement.getLocatePos(chunk), holder)` when the result is `START_PRESENT`
      and `skipKnownStructures` is false.
    - Otherwise call `level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS)` and increment
      `LocateStats.chunkLoads`.
    - Read the start with `level.structureManager().getStartForStructure(SectionPos.bottomOf(chunkAccess), holder.value(), chunkAccess)`.
    - Accept a valid start when `!skipKnownStructures` or the synchronized reference claim succeeds.
- Use a `synchronized (start)` block for `canBeReferenced()` plus `level.structureManager().addReference(start)`.
- Return `Optional.empty()` after all structures.

- [ ] **Step 2: Implement the coordinator**

- `submitBiome`:
  - Resolve `targets` from `level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().filter(target)`.
  - If empty, send vanilla biome-not-found on the server thread and return `OptionalInt.of(1)`.
  - Build target ids from holder keys for the request key.
  - Build backend, input with origin `BlockPos.containing(source.getPosition())`,
    radius `6400`, horizontal step `32`, vertical step `64`, min/max build height, a new
    `LocateStats`, and the request token.
  - Submit to `LocateLifecycle.service()` with timeout seconds from config.
  - Dispatch feedback to the server thread. Log `stats.summary()` when `debugStats` is enabled.
- `submitStructure`:
  - Build ordered placement groups from `ChunkGeneratorStructureState.getPlacementsForStructure`
    using a `LinkedHashMap`.
  - Snapshot concentric ring positions with `state.getRingPositionsFor(placement)`.
  - Build the structure biome filter with `new StructurePrefilter<>(backend.envelopes(), backend.sampler(), holder -> holder.value().biomes().stream().collect(Collectors.toUnmodifiableSet()), margin)` when `settings.structurePrefilter()` is true; otherwise use `StructurePrefilter.disabled()`.
  - Build `StructureSearchInput` with origin, radius `100`, seed `state.getLevelSeed()`,
    groups, ring map, a local `LocateStats stats`, `new StructureProbeImpl(level, filter, stats)`,
    `stats`, and the request token.
  - Submit to the service.
- On completion, if `settings.debugStats()` is true, log the request target, outcome, elapsed time,
  and `stats.summary()`.
- Feedback helper:
  - Check server running, level active, and player still connected.
  - `NOT_FOUND` sends the vanilla not-found escaped component.
  - `QUEUE_FULL`, `TIMEOUT`, `ERROR` send the matching new translation key.
  - `CANCELLED` is silent.
  - Success calls `LocateCommand.showLocateResult` with `Pair.of(result.position(), result.holder())`,
    the correct translation key, and `Duration.ofMillis(result.elapsedMillis())`.
- The coordinator must expose feedback only on the server thread.

- [ ] **Step 3: Add synchronization mixins**

- `StructureCheckMixin` targets `checkStart`, `onStructureLoad`, and `incrementReference`. If the
  current thread does not already hold the monitor, synchronize on the `StructureCheck` instance
  and re-invoke the method; cancel the original call.
- `ChunkGeneratorMixin` targets private static `tryAddReference`; synchronize on the
  `StructureStart`, atomically check `canBeReferenced()`, call `addReference`, and return the
  boolean.
- Do not clear `StructureCheck.featureChecks`.

- [ ] **Step 4: Wire backend cache invalidation**

- In the `LocateLifecycle` datapack-sync handler, call both `service.clearCache()` and
  `BiomeBackendFactory.clearCache()`.
- In the level-unload handler, call `service.cancelDimension(level.dimension())`; the identity
  cache can remain until datapack reload or server stop because its keys are parameter-list
  identities.
- Add `LocateLifecycle.coordinator()`; construct one coordinator after the service on server start,
  clear it on server stop, and have `LocateCommandBridge` read it.

- [ ] **Step 5: Register mixins**

Add `StructureCheckMixin` and `ChunkGeneratorMixin` to `mccreatorutils.mixins.json`.

- [ ] **Step 6: Compile**

Run: `./gradlew.bat :neoforge:compileJava`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils/locate \
        neoforge/src/main/java/com/izakji/mccreatorutils/mixin \
        neoforge/src/main/resources/mccreatorutils.mixins.json
git commit -m "feat: add structure probe and structure lane"
```

---

### Task 10: Command Hook, Client Timeout, and Translations

**Files:**
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/locate/LocateCommandBridge.java`
- Create: `neoforge/src/main/java/com/izakji/mccreatorutils/mixin/LocateCommandMixin.java`
- Modify: `neoforge/src/main/resources/mccreatorutils.mixins.json`
- Modify: `neoforge/src/main/java/com/izakji/mccreatorutils/client/ClientLocateTeleporter.java`
- Modify: `neoforge/src/main/resources/assets/mccreatorutils/lang/en_us.json`
- Modify: `neoforge/src/main/templates/META-INF/neoforge.mods.toml` only if the mixin block is missing.

**Interfaces:**
- Consumes: `LocateCoordinator`, `LocateLifecycle`, `LocateCommand.showLocateResult`.
- Produces:
  - `LocateCommandBridge.submitBiome(CommandSourceStack, ResourceOrTagArgument.Result<Biome>)` and
    `submitStructure(CommandSourceStack, ResourceOrTagKeyArgument.Result<Structure>)`, both
    returning `OptionalInt`.
  - `LocateCommandMixin` intercepts `locateStructure` and `locateBiome` at `HEAD`.
  - `ClientLocateTeleporter` timeout `120_000L` and async failure keys.

- [ ] **Step 1: Implement the bridge**

- Return `OptionalInt.empty()` when the source has no `ServerPlayer`, config is disabled, the
  service/coordinator is unavailable, or the target cannot be resolved. The mixin then leaves
  vanilla behavior intact.
- Resolve the structure `HolderSet<Structure>` exactly as vanilla:
  `structure.unwrap().map(key -> registry.getHolder(key).map(HolderSet::direct), registry::getTag)`.
- Delegate to `LocateCoordinator`.
- On queue-full submission, send the failure and return `OptionalInt.of(0)`.
- On accepted submission, return `OptionalInt.of(1)`.

- [ ] **Step 2: Implement the command mixin**

```java
@Mixin(LocateCommand.class)
public final class LocateCommandMixin {
    @Inject(method = "locateStructure", at = @At("HEAD"), cancellable = true)
    private static void mccreatorutils$locateStructure(
            CommandSourceStack source,
            ResourceOrTagKeyArgument.Result<Structure> target,
            CallbackInfoReturnable<Integer> cir) {
        LocateCommandBridge.submitStructure(source, target)
                .ifPresent(result -> cir.setReturnValue(result));
    }

    @Inject(method = "locateBiome", at = @At("HEAD"), cancellable = true)
    private static void mccreatorutils$locateBiome(
            CommandSourceStack source,
            ResourceOrTagArgument.Result<Biome> target,
            CallbackInfoReturnable<Integer> cir) {
        LocateCommandBridge.submitBiome(source, target)
                .ifPresent(result -> cir.setReturnValue(result));
    }
}
```

Add `LocateCommandMixin` and `MultiNoiseBiomeSourceAccessor` to `mccreatorutils.mixins.json`.

- [ ] **Step 3: Update the client status handling**

- Change `TIMEOUT_MILLIS` in `ClientLocateTeleporter` to `120_000L`.
- Change the pending failure key field from one string to a `Set<String>` containing:
  - `commands.locate.biome.not_found` or `commands.locate.structure.not_found`
  - `commands.mccreatorutils.locate.queue_full`
  - `commands.mccreatorutils.locate.timeout`
  - `commands.mccreatorutils.locate.failed`
- On any matching key, fail the pending request. Keep the existing success parser.
- Keep one pending request per client.

- [ ] **Step 4: Add translations**

Add to `en_us.json`:

```json
"commands.mccreatorutils.locate.queue_full": "Too many locate searches are running. Try again shortly.",
"commands.mccreatorutils.locate.timeout": "Locate search timed out.",
"commands.mccreatorutils.locate.failed": "Locate search failed."
```

- [ ] **Step 5: Build and verify**

Run: `./gradlew.bat :common:test`

Expected: PASS.

Run: `./gradlew.bat build`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add neoforge/src/main/java/com/izakji/mccreatorutils \
        neoforge/src/main/resources \
        neoforge/src/main/templates
git commit -m "feat: hook async locate into commands"
```

---

### Task 11: Integration, Profiling, and Acceptance

**Files:**
- Modify: only files required by defects found in this task.
- Test setup: `neoforge/run/mods` (Lithostitched jar; not committed).

**Interfaces:**
- Consumes: the complete engine.
- Produces: verified behavior and any required fixes.

- [ ] **Step 1: Prepare the test environment**

- Add the Lithostitched jar to `neoforge/run/mods`; Terralith requires it.
- Keep Terralith, TerraBlender, Biomes O' Plenty, and the reference locate mods out of the test
  run when testing our hook. The mixin plugin disables our hook if Async Locator Refined or
  BiomeSpy is present.
- Run `./gradlew.bat :neoforge:runClient`.

- [ ] **Step 2: Verify vanilla-compatible results**

For the same world and origin, record the vanilla result first with `enabled=false`, then with the
engine enabled. Check:

- `minecraft:plains`
- a cave biome such as `minecraft:lush_caves`
- an ocean biome
- a Terralith biome
- a Biomes O' Plenty or TerraBlender biome
- a vanilla structure and a Terralith structure

Expected: same coordinates in both runs for each target, within the same worldgen configuration.

- [ ] **Step 3: Verify asynchronous behavior**

- Confirm the locate success message arrives without blocking movement or the server tick.
- Confirm the Biomes and Structures panels show searching, teleport, and re-enable correctly.
- Confirm no premature 120-second client timeout appears.
- Trigger two searches concurrently.
- Fill the queue and confirm the queue-full failure.
- Allow a rare search to time out and confirm the timeout failure.
- Disconnect during a search and confirm no feedback is sent to a stale player.

- [ ] **Step 4: Verify modded edge cases**

- Terralith biomes are found through the base parameter list.
- TerraBlender biomes are found through per-region envelopes.
- A target missing from the parameter list still runs exact search instead of returning false
  not-found.
- A structure with a nearby biome check is not skipped at the default margin.
- With TerraBlender removed from `run/mods`, the game starts, the base path works, and no
  TerraBlender classloading error occurs.
- With Async Locator Refined or BiomeSpy installed, our hook is disabled and vanilla locate still
  works.

- [ ] **Step 5: Profile and inspect logs**

- Enable `debugStats` temporarily. Confirm each request logs counts and elapsed time.
- Check `latest.log` for tick warnings or exceptions.
- Confirm no locate candidate loop runs on the server tick thread. Chunk generation from the
  structure lane is expected and should not produce a full search loop on the tick thread.
- Confirm no deadlock between the structure lane and chunk generation.

- [ ] **Step 6: Run the full verification**

Run: `./gradlew.bat :common:test`

Expected: PASS, including the new engine tests and the existing 56 tests.

Run: `./gradlew.bat build`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit any fixes**

```bash
git add <changed files>
git commit -m "fix: resolve async locate integration issues"
```

If no fixes are needed, record the verification results in the task notes instead of creating an
empty commit.
