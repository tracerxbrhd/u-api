package dev.uapi.api.profile;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Shared bounded wire codec for mod-owned payloads carrying neutral profile facets. */
public final class ProfileFacetWireCodec {
    private ProfileFacetWireCodec() {
    }

    public static void encodeList(RegistryFriendlyByteBuf buffer, List<ProfileFacet> facets) {
        List<ProfileFacet> copy = List.copyOf(facets);
        if (copy.size() > ProfileFacetRegistry.MAXIMUM_FACETS)
            throw new IllegalArgumentException("too many profile facets");
        buffer.writeVarInt(copy.size());
        for (ProfileFacet facet : copy) encode(buffer, facet);
    }

    public static List<ProfileFacet> decodeList(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > ProfileFacetRegistry.MAXIMUM_FACETS)
            throw new IllegalArgumentException("invalid profile facet count: " + size);
        List<ProfileFacet> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) result.add(decode(buffer));
        return List.copyOf(result);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ProfileFacet facet) {
        buffer.writeResourceLocation(facet.id());
        encodeText(buffer, facet.title());
        buffer.writeBoolean(facet.icon().isPresent());
        facet.icon().ifPresent(icon -> {
            buffer.writeEnum(icon.type());
            buffer.writeResourceLocation(icon.id());
        });
        buffer.writeEnum(facet.audience());
        buffer.writeVarInt(facet.displayOrder());
        buffer.writeVarInt(facet.fields().size());
        for (ProfileFacetField field : facet.fields()) {
            encodeText(buffer, field.label());
            encodeText(buffer, field.value());
            buffer.writeBoolean(field.prominent());
        }
    }

    private static ProfileFacet decode(RegistryFriendlyByteBuf buffer) {
        var id = buffer.readResourceLocation();
        ProfileFacetText title = decodeText(buffer);
        java.util.Optional<ProfileFacetIcon> icon = buffer.readBoolean()
            ? java.util.Optional.of(new ProfileFacetIcon(buffer.readEnum(ProfileFacetIconType.class),
                buffer.readResourceLocation()))
            : java.util.Optional.empty();
        ProfileFacetAudience audience = buffer.readEnum(ProfileFacetAudience.class);
        int displayOrder = buffer.readVarInt();
        int size = buffer.readVarInt();
        if (size < 1 || size > ProfileFacet.MAXIMUM_FIELDS)
            throw new IllegalArgumentException("invalid profile facet field count: " + size);
        List<ProfileFacetField> fields = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            fields.add(new ProfileFacetField(decodeText(buffer), decodeText(buffer), buffer.readBoolean()));
        }
        return new ProfileFacet(id, title, icon, audience, displayOrder, fields);
    }

    private static void encodeText(RegistryFriendlyByteBuf buffer, ProfileFacetText text) {
        buffer.writeUtf(text.value(), ProfileFacetText.MAXIMUM_LENGTH);
        buffer.writeBoolean(text.translatable());
    }

    private static ProfileFacetText decodeText(RegistryFriendlyByteBuf buffer) {
        return new ProfileFacetText(buffer.readUtf(ProfileFacetText.MAXIMUM_LENGTH), buffer.readBoolean());
    }
}
