package dev.uapi.api.social;

import dev.uapi.api.services.UApiService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative social lookup contract.
 *
 * <p>Implementations expose snapshots only; consumers never receive Guild, Party, or addon-internal
 * mutable models. An active party may be either {@link SocialGroupType#PARTY} or
 * {@link SocialGroupType#GUILD_SQUAD}.</p>
 */
public interface SocialGroupService extends UApiService {
    Optional<SocialGroup> getActiveParty(UUID playerId);

    Optional<SocialGroup> getGuild(UUID playerId);

    List<SocialGroup> getGroups(UUID playerId);

    Optional<SocialGroup> getGroup(UUID groupId);

    /** Returns the authoritative participant set eligible for a new ready check. */
    Set<UUID> getReadyCheckParticipants(UUID groupId);

    /** Performs the provider's current server-side role and membership checks. */
    boolean canInitiateReadyCheck(UUID groupId, UUID playerId);

    default boolean isMember(UUID groupId, UUID playerId) {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(playerId, "playerId");
        return getGroup(groupId).map(group -> group.contains(playerId)).orElse(false);
    }
}
