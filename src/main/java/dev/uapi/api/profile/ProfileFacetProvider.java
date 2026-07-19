package dev.uapi.api.profile;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Optional server-side profile projection provider.
 *
 * <p>The provider is called on the server thread and must return only immutable snapshots. It must
 * enforce its own privacy policy; U-API additionally enforces each facet's declared audience.</p>
 */
public interface ProfileFacetProvider {
    ResourceLocation providerId();

    List<ProfileFacet> provide(ProfileFacetQuery query);
}
