package dev.uapi.client;

import net.minecraft.resources.ResourceLocation;

/** Implemented by custom screens that want the shared U-API screen navigation. */
public interface UApiTabHost {
    ResourceLocation uApiTabId();
    int uApiTabLeft();
    int uApiTabTop();

    /** Null keeps vanilla widget sprites, used by the vanilla inventory screen. */
    default UApiTabSprites uApiTabSprites() { return null; }
}
