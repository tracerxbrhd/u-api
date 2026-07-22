package dev.uapi.api.profile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/** Bounded, immutable, UI-safe projection contributed by an optional mod. */
public record ProfileFacet(
    Identifier id,
    ProfileFacetText title,
    Optional<ProfileFacetIcon> icon,
    ProfileFacetAudience audience,
    int displayOrder,
    List<ProfileFacetField> fields
) {
    public static final int MAXIMUM_FIELDS = 16;

    public ProfileFacet {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        icon = Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(audience, "audience");
        if (displayOrder < -1_000_000 || displayOrder > 1_000_000)
            throw new IllegalArgumentException("profile facet displayOrder is out of range");
        fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
        if (fields.isEmpty()) throw new IllegalArgumentException("profile facet must contain at least one field");
        if (fields.size() > MAXIMUM_FIELDS)
            throw new IllegalArgumentException("profile facet exceeds " + MAXIMUM_FIELDS + " fields");
        fields.forEach(field -> Objects.requireNonNull(field, "fields must not contain null"));
    }

    public ProfileFacet(Identifier id, ProfileFacetText title, ProfileFacetAudience audience,
                        int displayOrder, List<ProfileFacetField> fields) {
        this(id, title, Optional.empty(), audience, displayOrder, fields);
    }
}
