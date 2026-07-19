package dev.uapi.api.profile;

import java.util.Objects;

/** One label/value row in a neutral profile facet. */
public record ProfileFacetField(ProfileFacetText label, ProfileFacetText value, boolean prominent) {
    public ProfileFacetField {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(value, "value");
    }

    public static ProfileFacetField literal(String labelKey, String value) {
        return new ProfileFacetField(ProfileFacetText.translatable(labelKey),
            ProfileFacetText.literal(value), false);
    }

    public static ProfileFacetField prominent(String labelKey, String value) {
        return new ProfileFacetField(ProfileFacetText.translatable(labelKey),
            ProfileFacetText.literal(value), true);
    }
}
