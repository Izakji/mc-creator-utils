# Async Locate Engine Design

**Date:** 2026-10-27
**Status:** Ready for review
**Target:** Minecraft 1.21.1, NeoForge 21.1.265
**Branch:** `feat/async-locate-engine`

## 1. Purpose

Replace the server-side search used by player-issued `/locate biome` and `/locate structure`
commands with one asynchronous locate engine owned by Creator Utils.

The engine combines the two useful ideas from the installed reference mods:

- **Async Locator Refined 1.6.0:** bounded asynchronous execution, request deduplication,
  cancellation, timeouts, and completion on the server thread.
- **BiomeSpy 1.3.3:** height-independent climate sampling, climate-envelope pruning, and
  structure-biome prefiltering.

It also fixes problems found in those implementations:

- BiomeSpy performs the whole search synchronously on the server thread.
- BiomeSpy doubles the vanilla biome search radius, which changes results.
- BiomeSpy samples a structure candidate only at the chunk corner, which can skip a structure
  whose biome check runs at another position in the chunk.
- BiomeSpy clears `StructureCheck.featureChecks` on every check, discarding useful cached work.
- Async Locator runs the entire vanilla worldgen search on a worker thread and relies on partial
  synchronization of worldgen internals. It has no algorithmic pruning.

The result must remain compatible with vanilla results, work in modded biome environments, and
remove locate search loops from the server tick.

## 2. Requirements

### Functional

- Intercept player-issued `/locate biome <target>` and `/locate structure <target>`.
- Keep the existing vanilla chat success message with its clickable `/tp @s ...` command.
- Preserve vanilla first-match search order and search bounds.
- Preserve vanilla not-found and invalid-target behavior.
- Support vanilla biomes, Terralith biomes, TerraBlender biomes, Biomes O' Plenty biomes, and
  biome additions from datapack or `MultiNoiseBiomeSource` parameter-list mods.
- Support modded structures that use vanilla random-spread or concentric-ring placements.
- Keep the current Creator Utils Biomes and Structures panels working without a client protocol
  change.

### Non-functional

- No locate search loop on the server tick thread.
- Structure checks and chunk lookups must not block the server tick.
- Bounded concurrency, bounded queue, cancellation, and timeouts.
- No unbounded growth of request, envelope, or result caches.
- No direct dependency on Async Locator Refined or BiomeSpy.
- TerraBlender support is optional at runtime.
- Terralith and Lithostitched support requires no compile-time dependency.

### Out of scope

- `/locate poi` optimization.
- Eye of Ender, exploration maps, dolphin treasure, and merchant map searches.
- True-nearest reordering or larger search radii.
- Custom structure placement types beyond vanilla behavior.
- Client-side search or a new client/server packet protocol.
- Fabric support in this milestone.
- Copying source from BiomeSpy or Async Locator Refined.

## 3. Analysis of the Reference Mods

### 3.1 Async Locator Refined 1.6.0

The installed jar was decompiled and compared with the `1.21.1-1.6.0` source tag.

Relevant behavior:

- `AsyncLocator` submits whole vanilla locate calls to a virtual-thread-per-task executor.
- `LocateTaskLimiter` limits active searches and queued searches. The default is two concurrent
  and 128 queued searches.
- Identical structure requests are coalesced by dimension, target, position, and radius.
- Completion handlers are dispatched back to the server thread with `TickTask`.
- The API supports timeouts, cancellation, and level-active checks.
- Mixins synchronize selected `StructureCheck` methods and atomically claim structure references.
- The mod also covers Eye of Ender, maps, dolphins, and merchant trades; those features are outside
  this design.

The useful pattern is the request lifecycle: bounded admission, deduplication, cancellation,
timeout, and server-thread completion.

The limitation is that `findNearestMapStructure` and `findClosestBiome3d` still execute the
vanilla algorithm. That algorithm is not thread-safe as a whole. Async Locator reduces tick stalls
but does not reduce the amount of search work.

### 3.2 BiomeSpy 1.3.3

The installed jar was decompiled and compared with the current source.

Relevant behavior:

- `BiomeFinder` observes that temperature, humidity, continentalness, erosion, and weirdness are
  height-independent. It samples those five values once per `(x, z)` column.
- It builds an axis-aligned climate envelope for the target biomes. A column outside the envelope
  is rejected before the full biome lookup.
- Depth is sampled only for columns that pass all five envelope checks.
- `BiomeEnvelopeSelector` adds TerraBlender support by selecting envelopes per TerraBlender region.
- `StructureChecker` uses a 2D climate check at the candidate chunk's minimum X/Z block before
  calling the more expensive structure-presence check.
- `StructureCheckMixin` clears `StructureCheck.featureChecks` on every `checkStart` call.

The useful ideas are the envelope prune, the five-height-independent-noise observation, and the
structure-biome prefilter.

Problems not carried into this design:

- The search runs synchronously.
- BiomeSpy uses `radius * 2` for the biome search, unlike vanilla's `radius`.
- The structure prefilter samples only the chunk corner, which is not a sound necessary condition
  for structures that check a biome elsewhere in the chunk.
- Clearing `featureChecks` discards valid cache entries and increases repeated work.
- The implementation is tied to a small set of known biome sources.

### 3.3 Modded Biome Findings

The installed mod set contains TerraBlender, Biomes O' Plenty, and Terralith.

- **Terralith** does not use TerraBlender. Its
  `data/minecraft/worldgen/multi_noise_biome_source_parameter_list/overworld.json` overrides the
  overworld parameter list with `preset` plus `lithostitched:biomes`.
- **Lithostitched** decodes that file and replaces the actual
  `MultiNoiseBiomeSource` parameter list. The final `ParameterList.values()` therefore contains
  Terralith biomes.
- **TerraBlender** mixes into `Climate.ParameterList`, maintains a separate tree per region, and
  patches `MultiNoiseBiomeSource.getNoiseBiome` to use positional region lookup.
- **Lithostitched biome injectors** can wrap the source in `InjectorBiomeSource`. That wrapper can
  apply force-placement, alternate-layout, and replacement rules that are not represented by the
  root parameter list.

The engine therefore treats the final parameter list and TerraBlender region tables as the fast
path, and falls back to exact search for wrappers or unknown sources.

## 4. Architecture

### 4.1 Components

The loader-neutral engine lives in `common` under `com.izakji.mccreatorutils.locate`.

- `LocateType` — `BIOME` or `STRUCTURE`.
- `LocateRequestKey` — dimension, type, canonical target ids, origin, radius, and
  `skipKnownStructures`.
- `LocateRequest` — immutable request data plus cancellation and deadline state.
- `LocateService` — admission, deduplication, executors, timeouts, result cache, completion
  dispatch, and shutdown.
- `LocateResult` — position, matched holder, elapsed time, and optional failure reason.
- `BiomeSearch` — pure biome loop.
- `StructureSearch` — pure candidate generation and structure-biome prefilter.
- `BiomeEnvelope`, `BiomeEnvelopeIndex`, `BiomeEnvelopeProvider` — climate-space boxes and lookup.
- `StructurePrefilter` — candidate-chunk sampling against the structure's biome envelope.
- `LocateStats` — optional counters and timing data.

NeoForge-specific code lives in `neoforge` under
`com.izakji.mccreatorutils.locate` and `com.izakji.mccreatorutils.mixin`.

- `LocateLifecycle` — server start/stop, tick watchdog, level unload, config reload.
- `LocateConfig` — NeoForge server config.
- `NeoForgeLocateHooks` — level-active, player-connected, and server-thread dispatch helpers.
- `TerraBlenderCompat` — optional region and envelope adapter.
- `LocateCommandMixin` — intercept `/locate`.
- `StructureCheckMixin` — synchronize structure-check state with the structure lane.
- `ChunkGeneratorMixin` — synchronize structure-reference claims.
- `MultiNoiseBiomeSourceAccessor` — expose the parameter list.
- `MixinPlugin` — disable our locate mixins when a conflicting locate mod is loaded.

The engine must not import NeoForge classes. Platform-specific services are passed through small
interfaces so a future Fabric module can reuse the search and envelope code.

### 4.2 Data Flow

```text
Player /locate command
    LocateCommandMixin
        LocateService.submit
            deduplicate request
            admit to biome pool or structure lane
            BiomeSearch (pure worker)
            StructureSearch (structure lane)
                candidate generation
                StructurePrefilter
                checkStructurePresence
                STRUCTURE_STARTS chunk lookup
            server tick thread
                success/failure feedback
```

### 4.3 Thread Rules

- **Biome workers:** read-only climate sampling and parameter-list lookup. No `Level`,
  `StructureManager`, chunk access, or world mutation.
- **Structure lane:** one platform thread that runs both pure candidate generation and the world
  access required by structure validation. Candidate generation is written as pure, independently
  testable functions, but no other locate thread calls `StructureManager` or `Level.getChunk`.
  The lane may block; the server tick does not wait for it.
- **Server tick thread:** command interception, placement snapshots, chat feedback, and chunk
  generation scheduled by vanilla. No locate candidate loops run here.

`StructureCheck` access is serialized through the structure lane. The server tick can still call
those methods during normal chunk generation, so selected methods are synchronized with the server
thread as described in section 7.5.

### 4.4 Executors and Admission

- `biomePool`: fixed platform threads, default two, configured range one through four.
  `ThreadPoolExecutor` with named daemon threads and a bounded task queue.
- `structureLane`: one platform thread. All structure searches share it. Candidate generation and
  structure-presence checks are serialized to preserve vanilla ring order and protect structure
  caches. The structure lane is also the world access lane.
- Requests are admitted in submission order. `maxQueuedSearches` limits requests waiting for a
  biome worker or the structure lane.
- Duplicate requests share one internal future. Each caller gets a copy of the result.
- Cancellation uses a cooperative `AtomicBoolean` checked at column/ring boundaries and before
  every structure-lane world operation. If all callers cancel a shared request, the underlying
  request is cancelled.
- A request deadline is checked by a server tick watchdog and by long-running worker loops.
- On server stop, no new requests are admitted. Active requests are cancelled and executors are
  shut down with a short await.

**Result cache**

- Key by dimension, type, canonical target ids, exact origin, radius, and
  `skipKnownStructures`.
- Cache only successful results and fully completed not-found results. Do not cache
  cancellations, timeouts, or internal errors.
- Default lifetime is 30 seconds. `resultCacheSeconds=0` disables the cache.
- Clear on server stop, datapack reload, level unload, or parameter-list identity change.
- A cache hit still re-checks that the server is running, the level is active, and the source
  player is connected before feedback.

## 5. Modded Biome Support

### 5.1 Backend Selection

The engine chooses the most specific backend that can be proven correct for the source:

1. **Direct `MultiNoiseBiomeSource`:** base parameter-list envelope. This covers vanilla,
   Terralith, Lithostitched point lists, datapack parameter overrides, and other mods that put
   their biomes into the final `ParameterList.values()`.
2. **TerraBlender-augmented `MultiNoiseBiomeSource`:** per-region envelopes selected by
   `IExtendedParameterList.getUniqueness(qx, 0, qz)`, plus a base-list fallback envelope.
3. **Unknown source wrappers, including active Lithostitched `InjectorBiomeSource`:** no envelope.
   Exact search through the actual `BiomeSource.getNoiseBiome` continues, asynchronously.

A target biome that is absent from every known parameter list disables envelope pruning for the
whole request. The engine searches exactly instead of returning a false not-found.

### 5.2 Base Envelope Index

- Read `MultiNoiseBiomeSource.parameters().values()` once per parameter-list identity.
- Group `ParameterPoint` entries by `Holder<Biome>`.
- Build one `BiomeEnvelope` per biome from the temperature, humidity, continentalness, erosion, and
  weirdness intervals.
- Depth is not stored in the envelope because it is height-dependent.
- An envelope with a fixed axis remains valid. An envelope is impossible only when it has no
  parameter points.
- Cache the index by parameter-list identity. Clear it on server stop and datapack reload.

### 5.3 TerraBlender Envelope Index

TerraBlender is optional at runtime and `compileOnly` at build time.

- Determine the region type from the level's dimension type using TerraBlender's public
  `LevelUtils.getRegionTypeForDimension`.
- For each region index, gather parameter points from `Region.addBiomes`.
- Region index zero uses the base `ParameterList.values()`.
- Every region's target envelope includes the base-list envelope. TerraBlender can return the base
  tree when a region returns `DEFERRED_PLACEHOLDER`.
- If TerraBlender is installed but not initialized for the dimension, use the base index only.
- If the source is not a direct `MultiNoiseBiomeSource`, disable the TerraBlender fast path.

No TerraBlender class is loaded when the mod is absent. The adapter is isolated behind a
`BiomeEnvelopeProvider` implementation selected at runtime.

### 5.4 Terralith and Lithostitched

Terralith's biome entries are visible through the final `MultiNoiseBiomeSource` parameter list
after Lithostitched applies the `lithostitched:biomes` field. No Terralith-specific code is
required.

If Lithostitched wraps the source because biome injectors are active, the wrapper is treated as an
unknown backend. Exact async search preserves correctness, though the envelope speedup is lost.

### 5.5 Future Biome Mods

- A mod that adds entries to the final `MultiNoiseBiomeSource` parameter list gets the base fast
  path automatically.
- A mod that uses TerraBlender gets the regional fast path.
- A mod that wraps the source gets exact async fallback.
- A future adapter can implement `BiomeEnvelopeProvider` without changing the search loop.

## 6. Biome Search Algorithm

### 6.1 Inputs and Search Order

For `/locate biome`, capture:

- origin = `BlockPos.containing(source.getPosition())`
- radius = 6400
- horizontalStep = 32
- verticalStep = 64
- minimum build height + 1 and maximum build height
- the target predicate

Compute the target set from `biomeSource.possibleBiomes()` filtered by the predicate. An empty set
returns `null`, matching vanilla.

Use vanilla order exactly:

```text
i = Math.floorDiv(radius, horizontalStep)
for each column in BlockPos.spiralAround(BlockPos.ZERO, i, EAST, SOUTH):
    for each y in Mth.outFromOrigin(origin.y, minBuildHeight + 1, maxBuildHeight, verticalStep):
        check the biome
```

The search must not use BiomeSpy's doubled radius.

### 6.2 Envelope Pruning

For target parameter points

```text
p = (temperature, humidity, continentalness, erosion, weirdness, depth)
```

define the target box

```text
B = [min temperature, max temperature]
  x [min humidity, max humidity]
  x [min continentalness, max continentalness]
  x [min erosion, max erosion]
  x [min weirdness, max weirdness]
```

For each column `(x, z)`, sample only the five height-independent values. If any value is outside
its interval in the selected envelope, the column is a pruning candidate. Vanilla resolves the
nearest parameter point, not interval containment, so an outside sample can still resolve to the
target. The engine therefore prunes a candidate only when a zero-offset non-target parameter point
contains the full six-axis sample. Otherwise it falls through to the exact Y check. The default
coverage answer is false, which disables pruning.

This gate makes the prune sound for nearest-neighbour resolution. The intersection of the target
box with depth remains part of the exact Y check.

For TerraBlender, use the region selected by `getUniqueness(qx, 0, qz)`; uniqueness is computed
from X/Z only. Use the base envelope as a fallback when the region does not represent the target.

If the target is unrepresented in a selected region, combine the region envelope with the base
envelope. If the target is unrepresented everywhere, disable pruning.

### 6.3 Column and Y Evaluation

For each candidate column:

1. Build a lazy `ColumnClimate` record for the five axes.
2. Evaluate axes tightest-first. Fixed and narrow axes reject early.
3. Cache each axis value; evaluate each density function at most once per column.
4. If every axis is inside the envelope, iterate the vanilla Y order.
5. For each Y, compute depth with a `SinglePointContext` at `(blockX, y, blockZ)`.
6. Build the exact `Climate.TargetPoint` from the five cached values and depth.
7. Resolve the biome:
   - Direct `MultiNoiseBiomeSource`: `getNoiseBiome(TargetPoint)`.
   - TerraBlender: `IExtendedParameterList.findValuePositional(TargetPoint, qx, qy, qz)`.
   - Unknown source: the actual `biomeSource.getNoiseBiome(qx, qy, qz, sampler)`.
8. Return the first holder contained in the target set as
   `Pair.of(new BlockPos(x, y, z), holder)`.

The exact lookup uses the same quantized values as `Climate.Sampler.sample`. The target check,
Y order, and column order match vanilla.

### 6.4 Performance Expectations

Vanilla samples six noise functions for every column and every Y sample, then performs an R-tree
biome lookup. The new path samples five functions once per column, rejects most columns before
depth or R-tree work, and computes depth only for columns that can match.

The envelope index is built once per parameter list, so a Terralith-sized parameter list is not
rescanned for every request.

One worker handles one biome request. Concurrency comes from `biomePool` across independent
requests. Parallelizing one search requires preserving the earliest valid ordinal; that is a
future optimization only if benchmarks show a latency need.

## 7. Structure Search Algorithm

### 7.1 Vanilla Placement Order

For `/locate structure`, reproduce vanilla's placement selection:

- Build a placement map from every target holder and its placements.
- Handle `ConcentricRingsStructurePlacement` candidates first and update the best distance.
- For `RandomSpreadStructurePlacement`, iterate rings `k = 0..100`.
- Within a ring, visit candidates in vanilla's `j`-outer, `k`-inner perimeter order.
- Within one placement, return the first structure hit.
- Across placements in one ring, keep the closest hit by squared distance.
- Return after the first ring with any hit.

This preserves vanilla's first-hit behavior even though it is not globally nearest.

### 7.2 Placement Snapshot

Before the structure lane starts a request, the server tick thread captures:

- The placement list for each target holder.
- Concentric ring positions for each concentric placement.
- The level seed.
- The structure manager and level references for the structure lane.
- A reference to the actual biome source and sampler.

This avoids concurrent calls to `ensureStructuresGenerated` from a worker. The candidate phase only
performs immutable placement arithmetic and candidate generation.

### 7.3 Structure-Biome Prefilter

For each target structure, build the union envelope of its `biomes()` set using the same modded
backend selection as section 5.

For a candidate chunk, sample all quart X/Z positions in the chunk plus
`structurePrefilterMarginChunks` rings of adjacent chunks. The default margin is one chunk.

For a one-chunk margin, the sampled area is a 3 by 3 chunk square. Each chunk contributes its
4 by 4 quart grid, for 144 samples. Each sample uses only the five height-independent axes.

If every sampled position is outside the structure target's envelope for its selected region, and
the coverage gate confirms that a zero-offset non-target parameter point contains the sampled five
axes, skip the candidate's structure-presence check. If coverage is unavailable or false, proceed
with the exact vanilla check.

The prefilter does not sample depth. It is a best-effort speedup for the five height-independent
axes; the exact structure check remains authoritative. It is disabled by default so exact
structure results are preserved. A structure that checks biomes outside the sampled area, or whose
target could be nearest only at a depth where no covering point dominates, can be handled by
increasing the margin or leaving the prefilter disabled.

This replaces BiomeSpy's single chunk-corner sample, which is not sound for structures that check a
biome at the chunk center or another offset.

### 7.4 Candidate Validation

For each candidate that passes the prefilter, the structure lane performs the same steps as
vanilla:

1. `StructureManager.checkStructurePresence(chunkPos, structure, placement,
   skipKnownStructures)`.
2. If the result is `START_NOT_PRESENT`, reject the structure for this chunk.
3. If `skipKnownStructures` is false and the result is `START_PRESENT`, return
   `placement.getLocatePos(chunkPos)`.
4. Otherwise load the chunk with `ChunkStatus.STRUCTURE_STARTS`.
5. Read the structure start with `StructureManager.getStartForStructure`.
6. Accept it when it is valid and, for `skipKnownStructures=true`, the reference claim succeeds.

The structure lane may block on chunk generation. That blocks only the lane; the server tick
continues. The lane releases the structure-check lock before waiting for a chunk.

### 7.5 Caches and Locking

- Do not clear `StructureCheck.featureChecks`. Vanilla's cache is useful and is preserved.
- `StructureCheck.checkStart`, `onStructureLoad`, and `incrementReference` are synchronized with
  the server thread. The structure lane is the only locate thread that calls them.
- `ChunkGenerator.tryAddReference` is synchronized on the `StructureStart`, matching the
  known-good Async Locator pattern.
- Placement and ring snapshots are immutable for the lifetime of a request.
- Envelope indexes are read-only after construction.

### 7.6 Modded Structures

- Modded structures using random-spread or concentric-ring placements take the same path.
- Terralith structures with Terralith biome sets use the base envelope.
- TerraBlender biome sets use per-region envelopes.
- A structure with a biome that is absent from all parameter lists disables its prefilter.
- Active Lithostitched biome injectors disable the prefilter for that source.
- Unknown placement types are ignored, matching vanilla behavior.

## 8. Command and UI Integration

### 8.1 Command Interception

`LocateCommandMixin` injects at `HEAD` of `locateStructure` and `locateBiome`.

- Intercept only when the command source has a `ServerPlayer` entity.
- Resolve the target set and origin.
- Submit the request to `LocateService`.
- Return `1` immediately after successful submission.
- On admission failure, send the queue-full failure and return `0`.
- Do not intercept console, command-block, or other non-player sources. Those keep vanilla
  synchronous behavior and unchanged output semantics.

The structure branch resolves the same `HolderSet<Structure>` as vanilla. The biome branch passes
the parsed predicate and target result.

### 8.2 Feedback

On the server tick thread:

- Success calls the public `LocateCommand.showLocateResult` overload with the same translation key,
  absolute-Y flag, origin, pair, and elapsed duration. The existing clickable output remains:
  absolute Y for biomes, relative `~` Y for structures.
- Vanilla not-found sends the same escaped failure component as vanilla.
- Queue full, timeout, and internal failure use new translation keys:
  - `commands.mccreatorutils.locate.queue_full`
  - `commands.mccreatorutils.locate.timeout`
  - `commands.mccreatorutils.locate.failed`
- Check that the server is running, the level is still active, and the source player is still
  connected before sending feedback.

No new command and no client packet are introduced.

### 8.3 Client Status

`ClientLocateTeleporter` keeps its existing component parser for the vanilla success message.

- Raise its timeout from 15 seconds to 120 seconds so queued or rare searches do not fail in the
  UI before the server responds.
- Add the new failure keys to the recognized failure set.
- Keep the existing searching state, disabled teleport button, and green/red status colors.
- Keep the existing one-pending-request rule.

## 9. Configuration and Lifecycle

### 9.1 Configuration

Server config, namespace `locate`:

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `enabled` | `true` | boolean | Enable command interception and the async engine |
| `biomeThreads` | `2` | `1..4` | Maximum concurrent biome searches |
| `maxQueuedSearches` | `32` | `0..256` | Requests waiting for a worker or the structure lane |
| `timeoutSeconds` | `60` | `5..600` | Per-request deadline |
| `structurePrefilter` | `false` | boolean | Enable the best-effort structure-biome prefilter |
| `structurePrefilterMarginChunks` | `1` | `0..3` | Spatial margin for structure biome sampling |
| `resultCacheSeconds` | `30` | `0..300` | Successful and completed not-found result cache lifetime |
| `debugStats` | `false` | boolean | Log per-request counters and timings |

The biome thread limit is enforced with a semaphore at request start, so config changes affect the
next request without rebuilding the pool. The structure lane remains one thread. Changes to
`enabled` take effect on the next command. Timeout and queue changes take effect on the next
request.

### 9.2 Lifecycle

- `ServerStartedEvent`: create the service, executors, and envelope cache; register the tick
  watchdog and config listeners.
- `ServerStoppingEvent`: stop admission, cancel requests, clear caches, shut down executors.
- `LevelEvent.Unload`: cancel requests for that level and remove level-specific cache entries.
- `OnDatapackSyncEvent`: clear envelope and result caches.
- `ServerTickEvent.Post`: check request deadlines and process any pending completion dispatch.
- Config reload: re-read scalar limits; keep the existing executor threads.

### 9.3 Failure Handling

- Queue full: immediate failure, no worker allocation.
- Timeout: cancel, log at debug, send timeout failure if the source is valid.
- Worker exception: log with target, dimension, origin, and duration; send generic failure.
- Interrupted structure-lane work: treat as cancellation.
- Level unload or server stop: cancel silently.
- Player disconnect: suppress feedback and release the request.
- Invalid target: argument parsing and the existing command error path handle it before the engine
  runs.
- Duplicate request: share the result; each caller still receives its own feedback.
- Result cache hit: complete on the server tick thread with the cached position; still apply the
  level-active and player-connected checks.

### 9.4 Logging

With `debugStats=true`, log one line per request containing:

- type, dimension, target, origin, radius, skip-known flag
- elapsed milliseconds
- columns sampled, envelope rejects, exact biome lookups
- candidate chunks enumerated, prefilter skips, presence checks, chunk loads
- queue wait time and final outcome

Logs must not include the full component tree or any player chat content.

## 10. Mathematical Rationale and Sources

### 10.1 Sound Interval Pruning

The envelope is an axis-aligned bounding box in five-dimensional climate space. If a column's
climate vector lies outside the box, no parameter point for the target biome can equal that
vector, regardless of depth. Skipping the column is therefore sound.

The structure prefilter applies the same test to the finite set of quart positions where a
standard structure may test its biome. If every sampled position is outside the target box, no
sampled position can be a valid structure biome. The margin determines the locality contract.

This is branch-and-bound style pruning: the box is a cheap lower bound on the possibility of a
match. It removes regions without enumerating their expensive checks.

Best-first nearest-neighbor ordering was considered and rejected. The selected requirement is
vanilla-compatible first-match behavior, so reordering candidates would change results.

### 10.2 Sources

Reference implementations inspected on 2026-10-27:

- [Async Locator Refined 1.21.1-1.6.0](https://github.com/Alvaro842DEV/AsyncLocator-Refined/tree/1.21.1-1.6.0)
- [BiomeSpy repository](https://github.com/MoePus/BiomeSpy)
- [BiomeSpy Modrinth page](https://modrinth.com/mod/biomespy)
- [Terralith](https://modrinth.com/mod/terralith)
- [Lithostitched](https://github.com/Apollounknowndev/lithostitched)
- [TerraBlender](https://github.com/Glitchfiend/TerraBlender)

Algorithm and platform references:

- [Minecraft Wiki: Structure set](https://minecraft.wiki/w/Structure_set)
- [Roussopoulos, Kelley, and Vincent, Nearest Neighbor Queries, SIGMOD 1995](https://dl.acm.org/doi/10.1145/223784.223794)
- [Branch and bound](https://en.wikipedia.org/wiki/Branch_and_bound)
- [Nearest neighbor search](https://en.wikipedia.org/wiki/Nearest_neighbor_search)
- [PaperMC asynchronous chunk loading discussion](https://github.com/PaperMC/Paper/pull/1397)

The Paper discussion documents why chunk publication, chunk contents, and worldgen state must
remain on the server thread. This design uses that constraint to justify the structure lane and
the split between pure search and world access.

### 10.3 License Boundary

Async Locator Refined is MIT-licensed. BiomeSpy is LGPL-3.0-only. Creator Utils is currently
All Rights Reserved.

This design reuses algorithms and interface ideas. Implementation must be written independently.
Do not copy BiomeSpy source files, class bodies, comments, or constants into Creator Utils. Small
API signatures such as TerraBlender's public methods are used as required for compatibility.

## 11. Test and Acceptance Matrix

### 11.1 Unit Tests

- Envelope construction for single-biome, tag, fixed-axis, empty, and missing-biome inputs.
- Envelope soundness: randomized target parameter points and column vectors; no outside-box
  column contains a target point.
- Biome fast path versus a reference vanilla loop on a synthetic sampler: same first hit.
- Vanilla biome constants: radius 6400, horizontal step 32, vertical step 64, no doubled radius.
- Structure candidate order versus a reference implementation of the vanilla ring loop.
- Structure prefilter: no candidate is skipped if any sample in the configured area is inside the
  target envelope.
- TerraBlender region selection and base-envelope fallback with synthetic region tables.
- `skipKnownStructures` true and false behavior.
- Concentric-ring first-pass and best-distance behavior.
- Deduplication, queue admission, timeout, cancellation, and result-cache expiry.

### 11.2 Build Verification

Run:

```text
./gradlew :common:test
./gradlew build
```

### 11.3 Manual Integration

Use a development world with Terralith, TerraBlender, Biomes O' Plenty, and Lithostitched. Add the
Lithostitched jar to `neoforge/run/mods`; Terralith requires it and it is not present in the
current run directory.

Test:

- Vanilla biome, cave biome, ocean biome, and a Terralith biome.
- A Biomes O' Plenty or other TerraBlender biome.
- A vanilla structure and a Terralith structure.
- Same-coordinate comparison against vanilla with the new engine disabled.
- UI teleport succeeds and no premature client timeout appears.
- Two concurrent searches, duplicate requests, queue-full rejection, timeout, and player
  disconnect.
- No locate search loop appears in the server tick profile.
- The structure lane can wait on chunk generation while the server tick continues.

### 11.4 Acceptance Criteria

- Player `/locate biome` and `/locate structure` return the same coordinates as vanilla for the
  test matrix when the worldgen configuration is unchanged.
- Terralith and TerraBlender biomes are found.
- Search CPU work runs outside the server tick.
- Queue, timeout, cancellation, deduplication, and shutdown are bounded and tested.
- The Creator Utils Biomes and Structures tools work without client protocol changes.
- `:common:test` and `build` pass.
- Async Locator or BiomeSpy installed disables our command hook, and vanilla locate still works.

## 12. Risks and Limitations

- **Chunk generation cost:** locate can still request chunks. Vanilla chunk generation remains on
  the server chunk pipeline. Envelope pruning reduces candidates but cannot eliminate all
  generation work.
- **Structure lane thread safety:** structure-check state is shared with normal chunk generation.
  The serialized lane and synchronization mixins reduce risk but do not replace a fully
  thread-safe worldgen implementation. Any crash or corruption must be treated as a release
  blocker.
- **Generic BiomeSource safety:** unknown sources are assumed to provide read-only
  `getNoiseBiome`. A source that mutates state during sampling is outside the supported contract
  and must use a future platform-specific fallback.
- **Structure locality contract:** the prefilter assumes the structure's biome-check position is
  within the candidate chunk plus the configured margin. Standard vanilla and normal modded
  structures satisfy this. A structure that checks farther away needs a larger margin or
  prefilter disabled.
- **Static worldgen assumption:** if a datapack changes biome parameters after chunks were
  generated, an existing structure start could be skipped by the structure prefilter. The engine
  targets normal creator worlds where worldgen configuration is stable during a session.
- **Unknown modded wrappers:** they use exact async fallback and lose the envelope speedup. This is
  a performance limitation, not a correctness failure.
- **Client timeout:** the client waits 120 seconds while the server deadline is 60 seconds. A
  stalled server can still cause a client-side failure message.
- **Conflict mods:** Async Locator Refined and BiomeSpy are detected and disabled to prevent double
  interception. They should be removed for testing the new engine.
- **Terralith setup:** Terralith requires Lithostitched. The current `run/mods` directory does not
  contain it; integration testing requires adding it.

## 13. Non-Goals

- No POI locate optimization.
- No Eye of Ender, exploration map, dolphin, or merchant map features.
- No true-nearest or larger-radius search.
- No custom placement-type implementation.
- No client-side locate search.
- No new client/server protocol.
- No copying of BiomeSpy or Async Locator source.
- No unrelated refactoring of the existing Camera, World Time, Items, Mobs, or registry UI code.
