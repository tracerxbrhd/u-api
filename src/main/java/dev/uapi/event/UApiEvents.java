package dev.uapi.event;

import dev.uapi.instance.InstanceView;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

public final class UApiEvents {
    private UApiEvents() {}

    public static final class InstanceLifecycle extends Event {
        public enum Stage { CREATED, STARTED, COMPLETED, FAILED, CLEANING, REMOVED }
        private final InstanceView instance;
        private final Stage stage;
        private final String reason;

        public InstanceLifecycle(InstanceView instance, Stage stage, String reason) {
            this.instance = instance;
            this.stage = stage;
            this.reason = reason;
        }
        public InstanceView instance() { return instance; }
        public Stage stage() { return stage; }
        public String reason() { return reason; }
    }

    public static final class PortalLifecycle extends Event {
        public enum Stage { APPEARED, ENTERED, EXPIRED, DESTROYED }
        private final Identifier portalType;
        private final Stage stage;
        private final ResourceKey<Level> dimension;
        private final BlockPos position;
        private final ServerPlayer player;

        public PortalLifecycle(Identifier portalType, Stage stage, ResourceKey<Level> dimension,
                               BlockPos position, ServerPlayer player) {
            this.portalType = portalType;
            this.stage = stage;
            this.dimension = dimension;
            this.position = position.immutable();
            this.player = player;
        }
        public Identifier portalType() { return portalType; }
        public Stage stage() { return stage; }
        public ResourceKey<Level> dimension() { return dimension; }
        public BlockPos position() { return position; }
        public ServerPlayer player() { return player; }
    }

    public static final class MobLifecycle extends Event {
        public enum Stage { MOB_SPAWNED, BOSS_SPAWNED, BOSS_KILLED }
        private final InstanceView instance;
        private final Entity entity;
        private final Stage stage;

        public MobLifecycle(InstanceView instance, Entity entity, Stage stage) {
            this.instance = instance;
            this.entity = entity;
            this.stage = stage;
        }
        public InstanceView instance() { return instance; }
        public Entity entity() { return entity; }
        public Stage stage() { return stage; }
    }

    public static final class RewardGranted extends Event {
        private final InstanceView instance;
        private final ServerPlayer player;
        private final Identifier rewardType;
        public RewardGranted(InstanceView instance, ServerPlayer player, Identifier rewardType) {
            this.instance = instance;
            this.player = player;
            this.rewardType = rewardType;
        }
        public InstanceView instance() { return instance; }
        public ServerPlayer player() { return player; }
        public Identifier rewardType() { return rewardType; }
    }
}
