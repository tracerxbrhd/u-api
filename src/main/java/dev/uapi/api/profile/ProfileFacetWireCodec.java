package dev.uapi.api.profile;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

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
        buffer.writeVarInt(facet.entries().size());
        for (ProfileFacetEntry entry : facet.entries()) encodeEntry(buffer, entry);
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
        if (size < 0 || size > ProfileFacet.MAXIMUM_FIELDS)
            throw new IllegalArgumentException("invalid profile facet field count: " + size);
        List<ProfileFacetField> fields = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            fields.add(new ProfileFacetField(decodeText(buffer), decodeText(buffer), buffer.readBoolean()));
        }
        int entryCount = buffer.readVarInt();
        if (entryCount < 0 || entryCount > ProfileFacet.MAXIMUM_ENTRIES)
            throw new IllegalArgumentException("invalid profile facet entry count: " + entryCount);
        List<ProfileFacetEntry> entries = new ArrayList<>(entryCount);
        for (int index = 0; index < entryCount; index++) entries.add(decodeEntry(buffer));
        return new ProfileFacet(id, title, icon, audience, displayOrder, fields, entries);
    }

    private static void encodeText(RegistryFriendlyByteBuf buffer, ProfileFacetText text) {
        buffer.writeUtf(text.value(), ProfileFacetText.MAXIMUM_LENGTH);
        buffer.writeBoolean(text.translatable());
    }

    private static ProfileFacetText decodeText(RegistryFriendlyByteBuf buffer) {
        return new ProfileFacetText(buffer.readUtf(ProfileFacetText.MAXIMUM_LENGTH), buffer.readBoolean());
    }

    private static void encodeEntry(RegistryFriendlyByteBuf destination, ProfileFacetEntry entry) {
        RegistryFriendlyByteBuf buffer =
            new RegistryFriendlyByteBuf(Unpooled.buffer(), destination.registryAccess());
        try {
            buffer.writeResourceLocation(entry.type());
            buffer.writeBoolean(entry.id().isPresent());
            entry.id().ifPresent(buffer::writeResourceLocation);
            encodeText(buffer, entry.typeLabel());
            ComponentSerialization.STREAM_CODEC.encode(buffer, entry.name());
            ComponentSerialization.STREAM_CODEC.encode(buffer, entry.description());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, entry.icon());
            buffer.writeVarInt(entry.metadata().size());
            for (ProfileFacetField field : entry.metadata()) {
                encodeText(buffer, field.label());
                encodeText(buffer, field.value());
                buffer.writeBoolean(field.prominent());
            }
            int encodedSize = buffer.readableBytes();
            if (encodedSize > ProfileFacetEntry.MAXIMUM_WIRE_BYTES)
                throw new IllegalArgumentException("profile facet entry exceeds "
                    + ProfileFacetEntry.MAXIMUM_WIRE_BYTES + " encoded bytes");
            destination.writeVarInt(encodedSize);
            destination.writeBytes(buffer, buffer.readerIndex(), encodedSize);
        } finally {
            buffer.release();
        }
    }

    private static ProfileFacetEntry decodeEntry(RegistryFriendlyByteBuf source) {
        int encodedSize = source.readVarInt();
        if (encodedSize < 0 || encodedSize > ProfileFacetEntry.MAXIMUM_WIRE_BYTES)
            throw new IllegalArgumentException("invalid profile facet entry size: " + encodedSize);
        RegistryFriendlyByteBuf buffer =
            new RegistryFriendlyByteBuf(source.readSlice(encodedSize), source.registryAccess());
        var type = buffer.readResourceLocation();
        java.util.Optional<net.minecraft.resources.ResourceLocation> id = buffer.readBoolean()
            ? java.util.Optional.of(buffer.readResourceLocation()) : java.util.Optional.empty();
        ProfileFacetText typeLabel = decodeText(buffer);
        Component name = ComponentSerialization.STREAM_CODEC.decode(buffer);
        Component description = ComponentSerialization.STREAM_CODEC.decode(buffer);
        ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        int metadataCount = buffer.readVarInt();
        if (metadataCount < 0 || metadataCount > ProfileFacetEntry.MAXIMUM_METADATA_FIELDS)
            throw new IllegalArgumentException("invalid profile facet entry metadata count: "
                + metadataCount);
        List<ProfileFacetField> metadata = new ArrayList<>(metadataCount);
        for (int index = 0; index < metadataCount; index++) {
            metadata.add(new ProfileFacetField(
                decodeText(buffer), decodeText(buffer), buffer.readBoolean()));
        }
        if (buffer.isReadable())
            throw new IllegalArgumentException("profile facet entry contains trailing bytes");
        return new ProfileFacetEntry(type, id, typeLabel, name, description, icon, metadata);
    }
}
