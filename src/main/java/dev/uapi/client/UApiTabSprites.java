package dev.uapi.client;

import net.minecraft.resources.Identifier;

/** Resource-pack replaceable visual states for tabs hosted by a custom screen. */
public record UApiTabSprites(Identifier normal, Identifier hovered,
                             Identifier pressed, Identifier selected,
                             Identifier disabled) {}
