package dev.uapi.reward;

import com.google.gson.JsonObject;
import net.minecraft.util.RandomSource;

@FunctionalInterface
public interface RewardProvider {
    boolean grant(RewardContext context, JsonObject data, RandomSource random);
}
