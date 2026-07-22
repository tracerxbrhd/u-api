package dev.uapi.api.diagnostics;

import dev.uapi.api.services.ServiceDiagnosticSnapshot;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Immutable cross-module diagnostics snapshot with no live implementation objects. */
public record UApiDiagnosticSnapshot(
    boolean enabled,
    Instant capturedAt,
    TimingDiagnostic uiLayout,
    TimingDiagnostic uiRender,
    long activeUiComponents,
    long uiLayoutInvalidations,
    long uiRenderInvalidations,
    long activeOverlays,
    PlayerHeadCacheDiagnostic playerHeadCache,
    PacketRateDiagnostic packetRate,
    ServiceDiagnosticSnapshot services,
    Map<Identifier, Long> extensionGauges
) {
    public UApiDiagnosticSnapshot {
        Objects.requireNonNull(capturedAt, "capturedAt");
        Objects.requireNonNull(uiLayout, "uiLayout");
        Objects.requireNonNull(uiRender, "uiRender");
        Objects.requireNonNull(playerHeadCache, "playerHeadCache");
        Objects.requireNonNull(packetRate, "packetRate");
        Objects.requireNonNull(services, "services");
        extensionGauges = Map.copyOf(Objects.requireNonNull(extensionGauges, "extensionGauges"));
    }
}
