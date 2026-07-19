package dev.uapi.api.profile;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Namespaced icon identity without linking the consumer to the provider's registries. */
public record ProfileFacetIcon(ProfileFacetIconType type, ResourceLocation id) {
    public ProfileFacetIcon {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
    }
}
