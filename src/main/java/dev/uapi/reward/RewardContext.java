package dev.uapi.reward;

import dev.uapi.instance.InstanceView;
import net.minecraft.server.level.ServerPlayer;

public record RewardContext(InstanceView instance, ServerPlayer player) {}
