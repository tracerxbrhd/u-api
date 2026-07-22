package dev.uapi.instance;

import dev.uapi.difficulty.DifficultyRank;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface InstanceView {
    UUID id();
    Identifier definitionId();
    InstanceType type();
    DifficultyRank difficulty();
    UUID ownerId();
    Set<UUID> participants();
    Map<UUID, ReturnPoint> returnPoints();
    InstancePhase phase();
    long createdAtMillis();
    long deadlineMillis();
}
