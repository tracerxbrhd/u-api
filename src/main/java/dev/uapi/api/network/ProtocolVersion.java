package dev.uapi.api.network;

import java.util.Objects;
import java.util.Optional;

/**
 * Semantic network protocol version.
 *
 * <p>Endpoints are compatible when their major versions are equal. A higher or lower remote minor
 * version is accepted, but both endpoints must restrict optional features to the negotiated minor,
 * which is the lower of the two advertised minor versions. A major mismatch is never negotiated.</p>
 */
public record ProtocolVersion(int major, int minor) implements Comparable<ProtocolVersion> {
    public ProtocolVersion {
        if (major < 0) throw new IllegalArgumentException("major must not be negative");
        if (minor < 0) throw new IllegalArgumentException("minor must not be negative");
    }

    public static ProtocolVersion parse(String value) {
        Objects.requireNonNull(value, "value");
        String[] parts = value.split("\\.", -1);
        if (parts.length != 2) throw new IllegalArgumentException("Protocol version must use major.minor format");
        try {
            return new ProtocolVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid protocol version: " + value, exception);
        }
    }

    public boolean isCompatibleWith(ProtocolVersion remote) {
        return remote != null && major == remote.major;
    }

    public Optional<ProtocolVersion> negotiate(ProtocolVersion remote) {
        if (!isCompatibleWith(remote)) return Optional.empty();
        return Optional.of(new ProtocolVersion(major, Math.min(minor, remote.minor)));
    }

    /** Returns whether this negotiated/local version includes the requested minor feature level. */
    public boolean supportsMinor(int requiredMinor) {
        if (requiredMinor < 0) throw new IllegalArgumentException("requiredMinor must not be negative");
        return minor >= requiredMinor;
    }

    @Override
    public int compareTo(ProtocolVersion other) {
        Objects.requireNonNull(other, "other");
        int majorComparison = Integer.compare(major, other.major);
        return majorComparison != 0 ? majorComparison : Integer.compare(minor, other.minor);
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }
}
