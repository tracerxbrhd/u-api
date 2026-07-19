# Worldgen integration API

U-API 1.4.0 provides a small compatibility layer for ecosystem modules which add bounded,
deterministic regions to an existing world. It does not own the terrain implementation of those
modules and it does not replace the global chunk generator.

## Runtime context

Create one `WorldgenContext` for a loaded server level with
`WorldgenIntegrationRegistry.createContext(level)`. The context snapshots the dimension, seed,
build range, sea level, registry access, generator, biome source, random state, selected adapter
and its capabilities. It is safe to retain for asynchronous generation until that level unloads.

The following queries never request or load a chunk:

- `baseHeight(x, z, heightmapType)` asks the selected adapter for the generator's original column
  height;
- `baseBiome(x, y, z)` samples the original biome source in quart coordinates;
- `classifyBaseBiome(x, y, z)` returns `OCEAN`, `COAST`, `LAND` or `UNKNOWN`.

All queries return an empty/unknown fallback if the adapter lacks a capability or fails. The first
failure of each adapter operation is logged once; repeated per-block failures are not spammed.

## Adapters and capabilities

Register a stateless, thread-safe `WorldgenCompatibilityAdapter` during mod construction:

```java
WorldgenIntegrationRegistry.registerAdapter(myAdapter);
```

Adapter selection is deterministic: higher priority wins and equal priorities are ordered by
adapter ID. Duplicate IDs fail immediately. Registration freezes when the first context is
created, so late registration is rejected instead of changing worldgen during a server run.

The built-in `u_api:vanilla_noise` adapter supports Minecraft's
`NoiseBasedChunkGenerator`. It obtains height through `ChunkGenerator#getBaseHeight` and biomes
through the active `BiomeSource`, so data packs which replace noise settings are observed without
special imports. If no adapter accepts a generator, `u_api:unsupported` is selected with no
capabilities. A consuming mod should compare `context.capabilities()` with its required set and
disable only its own generation when capabilities are missing.

Do not create a named Tectonic, Biomes O' Plenty or other third-party adapter without compiling
and testing against that mod's real compatible API. Optional adapters belong in the integration
mod which has that optional dependency; U-API itself has no hard dependency on a worldgen mod.

## Biome classification

The default classifier recognizes both Minecraft biome tags and NeoForge common `c:` tags:

- ocean: `#u_api:is_ocean`, `#minecraft:is_ocean`, `#c:is_ocean`;
- coast: `#u_api:is_coast`, `#minecraft:is_beach`, `#c:is_beach`;
- land: only `#u_api:is_land` or a registered classifier.

The U-API tags are data-pack-extensible. `#u_api:is_land` is intentionally empty by default:
an unknown modded biome must remain `UNKNOWN`, not silently become land merely because it is not
tagged as ocean.

Adapters can supplement classification through `biomeClassifier()`. Other integrations can add a
classifier independently:

```java
WorldgenIntegrationRegistry.registerBiomeClassifier(id, priority, classifier);
```

Explicit tags are evaluated first, registered classifiers next, and the selected adapter's
classifier last. A classifier should return `UNKNOWN` when it has no opinion.

## Determinism and threading

- Use absolute block coordinates for every adapter query and terrain calculation.
- Do not load neighboring chunks or retain a `ServerLevel` in an adapter.
- Do not use mutable global worldgen state.
- Treat adapters and classifiers as concurrent, pure query services.
- Keep location-specific geometry, materials, features and debug commands in the consuming mod.

The API deliberately does not yet publish generic terrain/density/surface modifier interfaces or
stage events. Those contracts require a real shared dispatcher and stable stage semantics; marker
interfaces without an execution path would not provide compatibility.
