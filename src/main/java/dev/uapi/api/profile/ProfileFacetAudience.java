package dev.uapi.api.profile;

/**
 * Minimum audience policy enforced by U-API after a provider produces a facet.
 *
 * <p>Providers remain responsible for applying any stricter, mod-specific privacy rules before
 * returning data. In particular, a provider must never put secret values in a public facet.</p>
 */
public enum ProfileFacetAudience {
    PUBLIC,
    SHARED_SOCIAL_GROUP,
    SUBJECT_ONLY
}
