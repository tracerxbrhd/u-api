package dev.uapi.instance;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public record ReturnPoint(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
    public static ReturnPoint capture(ServerPlayer player) {
        return new ReturnPoint(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
            player.getYRot(), player.getXRot());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.identifier().toString());
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        return tag;
    }

    public static ReturnPoint load(CompoundTag tag) {
        Identifier id = Identifier.parse(tag.getStringOr("dimension", "minecraft:overworld"));
        return new ReturnPoint(ResourceKey.create(Registries.DIMENSION, id), tag.getDoubleOr("x", 0.0),
            tag.getDoubleOr("y", 0.0), tag.getDoubleOr("z", 0.0),
            tag.getFloatOr("yaw", 0.0F), tag.getFloatOr("pitch", 0.0F));
    }
}
