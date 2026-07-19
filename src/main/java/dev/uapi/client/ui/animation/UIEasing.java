package dev.uapi.client.ui.animation;

@FunctionalInterface
public interface UIEasing {
    UIEasing LINEAR = value -> value;
    UIEasing EASE_OUT_CUBIC = value -> 1 - Math.pow(1 - value, 3);
    UIEasing EASE_IN_OUT = value -> value < 0.5
        ? 4 * value * value * value
        : 1 - Math.pow(-2 * value + 2, 3) / 2;

    double apply(double normalizedProgress);
}
