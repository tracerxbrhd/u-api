package dev.uapi.client;

import dev.uapi.UApi;
import dev.uapi.client.sidebar.UApiSidebarButton;
import dev.uapi.client.sidebar.UApiSidebarButtons;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
        addScreenTabsIfSupported(event);

        if (event.getScreen() instanceof AbstractContainerScreen<?>) {
            addSidebarButtons(event);
        }
    }

    private static void addScreenTabsIfSupported(ScreenEvent.Init.Post event) {
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

    private static void addSidebarButtons(ScreenEvent.Init.Post event) {
        var buttons = UApiSidebarButtons.buttons();
        if (buttons.isEmpty()) return;

        int columns = UApiSidebarButtons.columns();

        for (int index = 0; index < buttons.size(); index++) {
            int column = index % columns;
            int row = index / columns;
            event.addListener(new UApiSidebarButton(
                UApiSidebarButtons.originX() + column * (UApiSidebarButtons.BUTTON_SIZE + UApiSidebarButtons.GAP),
                UApiSidebarButtons.originY() + row * (UApiSidebarButtons.BUTTON_SIZE + UApiSidebarButtons.GAP),
                UApiSidebarButtons.BUTTON_SIZE, buttons.get(index)));
        }
    }
}
