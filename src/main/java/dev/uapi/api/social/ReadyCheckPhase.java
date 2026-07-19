package dev.uapi.api.social;

/** Lifecycle state of a ready check. Every state except {@link #ACTIVE} is terminal. */
public enum ReadyCheckPhase {
    ACTIVE,
    ALL_READY,
    DECLINED,
    TIMED_OUT,
    CANCELLED;

    public boolean terminal() {
        return this != ACTIVE;
    }
}
