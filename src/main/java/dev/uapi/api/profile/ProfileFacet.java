package dev.uapi.api.profile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Bounded, immutable, UI-safe projection contributed by an optional mod. */
public record ProfileFacet(
    ResourceLocation id,
    ProfileFacetText title,
    Optional<ProfileFacetIcon> icon,
    ProfileFacetAudience audience,
    int displayOrder,
    List<ProfileFacetField> fields,
    List<ProfileFacetEntry> entries
) {
    public static final int MAXIMUM_FIELDS = 16;
    public static final int MAXIMUM_ENTRIES = 8;

    public ProfileFacet {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        icon = Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(audience, "audience");
        if (displayOrder < -1_000_000 || displayOrder > 1_000_000)
            throw new IllegalArgumentException("profile facet displayOrder is out of range");
        fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
        if (fields.size() > MAXIMUM_FIELDS)
            throw new IllegalArgumentException("profile facet exceeds " + MAXIMUM_FIELDS + " fields");
        fields.forEach(field -> Objects.requireNonNull(field, "fields must not contain null"));
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        if (entries.size() > MAXIMUM_ENTRIES)
            throw new IllegalArgumentException("profile facet exceeds " + MAXIMUM_ENTRIES + " entries");
        entries.forEach(entry -> Objects.requireNonNull(entry, "entries must not contain null"));
        if (fields.isEmpty() && entries.isEmpty())
            throw new IllegalArgumentException("profile facet must contain at least one field or entry");
    }

    public ProfileFacet(ResourceLocation id, ProfileFacetText title, Optional<ProfileFacetIcon> icon,
                        ProfileFacetAudience audience, int displayOrder, List<ProfileFacetField> fields) {
        this(id, title, icon, audience, displayOrder, fields, List.of());
    }

    public ProfileFacet(ResourceLocation id, ProfileFacetText title, ProfileFacetAudience audience,
                        int displayOrder, List<ProfileFacetField> fields) {
        this(id, title, Optional.empty(), audience, displayOrder, fields, List.of());
    }

    public static ProfileFacet entries(
        ResourceLocation id,
        ProfileFacetText title,
        Optional<ProfileFacetIcon> icon,
        ProfileFacetAudience audience,
        int displayOrder,
        List<ProfileFacetEntry> entries
    ) {
        return new ProfileFacet(id, title, icon, audience, displayOrder, List.of(), entries);
    }
}
