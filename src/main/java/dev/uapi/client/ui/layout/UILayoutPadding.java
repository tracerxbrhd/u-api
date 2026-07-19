package dev.uapi.client.ui.layout;

/** Non-negative padding used by the built-in layouts. */
public record UILayoutPadding(int left, int top, int right, int bottom) {
    public static final UILayoutPadding NONE = new UILayoutPadding(0, 0, 0, 0);

    public UILayoutPadding {
        if (left < 0 || top < 0 || right < 0 || bottom < 0) {
            throw new IllegalArgumentException("Layout padding cannot be negative");
        }
    }

    public static UILayoutPadding all(int amount) {
        return new UILayoutPadding(amount, amount, amount, amount);
    }
}
