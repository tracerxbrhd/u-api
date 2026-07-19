package dev.uapi.api.social;

/**
 * Portable role hints suitable for display and coarse integration decisions.
 * Provider-specific roles remain represented by {@link SocialGroupMember#roleId()}.
 */
public enum SocialGroupRoleFlag {
    OWNER,
    LEADER,
    ASSISTANT,
    MEMBER
}
