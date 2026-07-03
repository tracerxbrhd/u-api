package dev.uapi.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Central command root extended by U-API modules during mod construction. */
public final class UApiCommandRegistry {
    private static final Map<String, Supplier<LiteralArgumentBuilder<CommandSourceStack>>> SECTIONS =
        new LinkedHashMap<>();

    private UApiCommandRegistry() {}

    public static synchronized void registerSection(String name,
            Supplier<LiteralArgumentBuilder<CommandSourceStack>> factory) {
        if (name == null || !name.matches("[a-z0-9_]+"))
            throw new IllegalArgumentException("Invalid /uapi section name: " + name);
        if (SECTIONS.putIfAbsent(name, factory) != null)
            throw new IllegalStateException("Duplicate /uapi section: " + name);
    }

    @SubscribeEvent
    public static synchronized void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("uapi")
            .executes(context -> {
                context.getSource().sendSuccess(() -> Component.literal(
                    "U-API commands: " + String.join(", ", SECTIONS.keySet())), false);
                return SECTIONS.size();
            });
        SECTIONS.forEach((name, factory) -> {
            LiteralArgumentBuilder<CommandSourceStack> section = factory.get();
            if (!name.equals(section.getLiteral()))
                throw new IllegalStateException("/uapi section factory for '" + name + "' produced '" +
                    section.getLiteral() + "'");
            root.then(section);
        });
        event.getDispatcher().register(root);
    }
}
