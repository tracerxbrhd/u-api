package dev.uapi.client.ui.core;

/** Immutable GUI-space bounds used by retained U-API components. */
public record UIBounds(int x, int y, int width, int height) {
    public static final UIBounds EMPTY = new UIBounds(0, 0, 0, 0);

    public UIBounds {
        if (width < 0 || height < 0) throw new IllegalArgumentException("UI bounds cannot have a negative size");
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < (long) x + width
            && mouseY >= y && mouseY < (long) y + height;
    }

    public int right() {
        return saturatedAdd(x, width);
    }

    public int bottom() {
        return saturatedAdd(y, height);
    }

    public UIBounds inset(int amount) {
        return inset(amount, amount, amount, amount);
    }

    public UIBounds inset(int left, int top, int right, int bottom) {
        int insetLeft = Math.min(width, Math.max(0, left));
        int insetTop = Math.min(height, Math.max(0, top));
        int insetRight = Math.min(width - insetLeft, Math.max(0, right));
        int insetBottom = Math.min(height - insetTop, Math.max(0, bottom));
        return new UIBounds(saturatedAdd(x, insetLeft), saturatedAdd(y, insetTop),
            width - insetLeft - insetRight, height - insetTop - insetBottom);
    }

    public UIBounds translate(int deltaX, int deltaY) {
        return new UIBounds(saturatedAdd(x, deltaX), saturatedAdd(y, deltaY), width, height);
    }

    public boolean intersects(UIBounds other) {
        return other != null && (long) x < (long) other.x + other.width
            && (long) x + width > other.x
            && (long) y < (long) other.y + other.height
            && (long) y + height > other.y;
    }

    private static int saturatedAdd(int left, int right) {
        long result = (long) left + right;
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, result));
    }
}
