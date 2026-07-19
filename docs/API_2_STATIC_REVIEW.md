# U-API 2 client foundation static review

Date: 2026-07-13

Scope: the retained UI, HUD, world-overlay and diagnostics foundation as it existed during the
UI-only review. The release-wide persistence decision was made later: 2.0 is clean-install-only and
does not retain compatibility with earlier persisted-data formats (see the workspace `VERSIONING.md`).

## Hardened invariants

- Retained components now have single-container ownership; self-parenting, cycles and reuse in two
  unmounted trees are rejected before mount.
- Failed mounts dispose subscriptions and run cleanup. Unmount continues cleanup after focus or
  hook failures, and hidden/disabled ancestors release descendant focus.
- Native Minecraft widget focus and retained focus are mutually exclusive. Tab/Shift+Tab traversal
  is deterministic, with keyboard behavior for the basic interactive components.
- Padding, gaps, dividers, grid columns, bounds arithmetic, scroll ranges and duration values are
  bounded. Scissor state is restored with `finally` around extension render callbacks.
- Anchor metadata is pruned when children leave a container, avoiding retained references.
- HUD IDs/default placements and per-frame sizes are captured rather than repeatedly querying a
  mutable extension. Higher priority wins collision placement. A failed element or reserved-area
  provider is isolated and logged once for that registration.
- The overlay store is connection-scoped and bounded by `maxWorldOverlays`. Capacity eviction is
  priority-aware; cross-dimension updates do not interpolate; expired entries maintain diagnostic
  counts; failed renderers fall back; failed visibility checks hide markers.
- Overlay pixel size and time ranges are bounded. Occlusion cache intervals increase with distance
  LOD, and non-finite projections/distances are rejected.
- Diagnostics packet rates stop aging when collection is disabled. Listener failures and batch
  cleanup preserve the original state-update exception.

## Physical-side boundary

Client-side code is isolated under `dev.uapi.client.*`, the client-scoped lifecycle hook under
`dev.uapi.internal.services.client.*`, and the optional JEI plugin under
`dev.uapi.integration.jei.*`. The common API, non-client service backend, server package and common
mod initializer contain no imports of `net.minecraft.client` or `dev.uapi.client`.

## Deferred verification

Per the integration workflow, Gradle compilation, unit tests, game tests and dedicated-server smoke
remain intentionally deferred to the final combined verification pass. This review is source-level
and does not claim binary verification.
