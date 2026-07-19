package dev.uapi.api.level;

import net.minecraft.server.level.ServerPlayer;

/** Supplies a non-negative progression level without coupling consumers to a progression mod. */
@FunctionalInterface
public interface PlayerLevelProvider {
    int level(ServerPlayer player);
}
