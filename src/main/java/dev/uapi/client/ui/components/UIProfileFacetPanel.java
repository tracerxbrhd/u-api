package dev.uapi.client.ui.components;

import dev.uapi.api.profile.ProfileFacet;
import dev.uapi.api.profile.ProfileFacetField;
import dev.uapi.api.profile.ProfileFacetIcon;
import dev.uapi.api.profile.ProfileFacetIconType;
import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.Items;

/** Compact retained renderer for one already privacy-filtered profile facet. */
public final class UIProfileFacetPanel extends UIComponent {
    private ProfileFacet facet;

    public UIProfileFacetPanel(ProfileFacet facet) {
        this.facet = Objects.requireNonNull(facet, "facet");
    }

    public ProfileFacet facet() {
        return facet;
    }

    public void setFacet(ProfileFacet facet) {
        this.facet = Objects.requireNonNull(facet, "facet");
        invalidateRender();
    }

    /** Suggested height; callers may allocate less and the renderer clips to its assigned bounds. */
    public int suggestedHeight() {
        return 12 + facet.fields().size() * 12 + 8;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        context.graphics().fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(),
            theme().color(ColorToken.BACKGROUND_PANEL));
        UIRenderPrimitives.border(context.graphics(), bounds(), theme().color(ColorToken.BORDER_DEFAULT));
        if (bounds().width() <= 2 || bounds().height() <= 2) return;
        context.graphics().enableScissor(bounds().x() + 1, bounds().y() + 1,
            bounds().right() - 1, bounds().bottom() - 1);
        try {
            renderContent(context);
        } finally {
            context.graphics().disableScissor();
        }
    }

    private void renderContent(UIRenderContext context) {
        int left = bounds().x() + 5;
        int right = bounds().right() - 5;
        int y = bounds().y() + 4;
        int iconWidth = facet.icon().isPresent()
            ? renderIcon(context, facet.icon().orElseThrow(), left, y)
            : 0;
        int titleX = left + iconWidth;
        context.graphics().text(context.font(), facet.title().component(), titleX, y,
            theme().color(ColorToken.TEXT_PRIMARY), false);
        y += 13;
        for (ProfileFacetField field : facet.fields()) {
            if (y + context.font().lineHeight > bounds().bottom() - 3) break;
            Component label = field.label().component().copy().append(":");
            Component value = field.value().component();
            context.graphics().text(context.font(), label, left, y,
                theme().color(ColorToken.TEXT_MUTED), false);
            int valueX = Math.min(right, left + context.font().width(label) + 4);
            context.graphics().text(context.font(), value, valueX, y,
                theme().color(field.prominent() ? ColorToken.ACCENT_PRIMARY : ColorToken.TEXT_SECONDARY), false);
            y += 12;
        }
    }

    private static int renderIcon(UIRenderContext context, ProfileFacetIcon icon, int x, int y) {
        if (icon.type() == ProfileFacetIconType.ITEM) {
            var item = BuiltInRegistries.ITEM.getValue(icon.id());
            if (item != Items.AIR) {
                context.graphics().item(item.getDefaultInstance(), x, y - 3);
                return 20;
            }
        }
        if (icon.type() == ProfileFacetIconType.SPRITE) {
            context.graphics().blitSprite(RenderPipelines.GUI_TEXTURED, icon.id(), x, y - 3, 16, 16);
            return 20;
        }
        // Arbitrary provider textures have no universal UV/size contract. The owning screen may
        // render TEXTURE metadata itself; the generic fallback remains readable text.
        return 0;
    }
}
