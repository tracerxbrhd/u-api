package dev.uapi.api.diagnostics;

/** Packet counters and average rates for the most recent enabled collection window. */
public record PacketRateDiagnostic(long inbound, long outbound, double inboundPerSecond, double outboundPerSecond) {
}
