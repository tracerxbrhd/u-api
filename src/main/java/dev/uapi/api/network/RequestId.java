package dev.uapi.api.network;

import java.util.Objects;
import java.util.UUID;

/** Opaque request correlation identifier. */
public record RequestId(UUID value) implements Comparable<RequestId> {
    public RequestId {
        Objects.requireNonNull(value, "value");
    }

    public static RequestId random() {
        return new RequestId(UUID.randomUUID());
    }

    public static RequestId parse(String value) {
        return new RequestId(UUID.fromString(Objects.requireNonNull(value, "value")));
    }

    @Override
    public int compareTo(RequestId other) {
        Objects.requireNonNull(other, "other");
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
