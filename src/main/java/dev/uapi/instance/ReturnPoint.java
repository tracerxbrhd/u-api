package dev.uapi.instance;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public record ReturnPoint(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
    public static ReturnPoint capture(ServerPlayer player) {
        return new ReturnPoint(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
            player.getYRot(), player.getXRot());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.location().toString());
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        return tag;
    }

    public static ReturnPoint load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.parse(tag.getString("dimension"));
        return new ReturnPoint(ResourceKey.create(Registries.DIMENSION, id), tag.getDouble("x"),
            tag.getDouble("y"), tag.getDouble("z"), tag.getFloat("yaw"), tag.getFloat("pitch"));
    }
}
