package dev.uapi.client;

import net.minecraft.resources.ResourceLocation;

/** Resource-pack replaceable visual states for tabs hosted by a custom screen. */
public record UApiTabSprites(ResourceLocation normal, ResourceLocation hovered,
                             ResourceLocation pressed, ResourceLocation selected,
                             ResourceLocation disabled) {}
