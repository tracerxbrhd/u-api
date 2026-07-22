package dev.uapi.client;

import net.minecraft.resources.Identifier;

/** Implemented by custom screens that want the shared U-API screen navigation. */
public interface UApiTabHost {
    Identifier uApiTabId();
    int uApiTabLeft();
    int uApiTabTop();

    /** Null keeps vanilla widget sprites, used by the vanilla inventory screen. */
    default UApiTabSprites uApiTabSprites() { return null; }
}
