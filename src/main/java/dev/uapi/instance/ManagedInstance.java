package dev.uapi.instance;

import dev.uapi.difficulty.DifficultyRank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ManagedInstance implements InstanceView {
    private final UUID id;
    private final ResourceLocation definitionId;
    private final InstanceType type;
    private final DifficultyRank difficulty;
    private final UUID ownerId;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Map<UUID, ReturnPoint> returnPoints = new LinkedHashMap<>();
    private final long createdAtMillis;
    private InstancePhase phase;
    private long deadlineMillis;

    public ManagedInstance(UUID id, ResourceLocation definitionId, InstanceType type, DifficultyRank difficulty,
                           UUID ownerId, InstancePhase phase, long createdAtMillis, long deadlineMillis) {
        this.id = id;
        this.definitionId = definitionId;
        this.type = type;
        this.difficulty = difficulty;
        this.ownerId = ownerId;
        this.phase = phase;
        this.createdAtMillis = createdAtMillis;
        this.deadlineMillis = deadlineMillis;
    }

    @Override public UUID id() { return id; }
    @Override public ResourceLocation definitionId() { return definitionId; }
    @Override public InstanceType type() { return type; }
    @Override public DifficultyRank difficulty() { return difficulty; }
    @Override public UUID ownerId() { return ownerId; }
    @Override public Set<UUID> participants() { return Set.copyOf(participants); }
    @Override public Map<UUID, ReturnPoint> returnPoints() { return Map.copyOf(returnPoints); }
    @Override public InstancePhase phase() { return phase; }
    @Override public long createdAtMillis() { return createdAtMillis; }
    @Override public long deadlineMillis() { return deadlineMillis; }

    void setPhase(InstancePhase phase, long deadlineMillis) {
        this.phase = phase;
        this.deadlineMillis = deadlineMillis;
    }

    void addParticipant(UUID playerId, ReturnPoint returnPoint) {
        participants.add(playerId);
        returnPoints.putIfAbsent(playerId, returnPoint);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("definition", definitionId.toString());
        tag.putString("type", type.name());
        tag.putString("difficulty", difficulty.name());
        tag.putUUID("owner", ownerId);
        tag.putString("phase", phase.name());
        tag.putLong("createdAt", createdAtMillis);
        tag.putLong("deadline", deadlineMillis);
        ListTag players = new ListTag();
        for (UUID playerId : participants) {
            CompoundTag player = new CompoundTag();
            player.putUUID("id", playerId);
            ReturnPoint point = returnPoints.get(playerId);
            if (point != null) player.put("return", point.save());
            players.add(player);
        }
        tag.put("players", players);
        return tag;
    }

    public static ManagedInstance load(CompoundTag tag) {
        ManagedInstance instance = new ManagedInstance(tag.getUUID("id"),
            ResourceLocation.parse(tag.getString("definition")),
            InstanceType.valueOf(tag.getString("type")),
            DifficultyRank.byName(tag.getString("difficulty")), tag.getUUID("owner"),
            InstancePhase.valueOf(tag.getString("phase")), tag.getLong("createdAt"), tag.getLong("deadline"));
        ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
        for (Tag entry : players) {
            CompoundTag player = (CompoundTag) entry;
            UUID playerId = player.getUUID("id");
            instance.participants.add(playerId);
            if (player.contains("return", Tag.TAG_COMPOUND)) {
                instance.returnPoints.put(playerId, ReturnPoint.load(player.getCompound("return")));
            }
        }
        return instance;
    }
}
