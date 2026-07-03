package dev.uapi.instance;

public enum InstanceType {
    DUNGEON,
    SURVIVAL,
    BOSS_DUNGEON,
    SURVIVAL_ARENA;

    public boolean isSurvival() { return this == SURVIVAL || this == SURVIVAL_ARENA; }
}
