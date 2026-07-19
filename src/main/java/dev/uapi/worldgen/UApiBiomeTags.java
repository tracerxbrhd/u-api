package dev.uapi.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/** Data-pack-extensible biome classes used by the default worldgen classifier. */
public final class UApiBiomeTags {
    private static final String MOD_ID = "u_api";

    public static final TagKey<Biome> IS_OCEAN = create("is_ocean");
    public static final TagKey<Biome> IS_COAST = create("is_coast");
    public static final TagKey<Biome> IS_LAND = create("is_land");

    private UApiBiomeTags() {
    }

    private static TagKey<Biome> create(String path) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MOD_ID, path));
    }
}
