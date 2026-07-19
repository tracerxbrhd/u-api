package dev.uapi.client.ui.components;

import dev.uapi.client.ui.core.UIComponent;
import dev.uapi.client.ui.core.UIInputContext;
import dev.uapi.client.ui.core.UIRenderContext;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import java.util.function.DoubleConsumer;
import org.lwjgl.glfw.GLFW;

/** Finite stepped slider; values are normalized before listeners run. */
public final class UISlider extends UIComponent {
    private final double minimum;
    private final double maximum;
    private final double step;
    private final DoubleConsumer onChanged;
    private double value;

    public UISlider(double minimum, double maximum, double step, double value, DoubleConsumer onChanged) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum >= maximum) {
            throw new IllegalArgumentException("Slider range must be finite and increasing");
        }
        if (!Double.isFinite(step) || step <= 0) throw new IllegalArgumentException("Slider step must be positive");
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.onChanged = java.util.Objects.requireNonNull(onChanged, "onChanged");
        this.value = normalize(value);
    }

    public double value() {
        return value;
    }

    public void setValue(double value) {
        double normalized = normalize(value);
        if (Double.compare(this.value, normalized) == 0) return;
        this.value = normalized;
        onChanged.accept(normalized);
        invalidateRender();
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public boolean mouseClicked(UIInputContext context) {
        if (!enabled() || context.button() != 0) return false;
        requestFocus();
        double fraction = bounds().width() <= 1 ? 0 : (context.mouseX() - bounds().x()) / bounds().width();
        setValue(minimum + Math.max(0, Math.min(1, fraction)) * (maximum - minimum));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused() || !enabled()) return false;
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN) {
            setValue(value - step);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_UP) {
            setValue(value + step);
            return true;
        }
        return false;
    }

    @Override
    protected void renderComponent(UIRenderContext context) {
        int centerY = bounds().y() + bounds().height() / 2;
        int inset = Math.min(4, bounds().width() / 2);
        int left = bounds().x() + inset;
        int right = bounds().right() - inset;
        context.graphics().fill(left, centerY - 1, right, centerY + 1, theme().color(ColorToken.BORDER_DEFAULT));
        double fraction = (value - minimum) / (maximum - minimum);
        int knobX = left + (int) Math.round(Math.max(0, right - left) * fraction);
        context.graphics().fill(left, centerY - 1, knobX, centerY + 1, theme().color(ColorToken.ACCENT_PRIMARY));
        context.graphics().fill(knobX - 3, centerY - 5, knobX + 4, centerY + 6,
            theme().color(focused() ? ColorToken.BORDER_FOCUSED : ColorToken.TEXT_PRIMARY));
    }

    private double normalize(double candidate) {
        if (!Double.isFinite(candidate)) candidate = minimum;
        candidate = Math.max(minimum, Math.min(maximum, candidate));
        double stepped = minimum + Math.round((candidate - minimum) / step) * step;
        return Math.max(minimum, Math.min(maximum, stepped));
    }
}
