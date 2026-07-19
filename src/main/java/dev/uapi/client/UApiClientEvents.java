package dev.uapi.client;

import dev.uapi.UApi;
import dev.uapi.client.sidebar.UApiSidebarButton;
import dev.uapi.client.sidebar.UApiSidebarButtons;
import dev.uapi.client.hud.UApiHud;
import dev.uapi.client.overlay.UApiWorldOverlays;
import dev.uapi.config.UApiClientConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

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

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        UApiHud.tick(Minecraft.getInstance());
        UApiWorldOverlays.tick();
    }

    @SubscribeEvent
    public static void captureWorldProjection(RenderLevelStageEvent event) {
        UApiWorldOverlays.captureProjection(event);
    }

    @SubscribeEvent
    public static void renderRegisteredHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (UApiClientConfigManager.showWorldOverlays()) {
            UApiWorldOverlays.render(minecraft, event.getGuiGraphics(), event.getPartialTick());
        }
        if (UApiClientConfigManager.showRegisteredHud()) {
            UApiHud.render(minecraft, event.getGuiGraphics(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void clearConnectionOverlays(ClientPlayerNetworkEvent.LoggingOut event) {
        UApiWorldOverlays.clear();
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
