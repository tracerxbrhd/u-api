package dev.uapi.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/** Thread-safe biome classification hook used without loading or generating chunks. */
@FunctionalInterface
public interface BiomeClassifier {
    BiomeClass classify(WorldgenContext context, Holder<Biome> biome);

    enum BiomeClass {
        OCEAN,
        COAST,
        LAND,
        UNKNOWN
    }
}
