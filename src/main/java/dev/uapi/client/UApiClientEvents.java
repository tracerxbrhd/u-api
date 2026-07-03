package dev.uapi.client;

import dev.uapi.UApi;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = UApi.MOD_ID, value = Dist.CLIENT)
public final class UApiClientEvents {
    private UApiClientEvents() {}

    @SubscribeEvent
    public static void addScreenTabs(ScreenEvent.Init.Post event) {
        ResourceLocation selected;
        int left;
        int top;
        UApiTabSprites sprites = null;
        if (event.getScreen() instanceof InventoryScreen inventory) {
            selected = UApiScreenTabs.INVENTORY;
            left = inventory.getGuiLeft();
            top = inventory.getGuiTop() - 25;
        } else if (event.getScreen() instanceof UApiTabHost host) {
            selected = host.uApiTabId();
            left = host.uApiTabLeft();
            top = host.uApiTabTop();
            sprites = host.uApiTabSprites();
        } else return;

        int index = 0;
        for (UApiScreenTabs.Tab tab : UApiScreenTabs.tabs()) {
            event.addListener(new UApiTabButton(left + index * 26, top, tab, tab.id().equals(selected), sprites));
            index++;
        }
    }
}
