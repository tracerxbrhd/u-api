# U-API 2 foundation contracts

This document describes the public foundation shipped in U-API 2.0.0. Earlier 1.x and local 1.4
contracts are outside the source/binary compatibility promise; current worldgen contracts are
documented separately.

## Package boundary

- `dev.uapi.api.*` contains common, server-safe public contracts.
- `dev.uapi.client.ui.*` contains physical-client UI types and must never be referenced from a
  common mod initializer.
- `dev.uapi.internal.*` is implementation detail. Consumers must not import it.

U-API never imports consumer-mod classes such as Soul Ascension or Dedicated Dungeons.

## Scoped services

Services publish an interface extending `UApiService` and a stable namespaced ID.

```java
public interface ExampleService extends UApiService {
    Optional<String> findValue(UUID playerId);
}

ServiceRegistration registration = UApiServices.register(
    ExampleService.class,
    implementation,
    ServiceScope.SERVER
);

Optional<ExampleService> service = UApiServices.find(ExampleService.class);
```

Rules:

- only an explicit interface can be published;
- duplicate contracts and duplicate service IDs are rejected;
- `ServiceRegistration.close()` removes exactly the registration which created it;
- `SERVER` registrations are cleared after server stop;
- `CLIENT_CONNECTION` registrations are cleared on client logout;
- `GLOBAL` registrations live until their handle is closed or the process exits;
- `diagnosticSnapshot()` never exposes mutable service instances.

## Social groups

`SocialGroupService` exposes immutable Party, Guild, Squad, or custom snapshots. Consumers use
UUIDs and portable role flags; provider-owned role and Guild objects never cross the boundary.

Dedicated Dungeons should use this lookup order:

1. find `SocialGroupService` through `UApiServices`;
2. use `getActiveParty(playerId)` when present;
3. validate the returned roster again at launch time;
4. fall back to solo mode when no provider or active party exists.

`ReadyCheckService` is separate from social lookup. `ReadyCheckStateMachine` provides the canonical
pure transition rules for READY, NOT_READY, DECLINED, timeout, cancellation, and ALL_READY. A
provider still owns the authoritative clock, membership checks, concurrent-check policy, storage,
and packet distribution.

## Permissions

`PermissionService` evaluates a `PermissionKey` against a `PermissionContext`. The context contains
an actor, optional target, namespaced action, and optional generic resource. Decisions contain a
stable reason ID, not a localized message.

Implementations may extend the internal caching foundation, but integrations should depend only on
the public interface. Every role, membership, relation, or territory change must invalidate the
affected actor decisions.

## Network primitives

- `ProtocolVersion` negotiates equal major versions at the lower minor feature level.
- `RequestId` correlates one request with one acknowledgement or error.
- `OperationAck` and `OperationError` carry stable codes and no localized text.
- `ActorActionRateLimiter` is an exact bounded sliding-window limiter.
- `ConnectionRequestTracker` bounds pending operations, rejects duplicate IDs, expires timeouts,
  requires the exact connection for completion, and supports connection cleanup.

These primitives do not register NeoForge payloads by themselves. Each owning mod registers its
payload codecs and executes server validation before mutating state.

## Retained UI lifecycle

Client screens extend `UIScreen` and build their retained tree once per screen lifetime.

```java
public final class ExampleScreen extends UIScreen {
    private UILabel heading;

    @Override
    protected void buildUi(UIContainer root) {
        heading = root.add(new UILabel(title, 0xFFFFFFFF));
    }

    @Override
    protected void layoutUi(UIContainer root) {
        heading.setBounds(12, 12, width - 24, font.lineHeight);
    }

    @Override
    protected void renderScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw screen-specific retained data. Never send packets from this method.
    }
}
```

`UIState` supports batched notifications. Component subscriptions created with `observe(...)` are
closed automatically when the component unmounts. `UIScreen.removed()` cannot be bypassed by a
subclass; it always disposes the tree, including a partially failed mount. Hiding or disabling a
container releases focus held by any descendant. A screen may expose `activeUiComponentCount()` for
diagnostics.

Focusable retained controls participate in deterministic depth-first Tab/Shift+Tab traversal.
Taking retained focus clears Minecraft's native widget focus, while clicking a native widget clears
retained focus, so both input systems cannot receive the same key event.

The current Soul Ascension vertical slice moves `CharacterScreen` onto this lifecycle without
changing its gameplay, payload IDs, registry IDs, or visual theme. Later slices should replace the
remaining render-built hitboxes and lists with retained/virtualized components.

### Layout, theme and component library

Every `UIContainer` owns a `UILayout`. Built-in deterministic layouts cover row, column, grid,
stack, nine-point anchor, two-panel split, responsive breakpoint selection and absolute overlays.
`UIScrollContainer` owns bounded scroll state, while `UIVirtualizedList` renders only visible rows
and deliberately does not create one child component per item. Excessive padding, gaps and split
dividers are clamped to the available GUI-space bounds rather than placing zero-sized children
outside their container.

`UITheme` exposes semantic color, spacing, radius and animation tokens. A consuming mod should set
its theme on the screen root (`root.setTheme(...)`); descendants inherit it. Consumer mods can
therefore keep separate palettes without forking controls or layout code.

The reusable component package includes retained buttons and icon buttons, text/search fields,
checkboxes, sliders, dropdowns, tabs, rich text, scrolling/virtual lists, player summaries, role
and permission controls, progress bars, tooltip/context menu/modal primitives, confirmations,
bounded toast hosting, and empty/loading/error states. Input focus is owned by `UIScreen` and is
released on removal, so a removed text field cannot keep receiving keyboard events. Buttons,
checkboxes, sliders, dropdowns, tabs, text fields, retry states and virtual lists expose baseline
keyboard operation without constructing native widgets each frame.

## HUD framework

Client modules register a single long-lived `HudElement` with `UApiHud.register(...)` and keep the
returned registration handle. `visible(HudTickContext)` is evaluated before layout, so inactive
elements do not reserve space. `width()` and `height()` are sampled for every layout and may change
with the element's view model.

`HudPlacement` provides nine anchors, offsets, bounded scale, visibility and priority. Layout avoids
both elements placed earlier and dynamic `HudReservedAreaProvider` rectangles; higher-priority
elements are placed first, and U-API reserves the vanilla hotbar by default. Width and height are
sampled once per render pass, and screen clipping is reflected in the bounds passed to the element.
Client config can globally hide registered HUD without unregistering it.

An element or reserved-area provider that throws is disabled for that registration and logged once;
other mods' HUD remains operational. Closing and re-registering the handle clears that failure state.

## World overlays

Packet handlers create immutable `WorldOverlayMarker` snapshots and call
`UApiWorldOverlays.upsert(marker)`. Reusing a marker UUID interpolates from its current rendered
position. Markers are connection-scoped and cleared on logout; renderer registrations remain
global and have explicit close handles.

The framework filters dimension and min/max distance, clamps screen size, projects off-screen edge
indicators, applies priorities and distance LOD, caches block-occlusion checks, honors per-category
client visibility, and prunes expired markers. Marker type and visibility category are namespaced
IDs, so the framework has no consumer-mod dependency. Consumers can register a type-specific
`WorldOverlayRenderer`; unknown types use a small neutral fallback marker.

The connection store is bounded by the client `maxWorldOverlays` preference (default 512, hard
range 16..4096). At capacity, an incoming marker replaces the oldest lowest-priority marker; it is
rejected by `tryUpsert(...)` when every retained marker has a higher priority. Changing dimension for
the same marker ID starts at the new position instead of interpolating across dimensions. A failed
custom renderer is logged once and replaced by the neutral fallback. A failed visibility resolver
hides markers (fail closed) rather than accidentally revealing them.

Overlay privacy remains server-authoritative: this client framework renders only snapshots it was
given. Providers must perform relationship, permission and privacy checks before encoding a marker;
client visibility preferences are not an authorization boundary.

## Diagnostics

Diagnostics default to disabled in common config. Hot-path timings and counters exit before reading
the clock while disabled. When enabled, `UApiDiagnostics.snapshot()` includes UI layout/render
timings, mounted component and overlay counts, invalidations, head-cache statistics, packet rates,
the immutable service registry snapshot, and optional scalar gauges. A module can expose values
such as force-loaded chunk count with `registerGauge(...)` and close the handle with its lifecycle.
Operators can inspect the common snapshot with `/uapi api diagnostics`; client-only values naturally
remain local to the client process.

Packet rates use the most recent enabled collection window and stop aging after diagnostics are
disabled. Extension gauge failures are isolated from the snapshot command.
