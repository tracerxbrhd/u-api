package dev.uapi.integration.sophisticatedbackpacks;

import dev.uapi.UApi;
import dev.uapi.client.UApiScreenTabs;
import dev.uapi.integration.IntegrationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.lang.reflect.Constructor;

/** Client-only bridge to Sophisticated Backpacks' native open-backpack request. */
public final class SophisticatedBackpacksClientIntegration {
    private static final String MOD_ID = "sophisticatedbackpacks";
    private static final String OPEN_PAYLOAD_CLASS =
        "net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload";
    private static final Identifier TAB_ID =
        Identifier.fromNamespaceAndPath(UApi.MOD_ID, "sophisticated_backpack");
    private static final Identifier BACKPACK_ITEM =
        Identifier.fromNamespaceAndPath(MOD_ID, "backpack");

    private static boolean bootstrapped;
    private static boolean sendFailureLogged;
    private static Constructor<? extends CustomPacketPayload> openPayloadConstructor;

    private SophisticatedBackpacksClientIntegration() {}

    /** Registers the inventory tab once, but only when the optional mod exposes its native payload. */
    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        if (!IntegrationService.isLoaded(MOD_ID)) return;

        try {
            Class<? extends CustomPacketPayload> payloadType = Class.forName(OPEN_PAYLOAD_CLASS, false,
                SophisticatedBackpacksClientIntegration.class.getClassLoader())
                .asSubclass(CustomPacketPayload.class);
            openPayloadConstructor = payloadType.getConstructor();
            UApiScreenTabs.register(TAB_ID, 50,
                Component.translatable("screen.u_api.sophisticated_backpack"),
                SophisticatedBackpacksClientIntegration::backpackIcon,
                SophisticatedBackpacksClientIntegration::openBackpack);
            UApi.LOGGER.info("Enabled native Sophisticated Backpacks inventory tab");
        } catch (ReflectiveOperationException | ClassCastException | LinkageError exception) {
            openPayloadConstructor = null;
            UApi.LOGGER.warn("Sophisticated Backpacks is installed but its native open payload is not compatible; "
                + "the U-API backpack tab is disabled", exception);
        }
    }

    private static ItemStack backpackIcon() {
        Item item = BuiltInRegistries.ITEM.getOptional(BACKPACK_ITEM).orElse(Items.CHEST);
        return new ItemStack(item);
    }

    private static Screen openBackpack(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.getConnection() == null || openPayloadConstructor == null)
            return null;
        try {
            ClientPacketDistributor.sendToServer(openPayloadConstructor.newInstance());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            if (!sendFailureLogged) {
                sendFailureLogged = true;
                UApi.LOGGER.warn("Failed to send Sophisticated Backpacks' native open request", exception);
            }
        }
        // The server opens the backpack menu asynchronously in response to the native payload.
        return null;
    }
}
