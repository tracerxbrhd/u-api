package dev.uapi.instance;

public enum InstancePhase {
    WAITING_FOR_ENTRY,
    PREPARING,
    GENERATING,
    ACTIVE,
    BOSS_ACTIVE,
    RUNNING,
    REWARD,
    REWARD_PHASE,
    COMPLETED,
    FAILED,
    CLEANUP_PENDING,
    CLEANING,
    REMOVED;

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == CLEANUP_PENDING || this == CLEANING || this == REMOVED;
    }
}
