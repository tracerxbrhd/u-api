package dev.uapi.client.ui.core;

import dev.uapi.api.diagnostics.UApiDiagnostics;
import dev.uapi.client.ui.navigation.UIFocusTraversal;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Screen base that owns a retained component tree.
 *
 * <p>The tree is built once for a screen lifetime, re-laid out only when invalidated, and fully
 * unmounted from {@link #removed()}.</p>
 */
public abstract class UIScreen extends Screen {
    private UIContainer uiRoot;
    private UIComponent focusedUiComponent;
    private boolean layingOut;

    protected UIScreen(Component title) {
        super(title);
    }

    @Override
    protected final void init() {
        ensureUiTree();
        uiRoot.setBounds(0, 0, width, height);
        reflowUi();
        initScreen();
    }

    protected void buildUi(UIContainer root) {
    }

    protected void layoutUi(UIContainer root) {
    }

    protected void initScreen() {
    }

    protected void tickScreen() {
    }

    /** Draws screen-specific surfaces between the common backdrop and retained component tree. */
    protected void renderScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public final void tick() {
        if (uiRoot != null) uiRoot.tick();
        tickScreen();
    }

    @Override
    public final void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ensureUiTree();
        if (uiRoot.layoutInvalid()) reflowUi();
        long renderStarted = UApiDiagnostics.startTimer();
        try {
            renderScreenBackground(graphics, mouseX, mouseY, partialTick);
            renderScreen(graphics, mouseX, mouseY, partialTick);
            uiRoot.render(new UIRenderContext(minecraft, graphics, font, mouseX, mouseY, partialTick));
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        } finally {
            UApiDiagnostics.recordUiRenderTime(renderStarted);
        }
    }

    /** Draws the theme-backed retained-screen backdrop before any screen content. */
    protected void renderScreenBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int primary = uiRoot.resolvedTheme().color(ColorToken.BACKGROUND_PRIMARY);
        graphics.fill(0, 0, width, height, primary & 0x00FFFFFF | 0xA8000000);
    }

    /**
     * Vanilla calls this while extracting screen state. The retained lifecycle already rendered its
     * background before its content; running the vanilla blur here would blur the completed tree.
     */
    @Override
    public final void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** First-party management screens keep the integrated server running consistently. */
    @Override
    public final boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubledClick) {
        ensureUiTree();
        if (uiRoot.mouseClicked(new UIInputContext(event.x(), event.y(), event.button()))) return true;
        clearUiFocus(null);
        return super.mouseClicked(event, doubledClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ensureUiTree();
        return uiRoot.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
            || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (focusedUiComponent != null
            && focusedUiComponent.keyPressed(event.key(), event.scancode(), event.modifiers())) return true;
        if (event.key() == GLFW.GLFW_KEY_TAB && focusNext((event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return focusedUiComponent != null && focusedUiComponent.charTyped((char) event.codepoint(), 0)
            || super.charTyped(event);
    }

    @Override
    public final void removed() {
        RuntimeException failure = null;
        try {
            removedScreen();
        } catch (RuntimeException exception) {
            failure = exception;
        }
        if (uiRoot != null) {
            try {
                uiRoot.unmount();
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else failure.addSuppressed(exception);
            } finally {
                focusedUiComponent = null;
                uiRoot = null;
            }
        }
        try {
            super.removed();
        } catch (RuntimeException exception) {
            if (failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }
        if (failure != null) throw failure;
    }

    /** Subclass cleanup hook; retained component subscriptions are disposed immediately after it. */
    protected void removedScreen() {
    }

    protected final UIContainer uiRoot() {
        ensureUiTree();
        return uiRoot;
    }

    public final int activeUiComponentCount() {
        return uiRoot == null ? 0 : uiRoot.componentCount();
    }

    final void requestUiFocus(UIComponent component) {
        if (component.screen() != this || !component.effectivelyFocusable()) return;
        if (focusedUiComponent == component) return;
        UIComponent previous = focusedUiComponent;
        focusedUiComponent = null;
        if (previous != null) previous.setFocusedFromScreen(false);
        setFocused(null);
        focusedUiComponent = component;
        component.setFocusedFromScreen(true);
    }

    final void clearUiFocus(UIComponent expected) {
        if (focusedUiComponent == null || expected != null && focusedUiComponent != expected) return;
        UIComponent previous = focusedUiComponent;
        focusedUiComponent = null;
        previous.setFocusedFromScreen(false);
    }

    final void clearUiFocusWithin(UIComponent ancestor) {
        if (focusedUiComponent == null) return;
        for (UIComponent current = focusedUiComponent; current != null; current = current.parent()) {
            if (current == ancestor) {
                clearUiFocus(focusedUiComponent);
                return;
            }
        }
    }

    private void ensureUiTree() {
        if (uiRoot != null) return;
        UIContainer candidate = new UIContainer();
        candidate.mount(this, null);
        uiRoot = candidate;
        try {
            buildUi(candidate);
        } catch (RuntimeException exception) {
            try {
                candidate.unmount();
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            uiRoot = null;
            throw exception;
        }
    }

    private void reflowUi() {
        if (layingOut) throw new IllegalStateException("Recursive UI layout invalidation");
        layingOut = true;
        long layoutStarted = UApiDiagnostics.startTimer();
        try {
            layoutUi(uiRoot);
            uiRoot.layoutTree();
            uiRoot.markLayoutValid();
        } finally {
            UApiDiagnostics.recordUiLayoutTime(layoutStarted);
            layingOut = false;
        }
    }

    private boolean focusNext(boolean reverse) {
        ensureUiTree();
        UIComponent next = UIFocusTraversal.next(uiRoot, focusedUiComponent, reverse).orElse(null);
        if (next == null) return false;
        requestUiFocus(next);
        return true;
    }
}
