package dev.uapi.client.ui.components;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.chat.Component;

public record UIPlayerCardModel(UUID playerId, Component displayName, Component status, double healthFraction) {
    public UIPlayerCardModel {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(status, "status");
        if (!Double.isFinite(healthFraction)) healthFraction = 0;
        healthFraction = Math.max(0, Math.min(1, healthFraction));
    }
}
