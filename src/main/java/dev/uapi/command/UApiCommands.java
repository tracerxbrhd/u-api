package dev.uapi.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.uapi.instance.InstanceManager;
import dev.uapi.instance.InstanceView;
import dev.uapi.api.diagnostics.UApiDiagnostics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public final class UApiCommands {
    private UApiCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("api").requires(source -> source.hasPermission(2))
            .then(Commands.literal("status").executes(context -> {
                int count = InstanceManager.get(context.getSource().getServer()).all().size();
                context.getSource().sendSuccess(() -> Component.literal("U-API is active. Instances: " + count), false);
                return count;
            }))
            .then(Commands.literal("instances").executes(context -> {
                InstanceManager manager = InstanceManager.get(context.getSource().getServer());
                context.getSource().sendSuccess(() -> Component.literal("U-API instances:"), false);
                for (InstanceView instance : manager.all()) context.getSource().sendSuccess(() -> Component.literal(
                    instance.id() + " " + instance.type() + " " + instance.phase() + " owner=" + instance.ownerId()), false);
                return manager.all().size();
            }))
            .then(Commands.literal("diagnostics").executes(context -> {
                var snapshot = UApiDiagnostics.snapshot();
                context.getSource().sendSuccess(() -> Component.literal(
                    "U-API diagnostics: " + (snapshot.enabled() ? "enabled" : "disabled")
                        + ", services=" + snapshot.services().totalRegistrations()
                        + ", uiComponents=" + snapshot.activeUiComponents()
                        + ", overlays=" + snapshot.activeOverlays()), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    "UI avg layout/render µs=" + String.format(java.util.Locale.ROOT, "%.2f/%.2f",
                        snapshot.uiLayout().averageMicros(), snapshot.uiRender().averageMicros())
                        + ", invalidations=" + snapshot.uiLayoutInvalidations() + "/"
                        + snapshot.uiRenderInvalidations()), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    "Packets in/out=" + snapshot.packetRate().inbound() + "/" + snapshot.packetRate().outbound()
                        + ", head cache hit ratio=" + String.format(java.util.Locale.ROOT, "%.1f%%",
                        snapshot.playerHeadCache().hitRatio() * 100)), false);
                snapshot.extensionGauges().forEach((id, value) -> context.getSource().sendSuccess(
                    () -> Component.literal(id + "=" + value), false));
                return snapshot.services().totalRegistrations();
            }))
            .then(Commands.literal("complete").then(Commands.argument("id", StringArgumentType.word()).executes(context -> {
                UUID id = UUID.fromString(StringArgumentType.getString(context, "id"));
                InstanceManager manager = InstanceManager.get(context.getSource().getServer());
                manager.find(id).ifPresent(manager::returnAll);
                return manager.complete(id, "admin_command") ? 1 : 0;
            })))
            .then(Commands.literal("fail").then(Commands.argument("id", StringArgumentType.word()).executes(context -> {
                UUID id = UUID.fromString(StringArgumentType.getString(context, "id"));
                InstanceManager manager = InstanceManager.get(context.getSource().getServer());
                manager.find(id).ifPresent(manager::returnAll);
                return manager.fail(id, "admin_command") ? 1 : 0;
            })))
            .then(Commands.literal("cleanup").executes(context -> {
                InstanceManager manager = InstanceManager.get(context.getSource().getServer());
                var ids = manager.all().stream().filter(value -> value.phase().terminal()).map(InstanceView::id).toList();
                ids.forEach(id -> manager.remove(id, "admin_cleanup"));
                context.getSource().sendSuccess(() -> Component.literal("Removed " + ids.size() + " records."), true);
                return ids.size();
            }));
    }
}
