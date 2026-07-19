package dev.uapi.api.profile;

import java.util.Objects;
import net.minecraft.network.chat.Component;

/** Bounded text which remains localizable after it crosses a mod-owned network channel. */
public record ProfileFacetText(String value, boolean translatable) {
    public static final int MAXIMUM_LENGTH = 256;

    public ProfileFacetText {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("profile facet text must not be blank");
        if (value.length() > MAXIMUM_LENGTH)
            throw new IllegalArgumentException("profile facet text exceeds " + MAXIMUM_LENGTH + " characters");
    }

    public static ProfileFacetText literal(String value) {
        return new ProfileFacetText(value, false);
    }

    public static ProfileFacetText translatable(String translationKey) {
        return new ProfileFacetText(translationKey, true);
    }

    public Component component() {
        return translatable ? Component.translatable(value) : Component.literal(value);
    }
}
