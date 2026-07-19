package dev.uapi.client.overlay;

/** Whether a screen-space marker is hidden by solid blocks between camera and target. */
public enum OcclusionMode {
    RESPECT_BLOCKS,
    THROUGH_WALLS
}
