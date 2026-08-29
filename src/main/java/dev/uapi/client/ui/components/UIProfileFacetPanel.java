package dev.uapi.client.ui.components;

import dev.uapi.api.profile.ProfileFacet;
import dev.uapi.api.profile.ProfileFacetEntry;
import dev.uapi.api.profile.ProfileFacetField;
import dev.uapi.api.profile.ProfileFacetIcon;
import dev.uapi.api.profile.ProfileFacetIconType;
import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Compact retained renderer for one already privacy-filtered profile facet. */
public final class UIProfileFacetPanel extends UIComponent {
    private record CachedMetadata(Component label, Component value, boolean prominent) {}
    private record CachedEntry(
        Component typeLabel,
        Component name,
        ItemStack icon,
        List<FormattedCharSequence> description,
        List<CachedMetadata> metadata,
        int height
    ) {}

    private ProfileFacet facet;
    private ProfileFacet cachedFacet;
    private int cachedEntryWidth = -1;
    private List<CachedEntry> cachedEntries = List.of();

    public UIProfileFacetPanel(ProfileFacet facet) {
        this.facet = Objects.requireNonNull(facet, "facet");
    }

    public ProfileFacet facet() {
        return facet;
    }

    public void setFacet(ProfileFacet facet) {
        this.facet = Objects.requireNonNull(facet, "facet");
        cachedFacet = null;
        cachedEntryWidth = -1;
        cachedEntries = List.of();
        invalidateRender();
    }

    /** Suggested height; callers may allocate less and the renderer clips to its assigned bounds. */
    public int suggestedHeight() {
        int entryHeight = facet.entries().stream()
            .mapToInt(entry -> 42 + entry.metadata().size() * 12).sum();
        int entryGaps = Math.max(0, facet.entries().size() - 1) * 4;
        return 20 + facet.fields().size() * 12 + entryHeight + entryGaps;
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
        context.graphics().drawString(context.font(), facet.title().component(), titleX, y,
            theme().color(ColorToken.TEXT_PRIMARY), false);
        y += 13;
        for (ProfileFacetField field : facet.fields()) {
            if (y + context.font().lineHeight > bounds().bottom() - 3) break;
            Component label = field.label().component().copy().append(":");
            Component value = field.value().component();
            context.graphics().drawString(context.font(), label, left, y,
                theme().color(ColorToken.TEXT_MUTED), false);
            int valueX = Math.min(right, left + context.font().width(label) + 4);
            context.graphics().drawString(context.font(), value, valueX, y,
                theme().color(field.prominent() ? ColorToken.ACCENT_PRIMARY : ColorToken.TEXT_SECONDARY), false);
            y += 12;
        }
        if (!facet.fields().isEmpty() && !facet.entries().isEmpty()) y += 3;
        ensureEntryCache(context, Math.max(24, right - left));
        for (CachedEntry entry : cachedEntries) {
            if (y >= bounds().bottom() - 3) break;
            renderEntry(context, entry, left, right, y);
            y += entry.height() + 4;
        }
    }

    private void ensureEntryCache(UIRenderContext context, int width) {
        if (cachedFacet == facet && cachedEntryWidth == width) return;
        cachedFacet = facet;
        cachedEntryWidth = width;
        List<CachedEntry> rebuilt = new ArrayList<>(facet.entries().size());
        for (ProfileFacetEntry entry : facet.entries()) {
            ItemStack icon = entry.icon();
            boolean hasIcon = !icon.isEmpty();
            int textLeft = hasIcon ? 22 : 0;
            int descriptionWidth = Math.max(24, width - textLeft - 8);
            List<FormattedCharSequence> description = entry.description().getString().isEmpty()
                ? List.of() : List.copyOf(context.font().split(entry.description(), descriptionWidth));
            List<CachedMetadata> metadata = entry.metadata().stream()
                .map(field -> new CachedMetadata(
                    field.label().component().copy().append(":"),
                    field.value().component(),
                    field.prominent()))
                .toList();
            int textHeight = 22 + description.size() * 10 + metadata.size() * 12;
            rebuilt.add(new CachedEntry(
                entry.typeLabel().component(),
                entry.name(),
                icon,
                description,
                metadata,
                Math.max(36, textHeight + 7)));
        }
        cachedEntries = List.copyOf(rebuilt);
    }

    private void renderEntry(UIRenderContext context, CachedEntry entry, int left, int right, int y) {
        int bottom = Math.min(bounds().bottom() - 3, y + entry.height());
        context.graphics().fill(left, y, right, bottom,
            theme().color(ColorToken.BACKGROUND_SECONDARY));
        context.graphics().fill(left, y, right, y + 1,
            theme().color(ColorToken.BORDER_DEFAULT));
        context.graphics().fill(left, bottom - 1, right, bottom,
            theme().color(ColorToken.BORDER_DEFAULT));
        int textX = left + 5;
        if (!entry.icon().isEmpty()) {
            context.graphics().renderItem(entry.icon(), textX, y + 8);
            textX += 22;
        }
        int textWidth = Math.max(12, right - textX - 5);
        context.graphics().drawString(context.font(), entry.typeLabel(), textX, y + 4,
            theme().color(ColorToken.TEXT_MUTED), false);
        List<FormattedCharSequence> nameLines = context.font().split(entry.name(), textWidth);
        if (!nameLines.isEmpty()) {
            context.graphics().drawString(context.font(), nameLines.getFirst(), textX, y + 15,
                theme().color(ColorToken.ACCENT_PRIMARY), false);
        }
        int lineY = y + 27;
        for (CachedMetadata metadata : entry.metadata()) {
            if (lineY + context.font().lineHeight > bottom - 3) return;
            context.graphics().drawString(context.font(), metadata.label(), textX, lineY,
                theme().color(ColorToken.TEXT_MUTED), false);
            int valueX = Math.min(right - 5,
                textX + context.font().width(metadata.label()) + 4);
            context.graphics().drawString(context.font(), metadata.value(), valueX, lineY,
                theme().color(metadata.prominent()
                    ? ColorToken.ACCENT_PRIMARY : ColorToken.TEXT_SECONDARY), false);
            lineY += 12;
        }
        for (FormattedCharSequence line : entry.description()) {
            if (lineY + context.font().lineHeight > bottom - 3) return;
            context.graphics().drawString(context.font(), line, textX, lineY,
                theme().color(ColorToken.TEXT_SECONDARY), false);
            lineY += 10;
        }
    }

    private static int renderIcon(UIRenderContext context, ProfileFacetIcon icon, int x, int y) {
        if (icon.type() == ProfileFacetIconType.ITEM) {
            var item = BuiltInRegistries.ITEM.get(icon.id());
            if (item != Items.AIR) {
                context.graphics().renderItem(item.getDefaultInstance(), x, y - 3);
                return 20;
            }
        }
        if (icon.type() == ProfileFacetIconType.SPRITE) {
            context.graphics().blitSprite(icon.id(), x, y - 3, 16, 16);
            return 20;
        }
        // Arbitrary provider textures have no universal UV/size contract. The owning screen may
        // render TEXTURE metadata itself; the generic fallback remains readable text.
        return 0;
    }
}
