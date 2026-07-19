package dev.uapi.api.profile;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/** Server-authoritative query. Providers must not retain the server or player identifiers. */
public record ProfileFacetQuery(
    MinecraftServer server,
    UUID viewerId,
    UUID subjectId,
    ProfileFacetContext context
) {
    public ProfileFacetQuery {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(subjectId, "subjectId");
        Objects.requireNonNull(context, "context");
    }

    public boolean selfView() {
        return viewerId.equals(subjectId);
    }
}
