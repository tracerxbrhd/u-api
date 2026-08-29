package dev.uapi.integration.sophisticatedbackpacks;

import dev.uapi.UApi;
import dev.uapi.client.UApiScreenTabs;
import dev.uapi.integration.IntegrationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;

/** Client-only bridge to Sophisticated Backpacks' native open-backpack request. */
public final class SophisticatedBackpacksClientIntegration {
    private static final String MOD_ID = "sophisticatedbackpacks";
    private static final String OPEN_PAYLOAD_CLASS =
        "net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload";
    private static final String INVENTORY_PROVIDER_CLASS =
        "net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider";
    private static final String BACKPACK_SLOT_CONSUMER_CLASS = INVENTORY_PROVIDER_CLASS
        + "$BackpackInventorySlotConsumer";
    private static final Set<String> EQUIPPED_HANDLER_NAMES = Set.of("armor", "curios", "accessories");
    private static final long LOOKUP_CACHE_NANOS = 50_000_000L;
    private static final ResourceLocation TAB_ID =
        ResourceLocation.fromNamespaceAndPath(UApi.MOD_ID, "sophisticated_backpack");
    private static final ResourceLocation BACKPACK_ITEM =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "backpack");

    private static boolean bootstrapped;
    private static boolean sendFailureLogged;
    private static boolean lookupFailureLogged;
    private static Constructor<? extends CustomPacketPayload> openPayloadConstructor;
    private static Method inventoryProviderGetMethod;
    private static Method runOnBackpacksMethod;
    private static Class<?> backpackSlotConsumerType;
    private static Player cachedLookupPlayer;
    private static long cachedLookupNanos;
    private static boolean cachedLookupValid;
    private static Optional<EquippedBackpack> cachedLookupResult = Optional.empty();

    private SophisticatedBackpacksClientIntegration() {}

    /** Registers the inventory tab once, but only when the optional mod exposes its native APIs. */
    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        if (!IntegrationService.isLoaded(MOD_ID)) return;

        try {
            Class<? extends CustomPacketPayload> payloadType = Class.forName(OPEN_PAYLOAD_CLASS, false,
                SophisticatedBackpacksClientIntegration.class.getClassLoader())
                .asSubclass(CustomPacketPayload.class);
            Class<?> inventoryProviderType = Class.forName(INVENTORY_PROVIDER_CLASS, false,
                SophisticatedBackpacksClientIntegration.class.getClassLoader());
            backpackSlotConsumerType = Class.forName(BACKPACK_SLOT_CONSUMER_CLASS, false,
                SophisticatedBackpacksClientIntegration.class.getClassLoader());
            openPayloadConstructor = payloadType.getConstructor(int.class, String.class, String.class);
            inventoryProviderGetMethod = inventoryProviderType.getMethod("get");
            runOnBackpacksMethod = inventoryProviderType.getMethod("runOnBackpacks", Player.class,
                backpackSlotConsumerType);
            UApiScreenTabs.register(TAB_ID, 50,
                Component.translatable("screen.u_api.sophisticated_backpack"),
                SophisticatedBackpacksClientIntegration::backpackIcon,
                SophisticatedBackpacksClientIntegration::hasEquippedBackpack,
                SophisticatedBackpacksClientIntegration::openBackpack);
            UApi.LOGGER.info("Enabled equipped-only Sophisticated Backpacks inventory tab");
        } catch (ReflectiveOperationException | ClassCastException | LinkageError exception) {
            openPayloadConstructor = null;
            inventoryProviderGetMethod = null;
            runOnBackpacksMethod = null;
            backpackSlotConsumerType = null;
            UApi.LOGGER.warn("Sophisticated Backpacks is installed but its native inventory APIs are not "
                + "compatible; the U-API backpack tab is disabled", exception);
        }
    }

    private static ItemStack backpackIcon() {
        Optional<EquippedBackpack> equippedBackpack = findEquippedBackpack(Minecraft.getInstance());
        if (equippedBackpack.isPresent()) return equippedBackpack.get().stack().copy();
        Item item = BuiltInRegistries.ITEM.getOptional(BACKPACK_ITEM).orElse(Items.CHEST);
        return new ItemStack(item);
    }

    private static boolean hasEquippedBackpack() {
        return findEquippedBackpack(Minecraft.getInstance()).isPresent();
    }

    private static Screen openBackpack(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.getConnection() == null || openPayloadConstructor == null)
            return null;
        Optional<EquippedBackpack> equippedBackpack = scanEquippedBackpack(minecraft);
        if (equippedBackpack.isEmpty()) return null;
        EquippedBackpack backpack = equippedBackpack.get();
        try {
            PacketDistributor.sendToServer(openPayloadConstructor.newInstance(
                backpack.slot(), backpack.identifier(), backpack.handlerName()));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            if (!sendFailureLogged) {
                sendFailureLogged = true;
                UApi.LOGGER.warn("Failed to send Sophisticated Backpacks' native open request", exception);
            }
        }
        // The server opens the backpack menu asynchronously in response to the native payload.
        return null;
    }

    private static Optional<EquippedBackpack> findEquippedBackpack(Minecraft minecraft) {
        if (minecraft.player == null) return Optional.empty();
        long now = System.nanoTime();
        if (cachedLookupValid && minecraft.player == cachedLookupPlayer
            && now - cachedLookupNanos < LOOKUP_CACHE_NANOS)
            return cachedLookupResult;

        Optional<EquippedBackpack> result = scanEquippedBackpack(minecraft);
        cachedLookupPlayer = minecraft.player;
        cachedLookupNanos = now;
        cachedLookupValid = true;
        cachedLookupResult = result;
        return result;
    }

    private static Optional<EquippedBackpack> scanEquippedBackpack(Minecraft minecraft) {
        if (minecraft.player == null || inventoryProviderGetMethod == null || runOnBackpacksMethod == null
            || backpackSlotConsumerType == null) return Optional.empty();

        EquippedBackpack[] found = new EquippedBackpack[1];
        try {
            Object consumer = Proxy.newProxyInstance(backpackSlotConsumerType.getClassLoader(),
                new Class<?>[] {backpackSlotConsumerType}, (proxy, method, arguments) -> {
                    if (method.getName().equals("accept") && arguments != null && arguments.length == 4) {
                        String handlerName = (String) arguments[1];
                        if (!EQUIPPED_HANDLER_NAMES.contains(handlerName)) return false;
                        found[0] = new EquippedBackpack(((ItemStack) arguments[0]).copy(), handlerName,
                            (String) arguments[2], ((Number) arguments[3]).intValue());
                        return true;
                    }
                    if (method.getName().equals("toString")) return "U-API equipped backpack lookup";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == arguments[0];
                    return false;
                });
            Object inventoryProvider = inventoryProviderGetMethod.invoke(null);
            runOnBackpacksMethod.invoke(inventoryProvider, minecraft.player, consumer);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            if (!lookupFailureLogged) {
                lookupFailureLogged = true;
                UApi.LOGGER.warn("Failed to find an equipped Sophisticated Backpacks backpack", exception);
            }
            return Optional.empty();
        }
        return Optional.ofNullable(found[0]);
    }

    private record EquippedBackpack(ItemStack stack, String handlerName, String identifier, int slot) {}
}
