package dev.uapi.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class WorldgenCapabilitiesTest {
    @Test
    void copiesTheInputAndExposesAnImmutableSet() {
        EnumSet<WorldgenCapability> input = EnumSet.of(WorldgenCapability.BASE_BIOME_SAMPLE);
        WorldgenCapabilities capabilities = new WorldgenCapabilities(input);

        input.add(WorldgenCapability.BASE_HEIGHT_SAMPLE);

        assertEquals(Set.of(WorldgenCapability.BASE_BIOME_SAMPLE), capabilities.values());
        assertThrows(UnsupportedOperationException.class,
            () -> capabilities.values().add(WorldgenCapability.BASE_HEIGHT_SAMPLE));
    }

    @Test
    void reportsSupportedAndMissingCapabilities() {
        WorldgenCapabilities available = WorldgenCapabilities.of(
            WorldgenCapability.BASE_BIOME_SAMPLE,
            WorldgenCapability.BASE_HEIGHT_SAMPLE
        );
        WorldgenCapabilities required = WorldgenCapabilities.of(
            WorldgenCapability.BASE_HEIGHT_SAMPLE,
            WorldgenCapability.FINAL_DENSITY_DECORATION
        );

        assertTrue(available.supports(WorldgenCapability.BASE_BIOME_SAMPLE));
        assertFalse(available.supportsAll(required));
        assertEquals(Set.of(WorldgenCapability.FINAL_DENSITY_DECORATION), available.missing(required));
        assertTrue(available.missing(WorldgenCapabilities.none()).isEmpty());
    }

    @Test
    void emptyFactoriesShareTheSameValueSemantics() {
        assertEquals(WorldgenCapabilities.none(), WorldgenCapabilities.of());
        assertTrue(WorldgenCapabilities.none().values().isEmpty());
    }
}
