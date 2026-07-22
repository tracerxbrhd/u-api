package dev.uapi.integration.jei;

import dev.uapi.UApi;
import dev.uapi.client.sidebar.UApiSidebarButtons;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

import java.util.List;

@JeiPlugin
public final class UApiJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(UApi.MOD_ID, "jei");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(AbstractContainerScreen.class, new HelperButtonAreaHandler());
    }

    private static final class HelperButtonAreaHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {
        @Override
        public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> screen) {
            int width = UApiSidebarButtons.areaWidth();
            int height = UApiSidebarButtons.areaHeight();
            if (width <= 0 || height <= 0) return List.of();
            return List.of(new Rect2i(UApiSidebarButtons.originX(), UApiSidebarButtons.originY(), width, height));
        }
    }
}
