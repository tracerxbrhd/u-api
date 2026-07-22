package dev.uapi.api.network;

import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Successful operation envelope containing only correlation and a stable machine-readable code. */
public record OperationAck(RequestId requestId, Identifier code) {
    public OperationAck {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(code, "code");
    }
}
