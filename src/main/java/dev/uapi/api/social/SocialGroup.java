package dev.uapi.api.social;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Immutable group snapshot shared across optional integrations. */
public record SocialGroup(
    UUID groupId,
    SocialGroupType type,
    String displayName,
    long revision,
    List<SocialGroupMember> members
) {
    public SocialGroup {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(displayName, "displayName");
        displayName = displayName.trim();
        if (displayName.isEmpty()) throw new IllegalArgumentException("displayName must not be blank");
        if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        members = List.copyOf(Objects.requireNonNull(members, "members"));
        if (members.isEmpty()) throw new IllegalArgumentException("members must not be empty");

        Set<UUID> uniqueIds = new HashSet<>();
        for (SocialGroupMember member : members) {
            Objects.requireNonNull(member, "members must not contain null");
            if (!uniqueIds.add(member.playerId()))
                throw new IllegalArgumentException("duplicate social group member: " + member.playerId());
        }
    }

    public Optional<SocialGroupMember> member(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return members.stream().filter(member -> member.playerId().equals(playerId)).findFirst();
    }

    public boolean contains(UUID playerId) {
        return member(playerId).isPresent();
    }

    public Set<UUID> memberIds() {
        return members.stream().map(SocialGroupMember::playerId)
            .collect(Collectors.toUnmodifiableSet());
    }
}
