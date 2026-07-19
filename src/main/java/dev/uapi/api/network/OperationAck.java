package dev.uapi.api.network;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Successful operation envelope containing only correlation and a stable machine-readable code. */
public record OperationAck(RequestId requestId, ResourceLocation code) {
    public OperationAck {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(code, "code");
    }
}
