package dev.uapi.instance;

import dev.uapi.difficulty.DifficultyRank;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface InstanceView {
    UUID id();
    ResourceLocation definitionId();
    InstanceType type();
    DifficultyRank difficulty();
    UUID ownerId();
    Set<UUID> participants();
    Map<UUID, ReturnPoint> returnPoints();
    InstancePhase phase();
    long createdAtMillis();
    long deadlineMillis();
}
