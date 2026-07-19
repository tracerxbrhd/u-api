package dev.uapi.api.network;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Failed operation envelope containing no localized or user-facing text. */
public record OperationError(RequestId requestId, ResourceLocation code) {
    public OperationError {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(code, "code");
    }
}
