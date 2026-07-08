package dev.uapi.client;

import dev.uapi.UApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Client-only registry used by addon mods to attach screens to the shared inventory tab bar. */
public final class UApiScreenTabs {
    public static final ResourceLocation INVENTORY = ResourceLocation.fromNamespaceAndPath(UApi.MOD_ID, "inventory");
    private static final Map<ResourceLocation, Tab> TABS = new LinkedHashMap<>();

    static {
        register(INVENTORY, 0, Component.translatable("screen.u_api.inventory"),
            () -> new ItemStack(Items.CHEST), minecraft -> minecraft.player == null
                ? null : new InventoryScreen(minecraft.player));
    }

    private UApiScreenTabs() {}

    public static synchronized void register(ResourceLocation id, int order, Component title,
                                             Supplier<ItemStack> icon, Function<Minecraft, Screen> opener) {
        register(id, order, title, icon, () -> true, opener);
    }

    public static synchronized void register(ResourceLocation id, int order, Component title,
                                             Supplier<ItemStack> icon, BooleanSupplier visible,
                                             Function<Minecraft, Screen> opener) {
        TABS.put(id, new Tab(id, order, title, icon, null, visible, opener));
    }

    /** Registers a tab backed by a 16x16 texture so resource packs can replace its icon. */
    public static synchronized void register(ResourceLocation id, int order, Component title,
                                             ResourceLocation textureIcon, Function<Minecraft, Screen> opener) {
        register(id, order, title, textureIcon, () -> true, opener);
    }

    /** Registers a resource-pack-backed tab whose visibility can follow synchronized server rules. */
    public static synchronized void register(ResourceLocation id, int order, Component title,
                                             ResourceLocation textureIcon, BooleanSupplier visible,
                                             Function<Minecraft, Screen> opener) {
        TABS.put(id, new Tab(id, order, title, null, textureIcon, visible, opener));
    }

    public static synchronized List<Tab> tabs() {
        List<Tab> result = new ArrayList<>();
        for (Tab tab : TABS.values()) if (tab.visible().getAsBoolean()) result.add(tab);
        result.sort(Comparator.comparingInt(Tab::order).thenComparing(value -> value.id().toString()));
        return List.copyOf(result);
    }

    public record Tab(ResourceLocation id, int order, Component title, Supplier<ItemStack> itemIcon,
                      ResourceLocation textureIcon, BooleanSupplier visible, Function<Minecraft, Screen> opener) {}
}
