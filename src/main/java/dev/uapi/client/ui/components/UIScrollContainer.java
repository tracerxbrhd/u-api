package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.core.UIContainer;
import dev.uapi.client.ui.core.UIBounds;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Clipping container with bounded scroll state.
 *
 * <p>The owner uses {@link #scrollOffset()} during layout to offset its content; this keeps layout
 * deterministic and avoids mutating the component tree from render.</p>
 */
public class UIScrollContainer extends UIContainer {
    private int contentHeight;
    private int scrollOffset;
    private int wheelStep = 18;
    private IntConsumer onScrollChanged = ignored -> {};

    public int scrollOffset() {
        return scrollOffset;
    }

    public void setContentHeight(int contentHeight) {
        if (contentHeight < 0) throw new IllegalArgumentException("Scroll content height cannot be negative");
        if (this.contentHeight == contentHeight) return;
        this.contentHeight = contentHeight;
        setScrollOffset(scrollOffset);
        invalidateRender();
    }

    public void setWheelStep(int wheelStep) {
        if (wheelStep <= 0) throw new IllegalArgumentException("Wheel step must be positive");
        this.wheelStep = wheelStep;
    }

    public void setOnScrollChanged(IntConsumer listener) {
        onScrollChanged = Objects.requireNonNull(listener, "listener");
    }

    public void setScrollOffset(int offset) {
        int next = Math.max(0, Math.min(maxScroll(), offset));
        if (scrollOffset == next) return;
        scrollOffset = next;
        onScrollChanged.accept(next);
        invalidateLayout();
    }

    public int maxScroll() {
        return Math.max(0, contentHeight - bounds().height());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        if (!enabled() || scrollY == 0 || maxScroll() == 0) return false;
        int previous = scrollOffset;
        long requested = scrollOffset - (long) Math.signum(scrollY) * wheelStep;
        setScrollOffset((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, requested)));
        return previous != scrollOffset;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        if (bounds().width() == 0 || bounds().height() == 0) return;
        context.graphics().enableScissor(bounds().x(), bounds().y(), bounds().right(), bounds().bottom());
        try {
            super.renderComponent(context);
        } finally {
            context.graphics().disableScissor();
        }
    }

    @Override
    protected void onBoundsChanged(UIBounds previousBounds, UIBounds nextBounds) {
        int clamped = Math.max(0, Math.min(Math.max(0, contentHeight - nextBounds.height()), scrollOffset));
        if (clamped == scrollOffset) return;
        scrollOffset = clamped;
        onScrollChanged.accept(clamped);
    }
}
