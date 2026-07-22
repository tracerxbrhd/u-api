package dev.uapi.client.hud;

import dev.uapi.UApi;
import dev.uapi.client.ui.core.UIBounds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Client registry, placement preferences and collision-aware HUD renderer. */
public final class UApiHud {
    private static final Object LOCK = new Object();
    private static final Map<Identifier, Entry> ELEMENTS = new HashMap<>();
    private static final Map<Identifier, AreaEntry> RESERVED_AREAS = new HashMap<>();
    private static final Map<Identifier, HudPlacement> OVERRIDES = new HashMap<>();
    private static final Set<Identifier> FAILED_ELEMENTS = new HashSet<>();
    private static final Set<Identifier> FAILED_AREAS = new HashSet<>();
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private static final Comparator<Entry> RENDER_ORDER = Comparator
        .comparingInt((Entry entry) -> entry.placement().priority()).reversed()
        .thenComparing(entry -> entry.id().toString());

    static {
        registerReservedArea(Identifier.fromNamespaceAndPath(UApi.MOD_ID, "vanilla_hotbar"), (width, height) ->
            List.of(new UIBounds(Math.max(0, (width - 182) / 2), Math.max(0, height - 28),
                Math.min(182, Math.max(0, width)), Math.min(28, Math.max(0, height)))));
    }

    private UApiHud() {
    }

    public static HudElementRegistration register(HudElement element) {
        Objects.requireNonNull(element, "element");
        Identifier id = Objects.requireNonNull(element.id(), "element.id()");
        HudPlacement defaultPlacement = Objects.requireNonNull(element.defaultPlacement(), "element.defaultPlacement()");
        Entry entry = new Entry(NEXT_ID.incrementAndGet(), id, element, defaultPlacement,
            1, 1);
        synchronized (LOCK) {
            if (ELEMENTS.putIfAbsent(id, entry) != null) throw new IllegalStateException("HUD element already registered: " + id);
            FAILED_ELEMENTS.remove(id);
        }
        return new ElementHandle(id, entry);
    }

    public static HudReservedAreaRegistration registerReservedArea(Identifier id, HudReservedAreaProvider provider) {
        Objects.requireNonNull(id, "id");
        AreaEntry entry = new AreaEntry(NEXT_ID.incrementAndGet(), Objects.requireNonNull(provider, "provider"));
        synchronized (LOCK) {
            if (RESERVED_AREAS.putIfAbsent(id, entry) != null) throw new IllegalStateException("HUD area already registered: " + id);
            FAILED_AREAS.remove(id);
        }
        return new AreaHandle(id, entry);
    }

    /** Applies a client preference. Persistence belongs to the owning mod/config screen. */
    public static void setPlacement(Identifier elementId, HudPlacement placement) {
        Objects.requireNonNull(elementId, "elementId");
        Objects.requireNonNull(placement, "placement");
        synchronized (LOCK) {
            if (!ELEMENTS.containsKey(elementId)) throw new IllegalArgumentException("Unknown HUD element: " + elementId);
            OVERRIDES.put(elementId, placement);
        }
    }

    public static void resetPlacement(Identifier elementId) {
        synchronized (LOCK) {
            OVERRIDES.remove(Objects.requireNonNull(elementId, "elementId"));
        }
    }

    public static Map<Identifier, HudPlacement> placements() {
        synchronized (LOCK) {
            Map<Identifier, HudPlacement> result = new HashMap<>();
            ELEMENTS.forEach((id, entry) -> result.put(id, OVERRIDES.getOrDefault(id, entry.defaultPlacement())));
            return Map.copyOf(result);
        }
    }

    public static void tick(Minecraft minecraft) {
        HudTickContext context = new HudTickContext(minecraft);
        for (Entry entry : snapshotEntries()) {
            if (failed(entry.id())) continue;
            try {
                entry.element().tick(context);
            } catch (RuntimeException exception) {
                disableFailed(entry, "tick", exception);
            }
        }
    }

    public static void render(Minecraft minecraft, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        HudTickContext tickContext = new HudTickContext(minecraft);
        List<Entry> entries = snapshotEntries().stream()
            .filter(entry -> !failed(entry.id()))
            .filter(entry -> entry.placement().visible())
            .filter(entry -> visible(entry, tickContext))
            .map(UApiHud::sampleDimensions)
            .filter(Objects::nonNull)
            .toList();
        Collection<UIBounds> reserved = snapshotReserved(graphics.guiWidth(), graphics.guiHeight());
        List<HudLayoutEngine.PlacedElement> placedElements = HudLayoutEngine.layout(entries, reserved,
            graphics.guiWidth(), graphics.guiHeight());
        // Placement runs high priority first; drawing reverses that order so it also wins unavoidable overlap.
        for (int index = placedElements.size() - 1; index >= 0; index--) {
            HudLayoutEngine.PlacedElement placed = placedElements.get(index);
            Entry entry = placed.entry();
            HudPlacement placement = entry.placement();
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate(placed.bounds().x(), placed.bounds().y());
                graphics.pose().scale(placement.scale(), placement.scale());
                try {
                    int localWidth = Math.min(entry.width(), (int) Math.ceil(placed.bounds().width() / placement.scale()));
                    int localHeight = Math.min(entry.height(), (int) Math.ceil(placed.bounds().height() / placement.scale()));
                    entry.element().render(new HudRenderContext(minecraft, graphics, deltaTracker,
                        new UIBounds(0, 0, Math.max(0, localWidth), Math.max(0, localHeight)), placement.scale()));
                } catch (RuntimeException exception) {
                    disableFailed(entry, "render", exception);
                }
            } finally {
                graphics.pose().popMatrix();
            }
        }
    }

    private static List<Entry> snapshotEntries() {
        synchronized (LOCK) {
            return ELEMENTS.entrySet().stream().map(mapEntry -> {
                Entry entry = mapEntry.getValue();
                return entry.withPlacement(OVERRIDES.getOrDefault(mapEntry.getKey(), entry.defaultPlacement()));
            }).sorted(RENDER_ORDER).toList();
        }
    }

    private static Collection<UIBounds> snapshotReserved(int width, int height) {
        List<Map.Entry<Identifier, AreaEntry>> providers;
        synchronized (LOCK) {
            providers = RESERVED_AREAS.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue())).toList();
        }
        List<UIBounds> result = new ArrayList<>();
        for (Map.Entry<Identifier, AreaEntry> entry : providers) {
            synchronized (LOCK) {
                if (FAILED_AREAS.contains(entry.getKey())) continue;
            }
            try {
                Collection<UIBounds> supplied = entry.getValue().provider().reservedAreas(width, height);
                if (supplied != null) supplied.stream().filter(Objects::nonNull).forEach(result::add);
            } catch (RuntimeException exception) {
                boolean firstFailure;
                synchronized (LOCK) {
                    firstFailure = FAILED_AREAS.add(entry.getKey());
                }
                if (firstFailure) UApi.LOGGER.error(
                    "Disabling failed HUD reserved-area provider " + entry.getKey(), exception);
            }
        }
        return result;
    }

    private static boolean visible(Entry entry, HudTickContext context) {
        try {
            return entry.element().visible(context);
        } catch (RuntimeException exception) {
            disableFailed(entry, "visibility", exception);
            return false;
        }
    }

    private static Entry sampleDimensions(Entry entry) {
        try {
            int width = entry.element().width();
            int height = entry.element().height();
            return width <= 0 || height <= 0 ? null : entry.withDimensions(width, height);
        } catch (RuntimeException exception) {
            disableFailed(entry, "size", exception);
            return null;
        }
    }

    private static boolean failed(Identifier id) {
        synchronized (LOCK) {
            return FAILED_ELEMENTS.contains(id);
        }
    }

    private static void disableFailed(Entry entry, String phase, RuntimeException exception) {
        boolean firstFailure;
        synchronized (LOCK) {
            firstFailure = FAILED_ELEMENTS.add(entry.id());
        }
        if (firstFailure) UApi.LOGGER.error(
            "Disabling failed HUD element " + entry.id() + " during " + phase, exception);
    }

    record Entry(long registrationId, Identifier id, HudElement element, HudPlacement placement,
                 int width, int height) {
        HudPlacement defaultPlacement() {
            return placement;
        }

        Entry withPlacement(HudPlacement placement) {
            return new Entry(registrationId, id, element, placement, width, height);
        }

        Entry withDimensions(int width, int height) {
            return new Entry(registrationId, id, element, placement, width, height);
        }
    }

    private record AreaEntry(long registrationId, HudReservedAreaProvider provider) {
    }

    private static final class ElementHandle implements HudElementRegistration {
        private final Identifier id;
        private final Entry expected;
        private final AtomicBoolean closed = new AtomicBoolean();

        private ElementHandle(Identifier id, Entry expected) { this.id = id; this.expected = expected; }
        @Override public Identifier id() { return id; }
        @Override public boolean isActive() { synchronized (LOCK) { return !closed.get() && ELEMENTS.get(id) == expected; } }
        @Override public void close() { if (closed.compareAndSet(false, true)) synchronized (LOCK) {
            if (ELEMENTS.remove(id, expected)) {
                OVERRIDES.remove(id);
                FAILED_ELEMENTS.remove(id);
            }
        } }
    }

    private static final class AreaHandle implements HudReservedAreaRegistration {
        private final Identifier id;
        private final AreaEntry expected;
        private final AtomicBoolean closed = new AtomicBoolean();
        private AreaHandle(Identifier id, AreaEntry expected) { this.id = id; this.expected = expected; }
        @Override public Identifier id() { return id; }
        @Override public boolean isActive() { synchronized (LOCK) {
            return !closed.get() && RESERVED_AREAS.get(id) == expected;
        } }
        @Override public void close() { if (closed.compareAndSet(false, true)) synchronized (LOCK) {
            if (RESERVED_AREAS.remove(id, expected)) FAILED_AREAS.remove(id);
        } }
    }
}
