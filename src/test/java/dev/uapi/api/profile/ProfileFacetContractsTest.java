package dev.uapi.api.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class ProfileFacetContractsTest {
    @Test
    void snapshotsDefensivelyCopyFields() {
        List<ProfileFacetField> fields = new ArrayList<>();
        fields.add(ProfileFacetField.literal("profile.test.label", "value"));
        ProfileFacet facet = new ProfileFacet(ResourceLocation.fromNamespaceAndPath("test", "facet"),
            ProfileFacetText.translatable("profile.test.title"), ProfileFacetAudience.PUBLIC, 10, fields);

        fields.clear();

        assertEquals(1, facet.fields().size());
        assertThrows(UnsupportedOperationException.class, () -> facet.fields().clear());
    }

    @Test
    void rejectsUnboundedOrEmptyContent() {
        assertThrows(IllegalArgumentException.class, () -> ProfileFacetText.literal(" "));
        assertThrows(IllegalArgumentException.class, () -> ProfileFacetText.literal("x".repeat(257)));
        assertThrows(IllegalArgumentException.class, () -> new ProfileFacet(
            ResourceLocation.fromNamespaceAndPath("test", "empty"), ProfileFacetText.literal("Empty"),
            ProfileFacetAudience.PUBLIC, 0, List.of()));
    }
}
