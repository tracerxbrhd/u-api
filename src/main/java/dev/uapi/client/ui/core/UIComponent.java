package dev.uapi.client.ui.core;

import dev.uapi.client.ui.state.UIState;
import dev.uapi.api.diagnostics.UApiDiagnostics;
import dev.uapi.client.ui.theme.UITheme;
import dev.uapi.client.ui.theme.UIThemes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Base class for retained client components with deterministic mount/unmount cleanup. */
public abstract class UIComponent {
    private final List<AutoCloseable> subscriptions = new ArrayList<>();
    private UIBounds bounds = UIBounds.EMPTY;
    private UIContainer parent;
    private UIScreen screen;
    private UITheme theme;
    private boolean mounted;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean focused;
    private boolean layoutInvalid = true;
    private boolean renderInvalid = true;

    public final UIBounds bounds() {
        return bounds;
    }

    public final void setBounds(int x, int y, int width, int height) {
        setBounds(new UIBounds(x, y, width, height));
    }

    public final void setBounds(UIBounds nextBounds) {
        nextBounds = Objects.requireNonNull(nextBounds, "nextBounds");
        if (bounds.equals(nextBounds)) return;
        UIBounds previousBounds = bounds;
        bounds = nextBounds;
        try {
            onBoundsChanged(previousBounds, nextBounds);
        } finally {
            invalidateLayout();
        }
    }

    public final boolean visible() {
        return visible;
    }

    public final void setVisible(boolean visible) {
        if (this.visible == visible) return;
        this.visible = visible;
        try {
            if (!visible && mounted) screen.clearUiFocusWithin(this);
        } finally {
            invalidateLayout();
        }
    }

    public final boolean enabled() {
        return enabled;
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        try {
            if (!enabled && mounted) screen.clearUiFocusWithin(this);
        } finally {
            invalidateRender();
        }
    }

    public final boolean focused() {
        return focused;
    }

    /** Whether this component participates in keyboard focus traversal. */
    public boolean focusable() {
        return false;
    }

    public final void setTheme(UITheme theme) {
        if (this.theme == theme) return;
        this.theme = theme;
        invalidateRender();
    }

    protected final UITheme theme() {
        if (theme != null) return theme;
        return parent == null ? UIThemes.DEFAULT : parent.theme();
    }

    final UITheme resolvedTheme() {
        return theme();
    }

    public final boolean mounted() {
        return mounted;
    }

    public final UIScreen screen() {
        if (screen == null) throw new IllegalStateException("Component is not mounted");
        return screen;
    }

    public final void invalidateLayout() {
        UApiDiagnostics.recordUiLayoutInvalidation();
        layoutInvalid = true;
        renderInvalid = true;
        if (parent != null) parent.childLayoutInvalidated();
    }

    public final void invalidateRender() {
        UApiDiagnostics.recordUiRenderInvalidation();
        renderInvalid = true;
        if (parent != null) parent.childRenderInvalidated();
    }

    public final boolean layoutInvalid() {
        return layoutInvalid;
    }

    public final boolean renderInvalid() {
        return renderInvalid;
    }

    public void tick() {
    }

    public boolean mouseClicked(UIInputContext context) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    protected abstract void renderComponent(UIRenderContext context);

    protected void onMounted() {
    }

    protected void onUnmounted() {
    }

    protected void onFocusChanged(boolean focused) {
    }

    protected void onBoundsChanged(UIBounds previousBounds, UIBounds nextBounds) {
    }

    protected final void requestFocus() {
        if (!mounted || !enabled) return;
        screen.requestUiFocus(this);
    }

    protected final void clearFocus() {
        if (mounted) screen.clearUiFocus(this);
    }

    protected final <T> void observe(UIState<T> state, Consumer<? super T> listener) {
        if (!mounted) throw new IllegalStateException("State can only be observed by a mounted component");
        subscriptions.add(state.subscribe(listener));
    }

    final void mount(UIScreen owner, UIContainer newParent) {
        if (mounted) throw new IllegalStateException("Component is already mounted");
        if (parent != newParent) throw new IllegalStateException("Component is mounted through the wrong container");
        screen = Objects.requireNonNull(owner, "owner");
        mounted = true;
        UApiDiagnostics.uiComponentMounted();
        try {
            onMounted();
        } catch (RuntimeException exception) {
            try {
                onUnmounted();
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            disposeSubscriptions(exception);
            UApiDiagnostics.uiComponentUnmounted();
            mounted = false;
            screen = null;
            throw exception;
        }
    }

    final void unmount() {
        if (!mounted) return;
        RuntimeException failure = null;
        try {
            screen.clearUiFocusWithin(this);
        } catch (RuntimeException exception) {
            failure = exception;
        }
        try {
            onUnmounted();
        } catch (RuntimeException exception) {
            if (failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }
        failure = disposeSubscriptions(failure);
        mounted = false;
        screen = null;
        UApiDiagnostics.uiComponentUnmounted();
        if (failure != null) throw failure;
    }

    final void render(UIRenderContext context) {
        if (!visible) return;
        renderComponent(context);
        renderInvalid = false;
    }

    void markLayoutValid() {
        layoutInvalid = false;
    }

    final void setFocusedFromScreen(boolean focused) {
        if (this.focused == focused) return;
        this.focused = focused;
        onFocusChanged(focused);
        invalidateRender();
    }

    final UIContainer parent() {
        return parent;
    }

    final void attachTo(UIContainer owner) {
        if (parent != null) throw new IllegalStateException("Component already has a parent");
        parent = Objects.requireNonNull(owner, "owner");
    }

    final void detachFrom(UIContainer expected) {
        if (parent != expected) throw new IllegalStateException("Component parent changed unexpectedly");
        if (mounted) throw new IllegalStateException("A mounted component cannot be detached");
        parent = null;
    }

    final boolean effectivelyFocusable() {
        if (!mounted || !visible || !enabled || !focusable()) return false;
        for (UIContainer ancestor = parent; ancestor != null; ancestor = ancestor.parent()) {
            if (!ancestor.visible() || !ancestor.enabled()) return false;
        }
        return true;
    }

    private RuntimeException disposeSubscriptions(RuntimeException failure) {
        for (int index = subscriptions.size() - 1; index >= 0; index--) {
            try {
                subscriptions.get(index).close();
            } catch (Exception exception) {
                if (failure == null) failure = new IllegalStateException("Failed to dispose UI subscription", exception);
                else failure.addSuppressed(exception);
            }
        }
        subscriptions.clear();
        return failure;
    }
}
