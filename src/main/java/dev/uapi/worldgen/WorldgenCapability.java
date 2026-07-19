package dev.uapi.worldgen;

/**
 * Generator operations which an integration adapter can safely expose to ecosystem modules.
 *
 * <p>Capabilities describe support, not an instruction to mutate the whole world. Consumers
 * must still restrict every operation to their own deterministic region.</p>
 */
public enum WorldgenCapability {
    BASE_BIOME_SAMPLE,
    BASE_HEIGHT_SAMPLE,
    FINAL_DENSITY_DECORATION,
    BIOME_REPLACEMENT,
    SURFACE_REPLACEMENT,
    POST_CARVER_MUTATION,
    POST_DECORATION_MUTATION,
    STRUCTURE_FILTERING
}
