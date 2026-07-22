package dev.uapi.api.network;

import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Failed operation envelope containing no localized or user-facing text. */
public record OperationError(RequestId requestId, Identifier code) {
    public OperationError {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(code, "code");
    }
}
