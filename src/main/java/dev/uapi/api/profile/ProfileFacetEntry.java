package dev.uapi.api.profile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * One retained identity card inside a neutral profile facet.
 *
 * <p>The semantic {@code type} and optional source {@code id} are namespaced identifiers rather
 * than provider-specific Java types. Names, descriptions and item icons remain data-driven and are
 * defensively copied before entering a public snapshot.</p>
 */
public record ProfileFacetEntry(
    ResourceLocation type,
    Optional<ResourceLocation> id,
    ProfileFacetText typeLabel,
    Component name,
    Component description,
    ItemStack icon,
    List<ProfileFacetField> metadata
) {
    public static final int MAXIMUM_METADATA_FIELDS = 8;
    public static final int MAXIMUM_NAME_LENGTH = 512;
    public static final int MAXIMUM_DESCRIPTION_LENGTH = 4096;
    public static final int MAXIMUM_WIRE_BYTES = 16 * 1024;

    public ProfileFacetEntry {
        Objects.requireNonNull(type, "type");
        id = Objects.requireNonNull(id, "id");
        Objects.requireNonNull(typeLabel, "typeLabel");
        name = copyBounded(Objects.requireNonNull(name, "name"), MAXIMUM_NAME_LENGTH, "name", false);
        description = copyBounded(Objects.requireNonNull(description, "description"),
            MAXIMUM_DESCRIPTION_LENGTH, "description", true);
        icon = Objects.requireNonNull(icon, "icon").isEmpty()
            ? ItemStack.EMPTY : icon.copyWithCount(1);
        metadata = List.copyOf(Objects.requireNonNull(metadata, "metadata"));
        if (metadata.size() > MAXIMUM_METADATA_FIELDS)
            throw new IllegalArgumentException("profile facet entry exceeds "
                + MAXIMUM_METADATA_FIELDS + " metadata fields");
        metadata.forEach(field ->
            Objects.requireNonNull(field, "metadata must not contain null"));
    }

    public ProfileFacetEntry(
        ResourceLocation type,
        ProfileFacetText typeLabel,
        Component name,
        Component description,
        ItemStack icon,
        List<ProfileFacetField> metadata
    ) {
        this(type, Optional.empty(), typeLabel, name, description, icon, metadata);
    }

    @Override
    public Component name() {
        return name.copy();
    }

    @Override
    public Component description() {
        return description.copy();
    }

    @Override
    public ItemStack icon() {
        return icon.isEmpty() ? ItemStack.EMPTY : icon.copy();
    }

    private static Component copyBounded(Component value, int maximum, String field, boolean emptyAllowed) {
        String plainText = value.getString();
        if (!emptyAllowed && plainText.isBlank())
            throw new IllegalArgumentException("profile facet entry " + field + " must not be blank");
        if (plainText.length() > maximum)
            throw new IllegalArgumentException("profile facet entry " + field + " exceeds "
                + maximum + " characters");
        return value.copy();
    }
}
