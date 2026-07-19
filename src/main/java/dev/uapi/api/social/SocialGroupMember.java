package dev.uapi.api.social;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Immutable, public-safe member snapshot without provider-owned role objects. */
public record SocialGroupMember(
    UUID playerId,
    String displayName,
    Optional<String> roleId,
    Set<SocialGroupRoleFlag> roleFlags,
    boolean online
) {
    public SocialGroupMember {
        Objects.requireNonNull(playerId, "playerId");
        displayName = requireText(displayName, "displayName");
        roleId = Objects.requireNonNull(roleId, "roleId").map(value -> requireText(value, "roleId"));
        Objects.requireNonNull(roleFlags, "roleFlags");
        EnumSet<SocialGroupRoleFlag> copied = roleFlags.isEmpty()
            ? EnumSet.noneOf(SocialGroupRoleFlag.class)
            : EnumSet.copyOf(roleFlags);
        roleFlags = Collections.unmodifiableSet(copied);
    }

    public boolean hasRoleFlag(SocialGroupRoleFlag flag) {
        return roleFlags.contains(Objects.requireNonNull(flag, "flag"));
    }

    public boolean isLeader() {
        return hasRoleFlag(SocialGroupRoleFlag.OWNER) || hasRoleFlag(SocialGroupRoleFlag.LEADER);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
