package dev.uapi.worldgen;

import dev.uapi.worldgen.BiomeClassifier.BiomeClass;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Immutable per-level snapshot for deterministic, chunk-load-free worldgen queries.
 *
 * <p>The referenced Minecraft generator objects are the same read-only runtime objects used by
 * vanilla generation. Consumers must not retain a {@code ServerLevel} or request chunks from a
 * worldgen worker thread.</p>
 */
public final class WorldgenContext {
    private final ResourceKey<Level> dimension;
    private final long seed;
    private final int minBuildHeight;
    private final int height;
    private final int seaLevel;
    private final RegistryAccess registryAccess;
    private final ChunkGenerator generator;
    private final BiomeSource biomeSource;
    private final RandomState randomState;
    private final LevelHeightAccessor heightAccessor;
    private final ResourceLocation adapterId;
    private final WorldgenCompatibilityAdapter adapter;
    private final WorldgenCapabilities capabilities;
    private final BiomeClassifier classifier;

    WorldgenContext(
        ResourceKey<Level> dimension,
        long seed,
        int minBuildHeight,
        int height,
        int seaLevel,
        RegistryAccess registryAccess,
        ChunkGenerator generator,
        RandomState randomState,
        ResourceLocation adapterId,
        WorldgenCompatibilityAdapter adapter,
        WorldgenCapabilities capabilities,
        BiomeClassifier classifier
    ) {
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.registryAccess = Objects.requireNonNull(registryAccess, "registryAccess");
        this.generator = Objects.requireNonNull(generator, "generator");
        this.biomeSource = Objects.requireNonNull(generator.getBiomeSource(), "generator biomeSource");
        this.randomState = Objects.requireNonNull(randomState, "randomState");
        this.adapterId = Objects.requireNonNull(adapterId, "adapterId");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.classifier = Objects.requireNonNull(classifier, "classifier");
        if (height <= 0) {
            throw new IllegalArgumentException("Worldgen height must be positive");
        }
        this.seed = seed;
        this.minBuildHeight = minBuildHeight;
        this.height = height;
        this.seaLevel = seaLevel;
        this.heightAccessor = LevelHeightAccessor.create(minBuildHeight, height);
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public long seed() {
        return this.seed;
    }

    public int minBuildHeight() {
        return this.minBuildHeight;
    }

    public int maxBuildHeight() {
        return this.minBuildHeight + this.height;
    }

    public int height() {
        return this.height;
    }

    public int seaLevel() {
        return this.seaLevel;
    }

    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    public ChunkGenerator generator() {
        return this.generator;
    }

    public BiomeSource biomeSource() {
        return this.biomeSource;
    }

    public RandomState randomState() {
        return this.randomState;
    }

    public LevelHeightAccessor heightAccessor() {
        return this.heightAccessor;
    }

    public WorldgenCompatibilityAdapter adapter() {
        return this.adapter;
    }

    public ResourceLocation adapterId() {
        return this.adapterId;
    }

    public WorldgenCapabilities capabilities() {
        return this.capabilities;
    }

    public BiomeClassifier classifier() {
        return this.classifier;
    }

    public OptionalInt baseHeight(int blockX, int blockZ, Heightmap.Types type) {
        Objects.requireNonNull(type, "type");
        if (!this.capabilities.supports(WorldgenCapability.BASE_HEIGHT_SAMPLE)) {
            return OptionalInt.empty();
        }
        try {
            OptionalInt result = this.adapter.baseHeight(this, blockX, blockZ, type);
            return result == null ? OptionalInt.empty() : result;
        } catch (RuntimeException | LinkageError exception) {
            WorldgenIntegrationRegistry.reportFailure(this.adapterId, "base_height", exception);
            return OptionalInt.empty();
        }
    }

    public Optional<Holder<Biome>> baseBiome(int blockX, int blockY, int blockZ) {
        if (!this.capabilities.supports(WorldgenCapability.BASE_BIOME_SAMPLE)) {
            return Optional.empty();
        }
        try {
            Optional<Holder<Biome>> result = this.adapter.baseBiome(this, blockX, blockY, blockZ);
            return result == null ? Optional.empty() : result;
        } catch (RuntimeException | LinkageError exception) {
            WorldgenIntegrationRegistry.reportFailure(this.adapterId, "base_biome", exception);
            return Optional.empty();
        }
    }

    public BiomeClass classify(Holder<Biome> biome) {
        Objects.requireNonNull(biome, "biome");
        try {
            BiomeClass result = this.classifier.classify(this, biome);
            return result == null ? BiomeClass.UNKNOWN : result;
        } catch (RuntimeException | LinkageError exception) {
            WorldgenIntegrationRegistry.reportFailure(this.adapterId, "biome_classification", exception);
            return BiomeClass.UNKNOWN;
        }
    }

    public BiomeClass classifyBaseBiome(int blockX, int blockY, int blockZ) {
        return this.baseBiome(blockX, blockY, blockZ)
            .map(this::classify)
            .orElse(BiomeClass.UNKNOWN);
    }
}
