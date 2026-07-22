package dev.uapi.worldgen;

import com.mojang.logging.LogUtils;
import dev.uapi.worldgen.BiomeClassifier.BiomeClass;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.neoforged.neoforge.common.Tags;
import org.slf4j.Logger;

/**
 * Deterministic registry for executable worldgen adapters and supplemental biome classifiers.
 * Registration closes when the first level context is created.
 */
public final class WorldgenIntegrationRegistry {
    private static final String MOD_ID = "u_api";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Comparator<AdapterEntry> ADAPTER_ORDER = Comparator
        .comparingInt(AdapterEntry::priority).reversed()
        .thenComparing(entry -> entry.id().toString());
    private static final Comparator<ClassifierEntry> CLASSIFIER_ORDER = Comparator
        .comparingInt(ClassifierEntry::priority).reversed()
        .thenComparing(entry -> entry.id().toString());

    private static final Map<Identifier, AdapterEntry> ADAPTERS = new LinkedHashMap<>();
    private static final Map<Identifier, ClassifierEntry> CLASSIFIERS = new LinkedHashMap<>();
    private static final Set<String> REPORTED_FAILURES = ConcurrentHashMap.newKeySet();
    private static final WorldgenCompatibilityAdapter UNSUPPORTED = new UnsupportedWorldgenAdapter();
    private static boolean frozen;

    static {
        registerBuiltIn(new VanillaNoiseWorldgenAdapter());
    }

    private WorldgenIntegrationRegistry() {
    }

    /** Forces class initialization without freezing registration. */
    public static void bootstrap() {
    }

    public static synchronized void registerAdapter(WorldgenCompatibilityAdapter adapter) {
        ensureMutable();
        registerEntry(adapter);
    }

    public static synchronized void registerBiomeClassifier(
        Identifier id,
        int priority,
        BiomeClassifier classifier
    ) {
        ensureMutable();
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(classifier, "classifier");
        if (CLASSIFIERS.putIfAbsent(id, new ClassifierEntry(id, priority, classifier)) != null) {
            throw new IllegalStateException("Worldgen biome classifier already registered: " + id);
        }
    }

    public static synchronized List<WorldgenCompatibilityAdapter> adapters() {
        return sortedAdapters().stream().map(AdapterEntry::adapter).toList();
    }

    public static WorldgenContext createContext(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        RegistrySnapshot snapshot = freezeAndSnapshot();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        ResourceKey<Level> dimension = level.dimension();
        AdapterSelection selection = select(snapshot.adapters(), dimension, generator);
        BiomeClassifier classifier = composeClassifier(selection.id(), selection.adapter(), snapshot.classifiers());
        return new WorldgenContext(
            dimension,
            level.getSeed(),
            level.getMinY(),
            level.getHeight(),
            generator.getSeaLevel(),
            level.registryAccess(),
            generator,
            level.getChunkSource().randomState(),
            selection.id(),
            selection.adapter(),
            selection.capabilities(),
            classifier
        );
    }

    static void reportFailure(Identifier integrationId, String operation, Throwable exception) {
        String failureKey = integrationId + "#" + operation;
        if (REPORTED_FAILURES.add(failureKey)) {
            LOGGER.warn(
                "Worldgen integration {} failed during {}; this failure will not be logged again",
                integrationId,
                operation,
                exception
            );
        }
    }

    private static synchronized RegistrySnapshot freezeAndSnapshot() {
        frozen = true;
        return new RegistrySnapshot(sortedAdapters(), sortedClassifiers());
    }

    private static List<AdapterEntry> sortedAdapters() {
        List<AdapterEntry> entries = new ArrayList<>(ADAPTERS.values());
        entries.sort(ADAPTER_ORDER);
        return List.copyOf(entries);
    }

    private static List<ClassifierEntry> sortedClassifiers() {
        List<ClassifierEntry> entries = new ArrayList<>(CLASSIFIERS.values());
        entries.sort(CLASSIFIER_ORDER);
        return List.copyOf(entries);
    }

    private static AdapterSelection select(
        List<AdapterEntry> adapters,
        ResourceKey<Level> dimension,
        ChunkGenerator generator
    ) {
        for (AdapterEntry entry : adapters) {
            try {
                if (!entry.adapter().supports(dimension, generator)) {
                    continue;
                }
                WorldgenCapabilities capabilities = Objects.requireNonNull(
                    entry.adapter().capabilities(dimension, generator),
                    "adapter capabilities"
                );
                return new AdapterSelection(entry.id(), entry.adapter(), capabilities);
            } catch (RuntimeException | LinkageError exception) {
                reportFailure(entry.id(), "selection", exception);
            }
        }
        return new AdapterSelection(
            UnsupportedWorldgenAdapter.ID,
            UNSUPPORTED,
            WorldgenCapabilities.none()
        );
    }

    private static BiomeClassifier composeClassifier(
        Identifier adapterId,
        WorldgenCompatibilityAdapter adapter,
        List<ClassifierEntry> classifiers
    ) {
        BiomeClassifier adapterClassifier;
        try {
            adapterClassifier = Objects.requireNonNull(adapter.biomeClassifier(), "adapter biomeClassifier");
        } catch (RuntimeException | LinkageError exception) {
            reportFailure(adapterId, "classifier_creation", exception);
            adapterClassifier = (context, biome) -> BiomeClass.UNKNOWN;
        }
        BiomeClassifier selectedAdapterClassifier = adapterClassifier;
        return (context, biome) -> {
            BiomeClass tagged = classifyTags(context, biome);
            if (tagged != BiomeClass.UNKNOWN) {
                return tagged;
            }
            for (ClassifierEntry entry : classifiers) {
                BiomeClass result = safeClassify(entry.id(), entry.classifier(), context, biome);
                if (result != BiomeClass.UNKNOWN) {
                    return result;
                }
            }
            return safeClassify(adapterId, selectedAdapterClassifier, context, biome);
        };
    }

    private static BiomeClass classifyTags(WorldgenContext context, Holder<Biome> biome) {
        if (biome.is(UApiBiomeTags.IS_OCEAN)
            || biome.is(BiomeTags.IS_OCEAN)
            || biome.is(Tags.Biomes.IS_OCEAN)) {
            return BiomeClass.OCEAN;
        }
        if (biome.is(UApiBiomeTags.IS_COAST)
            || biome.is(BiomeTags.IS_BEACH)
            || biome.is(Tags.Biomes.IS_BEACH)) {
            return BiomeClass.COAST;
        }
        return biome.is(UApiBiomeTags.IS_LAND) ? BiomeClass.LAND : BiomeClass.UNKNOWN;
    }

    private static BiomeClass safeClassify(
        Identifier id,
        BiomeClassifier classifier,
        WorldgenContext context,
        Holder<Biome> biome
    ) {
        try {
            BiomeClass result = classifier.classify(context, biome);
            return result == null ? BiomeClass.UNKNOWN : result;
        } catch (RuntimeException | LinkageError exception) {
            reportFailure(id, "biome_classifier", exception);
            return BiomeClass.UNKNOWN;
        }
    }

    private static void registerBuiltIn(WorldgenCompatibilityAdapter adapter) {
        registerEntry(adapter);
    }

    private static void registerEntry(WorldgenCompatibilityAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        Identifier id = Objects.requireNonNull(adapter.id(), "adapter id");
        AdapterEntry entry = new AdapterEntry(id, adapter.priority(), adapter);
        if (ADAPTERS.putIfAbsent(id, entry) != null) {
            throw new IllegalStateException("Worldgen adapter already registered: " + id);
        }
    }

    private static void ensureMutable() {
        if (frozen) {
            throw new IllegalStateException("Worldgen integration registration is already frozen");
        }
    }

    private record AdapterEntry(
        Identifier id,
        int priority,
        WorldgenCompatibilityAdapter adapter
    ) {
    }

    private record ClassifierEntry(
        Identifier id,
        int priority,
        BiomeClassifier classifier
    ) {
    }

    private record RegistrySnapshot(
        List<AdapterEntry> adapters,
        List<ClassifierEntry> classifiers
    ) {
    }

    private record AdapterSelection(
        Identifier id,
        WorldgenCompatibilityAdapter adapter,
        WorldgenCapabilities capabilities
    ) {
    }

    private static final class VanillaNoiseWorldgenAdapter implements WorldgenCompatibilityAdapter {
        private static final Identifier ID = Identifier.fromNamespaceAndPath(MOD_ID, "vanilla_noise");
        private static final WorldgenCapabilities CAPABILITIES = WorldgenCapabilities.of(
            WorldgenCapability.BASE_BIOME_SAMPLE,
            WorldgenCapability.BASE_HEIGHT_SAMPLE,
            WorldgenCapability.FINAL_DENSITY_DECORATION,
            WorldgenCapability.BIOME_REPLACEMENT,
            WorldgenCapability.SURFACE_REPLACEMENT,
            WorldgenCapability.POST_CARVER_MUTATION,
            WorldgenCapability.POST_DECORATION_MUTATION,
            WorldgenCapability.STRUCTURE_FILTERING
        );

        @Override
        public Identifier id() {
            return ID;
        }

        @Override
        public int priority() {
            return -1_000;
        }

        @Override
        public boolean supports(ResourceKey<Level> dimension, ChunkGenerator generator) {
            return generator instanceof NoiseBasedChunkGenerator;
        }

        @Override
        public WorldgenCapabilities capabilities(ResourceKey<Level> dimension, ChunkGenerator generator) {
            return CAPABILITIES;
        }

        @Override
        public OptionalInt baseHeight(
            WorldgenContext context,
            int blockX,
            int blockZ,
            Heightmap.Types type
        ) {
            return OptionalInt.of(context.generator().getBaseHeight(
                blockX,
                blockZ,
                type,
                context.heightAccessor(),
                context.randomState()
            ));
        }

        @Override
        public Optional<Holder<Biome>> baseBiome(
            WorldgenContext context,
            int blockX,
            int blockY,
            int blockZ
        ) {
            return Optional.of(context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(blockX),
                QuartPos.fromBlock(blockY),
                QuartPos.fromBlock(blockZ),
                context.randomState().sampler()
            ));
        }

        @Override
        public String compatibilityDetails() {
            return "Vanilla NoiseBasedChunkGenerator pipeline";
        }
    }

    private static final class UnsupportedWorldgenAdapter implements WorldgenCompatibilityAdapter {
        private static final Identifier ID = Identifier.fromNamespaceAndPath(MOD_ID, "unsupported");

        @Override
        public Identifier id() {
            return ID;
        }

        @Override
        public boolean supports(ResourceKey<Level> dimension, ChunkGenerator generator) {
            return true;
        }

        @Override
        public WorldgenCapabilities capabilities(ResourceKey<Level> dimension, ChunkGenerator generator) {
            return WorldgenCapabilities.none();
        }

        @Override
        public OptionalInt baseHeight(
            WorldgenContext context,
            int blockX,
            int blockZ,
            Heightmap.Types type
        ) {
            return OptionalInt.empty();
        }

        @Override
        public Optional<Holder<Biome>> baseBiome(
            WorldgenContext context,
            int blockX,
            int blockY,
            int blockZ
        ) {
            return Optional.empty();
        }

        @Override
        public String compatibilityDetails() {
            return "No registered adapter accepted this chunk generator";
        }
    }
}
