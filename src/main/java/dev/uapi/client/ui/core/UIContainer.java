package dev.uapi.client.ui.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Retained component group. Children are mounted once and disposed in reverse order. */
public class UIContainer extends UIComponent {
    private final List<UIComponent> children = new ArrayList<>();
    private UILayout layout = UILayout.absolute();

    public final <T extends UIComponent> T add(T child) {
        Objects.requireNonNull(child, "child");
        if (children.contains(child)) throw new IllegalArgumentException("Component is already a child");
        if (child.mounted()) throw new IllegalArgumentException("Component is already mounted elsewhere");
        if (child.parent() != null) throw new IllegalArgumentException("Component already belongs to another container");
        for (UIContainer ancestor = this; ancestor != null; ancestor = ancestor.parent()) {
            if (ancestor == child) throw new IllegalArgumentException("Component tree cannot contain a cycle");
        }
        child.attachTo(this);
        children.add(child);
        if (mounted()) {
            try {
                child.mount(screen(), this);
            } catch (RuntimeException exception) {
                children.remove(child);
                child.detachFrom(this);
                throw exception;
            }
        }
        invalidateLayout();
        return child;
    }

    public final boolean remove(UIComponent child) {
        if (!children.remove(child)) return false;
        try {
            child.unmount();
        } finally {
            child.detachFrom(this);
            invalidateLayout();
        }
        return true;
    }

    public final List<UIComponent> children() {
        return Collections.unmodifiableList(children);
    }

    public final int componentCount() {
        int count = 1;
        for (UIComponent child : children) {
            count += child instanceof UIContainer container ? container.componentCount() : 1;
        }
        return count;
    }

    public final UILayout layout() {
        return layout;
    }

    public final void setLayout(UILayout layout) {
        this.layout = Objects.requireNonNull(layout, "layout");
        invalidateLayout();
    }

    @Override
    public void tick() {
        for (UIComponent child : List.copyOf(children)) if (child.visible()) child.tick();
    }

    @Override
    public boolean mouseClicked(UIInputContext context) {
        for (int index = children.size() - 1; index >= 0; index--) {
            UIComponent child = children.get(index);
            if (child.visible() && child.enabled() && child.bounds().contains(context.mouseX(), context.mouseY())
                && child.mouseClicked(context)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (int index = children.size() - 1; index >= 0; index--) {
            UIComponent child = children.get(index);
            if (child.visible() && child.enabled() && child.bounds().contains(mouseX, mouseY)
                && child.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        }
        return false;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        for (UIComponent child : children) child.render(context);
    }

    @Override
    protected void onMounted() {
        int mountedChildren = 0;
        try {
            for (UIComponent child : children) {
                child.mount(screen(), this);
                mountedChildren++;
            }
        } catch (RuntimeException exception) {
            for (int index = mountedChildren - 1; index >= 0; index--) {
                try {
                    children.get(index).unmount();
                } catch (RuntimeException cleanupFailure) {
                    exception.addSuppressed(cleanupFailure);
                }
            }
            throw exception;
        }
    }

    @Override
    protected void onUnmounted() {
        RuntimeException failure = null;
        for (int index = children.size() - 1; index >= 0; index--) {
            try {
                children.get(index).unmount();
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }

    @Override
    void markLayoutValid() {
        super.markLayoutValid();
        for (UIComponent child : children) child.markLayoutValid();
    }


    void layoutTree() {
        layout.layout(this, bounds());
        for (UIComponent child : children) {
            if (child instanceof UIContainer container && child.visible()) container.layoutTree();
        }
    }

    void childLayoutInvalidated() {
        invalidateLayout();
    }

    void childRenderInvalidated() {
        invalidateRender();
    }
}
