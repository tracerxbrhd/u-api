package dev.uapi.instance;

import dev.uapi.config.UApiServerConfig;
import dev.uapi.difficulty.DifficultyRank;
import dev.uapi.event.UApiEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Relative;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;

public final class InstanceManager {
    private static final Map<MinecraftServer, InstanceManager> MANAGERS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private final MinecraftServer server;
    private final InstanceSavedData data;
    private final long bootMillis;

    private InstanceManager(MinecraftServer server) {
        this.server = server;
        this.bootMillis = System.currentTimeMillis();
        this.data = server.overworld().getDataStorage().computeIfAbsent(InstanceSavedData.TYPE);
    }

    public static InstanceManager get(MinecraftServer server) {
        return MANAGERS.computeIfAbsent(server, InstanceManager::new);
    }

    /** Releases the manager's strong value-to-server reference at the lifecycle boundary. */
    public static void stop(MinecraftServer server) {
        MANAGERS.remove(Objects.requireNonNull(server, "server"));
    }

    public Collection<InstanceView> all() {
        return data.instances().values().stream().map(value -> (InstanceView) value).toList();
    }

    public Optional<InstanceView> find(UUID id) {
        return Optional.ofNullable(data.instances().get(id));
    }

    public boolean isFromPreviousServerRun(InstanceView instance) {
        return instance.createdAtMillis() < bootMillis;
    }

    public Optional<InstanceView> findByPlayer(UUID playerId) {
        return data.instances().values().stream()
            .filter(instance -> !instance.phase().terminal() && instance.participants().contains(playerId))
            .map(instance -> (InstanceView) instance).findFirst();
    }

    public Optional<InstanceView> findAssignedByPlayer(UUID playerId) {
        return data.instances().values().stream()
            .filter(instance -> instance.phase() != InstancePhase.REMOVED && instance.participants().contains(playerId))
            .map(instance -> (InstanceView) instance).findFirst();
    }

    public ManagedInstance create(Identifier definitionId, InstanceType type, DifficultyRank difficulty,
                                  ServerPlayer owner, int entrySeconds) {
        long active = data.instances().values().stream().filter(value -> !value.phase().terminal()).count();
        int maxActive = UApiServerConfig.MAX_ACTIVE_INSTANCES.get();
        if (maxActive > 0 && active >= maxActive)
            throw new IllegalStateException("Maximum active instance count reached");
        if (findByPlayer(owner.getUUID()).isPresent())
            throw new IllegalStateException("Player is already assigned to an instance");
        long now = System.currentTimeMillis();
        ManagedInstance instance = new ManagedInstance(UUID.randomUUID(), definitionId, type, difficulty,
            owner.getUUID(), InstancePhase.WAITING_FOR_ENTRY, now, now + entrySeconds * 1000L);
        data.instances().put(instance.id(), instance);
        data.changed();
        NeoForge.EVENT_BUS.post(new UApiEvents.InstanceLifecycle(instance,
            UApiEvents.InstanceLifecycle.Stage.CREATED, "created"));
        return instance;
    }

    public boolean addParticipant(UUID id, ServerPlayer player) {
        ManagedInstance instance = data.instances().get(id);
        if (instance == null || instance.phase().terminal()) return false;
        Optional<InstanceView> assigned = findByPlayer(player.getUUID());
        if (assigned.isPresent() && !assigned.get().id().equals(id)) return false;
        if (instance.participants().contains(player.getUUID())) return true;
        if (instance.participants().size() >= UApiServerConfig.MAX_PLAYERS_PER_INSTANCE.get()) return false;
        instance.addParticipant(player.getUUID(), ReturnPoint.capture(player));
        data.changed();
        return true;
    }

    public boolean transition(UUID id, InstancePhase phase, int durationSeconds, String reason) {
        ManagedInstance instance = data.instances().get(id);
        if (instance == null || !allowed(instance.phase(), phase)) return false;
        long deadline = durationSeconds <= 0 ? 0L : System.currentTimeMillis() + durationSeconds * 1000L;
        instance.setPhase(phase, deadline);
        data.changed();
        UApiEvents.InstanceLifecycle.Stage stage = switch (phase) {
            case RUNNING, ACTIVE -> UApiEvents.InstanceLifecycle.Stage.STARTED;
            case COMPLETED -> UApiEvents.InstanceLifecycle.Stage.COMPLETED;
            case FAILED -> UApiEvents.InstanceLifecycle.Stage.FAILED;
            case CLEANUP_PENDING, CLEANING -> UApiEvents.InstanceLifecycle.Stage.CLEANING;
            case REMOVED -> UApiEvents.InstanceLifecycle.Stage.REMOVED;
            default -> null;
        };
        if (stage != null) NeoForge.EVENT_BUS.post(new UApiEvents.InstanceLifecycle(instance, stage, reason));
        return true;
    }

    public boolean fail(UUID id, String reason) { return transition(id, InstancePhase.FAILED, 0, reason); }
    public boolean complete(UUID id, String reason) { return transition(id, InstancePhase.COMPLETED, 0, reason); }

    public boolean remove(UUID id, String reason) {
        ManagedInstance instance = data.instances().get(id);
        if (instance == null) return false;
        instance.setPhase(InstancePhase.REMOVED, 0L);
        NeoForge.EVENT_BUS.post(new UApiEvents.InstanceLifecycle(instance,
            UApiEvents.InstanceLifecycle.Stage.REMOVED, reason));
        data.instances().remove(id);
        data.changed();
        return true;
    }

    public void tick(long nowMillis) {
        for (ManagedInstance instance : new ArrayList<>(data.instances().values())) {
            if (instance.deadlineMillis() <= 0 || nowMillis < instance.deadlineMillis()) continue;
            switch (instance.phase()) {
                case WAITING_FOR_ENTRY, PREPARING, GENERATING, ACTIVE, BOSS_ACTIVE, RUNNING -> fail(instance.id(), "timer_expired");
                case REWARD, REWARD_PHASE -> complete(instance.id(), "reward_timer_expired");
                default -> { }
            }
        }
    }

    public boolean returnPlayer(ServerPlayer player, InstanceView instance) {
        ReturnPoint point = instance.returnPoints().get(player.getUUID());
        ServerLevel target = point == null ? server.overworld() : server.getLevel(point.dimension());
        if (target == null) target = server.overworld();
        BlockPos fallback = target.getRespawnData().globalPos().pos();
        double x = point == null ? fallback.getX() + 0.5 : point.x();
        double y = point == null ? fallback.getY() + 1.0 : point.y();
        double z = point == null ? fallback.getZ() + 0.5 : point.z();
        float yaw = point == null ? 0 : point.yaw();
        float pitch = point == null ? 0 : point.pitch();
        BlockPos requested = BlockPos.containing(x, y, z);
        BlockState feet = target.getBlockState(requested);
        BlockState head = target.getBlockState(requested.above());
        BlockState floor = target.getBlockState(requested.below());
        if (!feet.getCollisionShape(target, requested).isEmpty()
            || !head.getCollisionShape(target, requested.above()).isEmpty()
            || floor.getCollisionShape(target, requested.below()).isEmpty()) {
            x = fallback.getX() + 0.5; y = fallback.getY() + 1.0; z = fallback.getZ() + 0.5;
        }
        player.teleportTo(target, x, y, z, java.util.Set.<Relative>of(), yaw, pitch, false);
        player.sendSystemMessage(Component.translatable("message.u_api.returned"));
        return true;
    }

    public int returnAll(InstanceView instance) {
        int returned = 0;
        for (UUID playerId : instance.participants()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null && returnPlayer(player, instance)) returned++;
        }
        return returned;
    }

    private static boolean allowed(InstancePhase from, InstancePhase to) {
        if (from == to || from == InstancePhase.REMOVED) return false;
        if (to == InstancePhase.FAILED || to == InstancePhase.CLEANUP_PENDING
            || to == InstancePhase.CLEANING || to == InstancePhase.REMOVED) return true;
        return switch (from) {
            case WAITING_FOR_ENTRY -> to == InstancePhase.PREPARING || to == InstancePhase.GENERATING
                || to == InstancePhase.RUNNING || to == InstancePhase.ACTIVE;
            case PREPARING -> to == InstancePhase.GENERATING || to == InstancePhase.RUNNING || to == InstancePhase.ACTIVE;
            case GENERATING -> to == InstancePhase.ACTIVE || to == InstancePhase.BOSS_ACTIVE;
            case ACTIVE -> to == InstancePhase.BOSS_ACTIVE || to == InstancePhase.REWARD_PHASE || to == InstancePhase.COMPLETED;
            case BOSS_ACTIVE -> to == InstancePhase.REWARD_PHASE || to == InstancePhase.REWARD || to == InstancePhase.COMPLETED;
            case RUNNING -> to == InstancePhase.BOSS_ACTIVE || to == InstancePhase.REWARD
                || to == InstancePhase.REWARD_PHASE || to == InstancePhase.COMPLETED;
            case REWARD, REWARD_PHASE -> to == InstancePhase.COMPLETED;
            default -> false;
        };
    }
}
