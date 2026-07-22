package dev.uapi.reward;

import com.google.gson.JsonObject;
import dev.uapi.UApi;
import dev.uapi.config.UApiServerConfig;
import dev.uapi.event.UApiEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RewardRegistry {
    public static final Identifier ITEM = Identifier.fromNamespaceAndPath("minecraft", "item");
    public static final Identifier EXPERIENCE = Identifier.fromNamespaceAndPath("minecraft", "experience");
    private static final Map<Identifier, RewardProvider> PROVIDERS = new ConcurrentHashMap<>();
    private static volatile Map<Identifier, RewardPool> pools = Map.of();
    private static boolean bootstrapped;

    private RewardRegistry() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        registerProvider(ITEM, RewardRegistry::grantItem);
        registerProvider(EXPERIENCE, RewardRegistry::grantExperience);
    }

    public static void registerProvider(Identifier type, RewardProvider provider) {
        if (PROVIDERS.putIfAbsent(type, provider) != null)
            throw new IllegalStateException("Reward provider already registered: " + type);
    }

    public static void replacePools(Map<Identifier, RewardPool> loaded) {
        pools = Map.copyOf(loaded);
        UApi.LOGGER.info("Loaded {} U-API reward pools", pools.size());
    }

    public static boolean grant(Identifier poolId, RewardContext context) {
        RewardPool pool = pools.get(poolId);
        if (pool == null) return false;
        RandomSource random = context.player().getRandom();
        boolean granted = false;
        for (int roll = 0; roll < pool.rolls(); roll++) {
            List<RewardPool.Entry> eligible = pool.entries().stream()
                .filter(entry -> context.instance().difficulty().ordinal() >= entry.minimumRank().ordinal())
                .filter(entry -> PROVIDERS.containsKey(entry.type())).toList();
            if (eligible.isEmpty()) continue;
            int selected = random.nextInt(eligible.stream().mapToInt(RewardPool.Entry::weight).sum());
            RewardPool.Entry choice = eligible.get(eligible.size() - 1);
            for (RewardPool.Entry entry : eligible) {
                selected -= entry.weight();
                if (selected < 0) { choice = entry; break; }
            }
            RewardProvider provider = PROVIDERS.get(choice.type());
            if (provider == null) {
                if (UApiServerConfig.LOG_SKIPPED_REWARDS.get())
                    UApi.LOGGER.warn("Skipping reward with unavailable provider {}", choice.type());
                continue;
            }
            if (provider.grant(context, choice.data(), random)) {
                granted = true;
                NeoForge.EVENT_BUS.post(new UApiEvents.RewardGranted(
                    context.instance(), context.player(), choice.type()));
            }
        }
        return granted;
    }

    private static boolean grantItem(RewardContext context, JsonObject data, RandomSource random) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(GsonHelper.getAsString(data, "id")));
        if (item == Items.AIR) return false;
        int min = Math.max(1, GsonHelper.getAsInt(data, "min", 1));
        int max = Math.max(min, GsonHelper.getAsInt(data, "max", min));
        int amount = min + random.nextInt(max - min + 1);
        int scaled = Math.max(1, (int) Math.round(amount * context.instance().difficulty().rewardMultiplier()));
        ItemStack stack = new ItemStack(item, scaled);
        ServerPlayer player = context.player();
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        return true;
    }

    private static boolean grantExperience(RewardContext context, JsonObject data, RandomSource random) {
        int min = Math.max(0, GsonHelper.getAsInt(data, "min", 1));
        int max = Math.max(min, GsonHelper.getAsInt(data, "max", min));
        int amount = min + random.nextInt(max - min + 1);
        context.player().giveExperiencePoints((int) Math.round(
            amount * context.instance().difficulty().rewardMultiplier()));
        return amount > 0;
    }
}
