package dev.uapi.client.ui.core;

/** Immutable pointer input passed to retained components. */
public record UIInputContext(double mouseX, double mouseY, int button) {
}
