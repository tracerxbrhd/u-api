package dev.uapi.worldgen;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Loader-safe bridge between ecosystem worldgen modules and one generator implementation.
 *
 * <p>Adapters are registered during mod construction and must be stateless and thread-safe.
 * Optional integrations should keep third-party classes in their own mod and register an
 * adapter only when that mod is available.</p>
 */
public interface WorldgenCompatibilityAdapter {
    ResourceLocation id();

    default int priority() {
        return 0;
    }

    boolean supports(ResourceKey<Level> dimension, ChunkGenerator generator);

    WorldgenCapabilities capabilities(ResourceKey<Level> dimension, ChunkGenerator generator);

    OptionalInt baseHeight(WorldgenContext context, int blockX, int blockZ, Heightmap.Types type);

    Optional<Holder<Biome>> baseBiome(WorldgenContext context, int blockX, int blockY, int blockZ);

    default BiomeClassifier biomeClassifier() {
        return (context, biome) -> BiomeClassifier.BiomeClass.UNKNOWN;
    }

    default String compatibilityDetails() {
        return "";
    }
}
