package dev.uapi.worldgen;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Immutable capability set returned by a {@link WorldgenCompatibilityAdapter}. */
public final class WorldgenCapabilities {
    private static final WorldgenCapabilities NONE = new WorldgenCapabilities(Set.of());

    private final Set<WorldgenCapability> values;

    public WorldgenCapabilities(Collection<WorldgenCapability> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            this.values = Set.of();
            return;
        }
        EnumSet<WorldgenCapability> copy = EnumSet.copyOf(values);
        this.values = Collections.unmodifiableSet(copy);
    }

    public static WorldgenCapabilities none() {
        return NONE;
    }

    public static WorldgenCapabilities of(WorldgenCapability... values) {
        Objects.requireNonNull(values, "values");
        return values.length == 0 ? NONE : new WorldgenCapabilities(Arrays.asList(values));
    }

    public Set<WorldgenCapability> values() {
        return this.values;
    }

    public boolean supports(WorldgenCapability capability) {
        return this.values.contains(Objects.requireNonNull(capability, "capability"));
    }

    public boolean supportsAll(WorldgenCapabilities required) {
        Objects.requireNonNull(required, "required");
        return this.values.containsAll(required.values);
    }

    public Set<WorldgenCapability> missing(WorldgenCapabilities required) {
        Objects.requireNonNull(required, "required");
        if (this.supportsAll(required)) {
            return Set.of();
        }
        EnumSet<WorldgenCapability> missing = EnumSet.copyOf(required.values);
        missing.removeAll(this.values);
        return Collections.unmodifiableSet(missing);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof WorldgenCapabilities capabilities && this.values.equals(capabilities.values);
    }

    @Override
    public int hashCode() {
        return this.values.hashCode();
    }

    @Override
    public String toString() {
        return this.values.toString();
    }
}
