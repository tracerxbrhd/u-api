package dev.uapi.client.ui.theme;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable semantic theme. Components consume tokens instead of mod-specific color names. */
public final class UITheme {
    public enum ColorToken {
        BACKGROUND_PRIMARY,
        BACKGROUND_SECONDARY,
        BACKGROUND_PANEL,
        BORDER_DEFAULT,
        BORDER_FOCUSED,
        TEXT_PRIMARY,
        TEXT_SECONDARY,
        TEXT_MUTED,
        ACCENT_PRIMARY,
        ACCENT_SUCCESS,
        ACCENT_WARNING,
        ACCENT_DANGER
    }

    public enum SpacingToken { SMALL, MEDIUM, LARGE }

    public enum RadiusToken { SMALL, MEDIUM }

    public enum AnimationToken { FAST, NORMAL }

    private final Map<ColorToken, Integer> colors;
    private final Map<SpacingToken, Integer> spacing;
    private final Map<RadiusToken, Integer> radii;
    private final Map<AnimationToken, Duration> animations;

    private UITheme(Builder builder) {
        colors = Map.copyOf(builder.colors);
        spacing = Map.copyOf(builder.spacing);
        radii = Map.copyOf(builder.radii);
        animations = Map.copyOf(builder.animations);
        requireComplete(ColorToken.values(), colors, "color");
        requireComplete(SpacingToken.values(), spacing, "spacing");
        requireComplete(RadiusToken.values(), radii, "radius");
        requireComplete(AnimationToken.values(), animations, "animation");
    }

    public int color(ColorToken token) {
        return colors.get(Objects.requireNonNull(token, "token"));
    }

    public int spacing(SpacingToken token) {
        return spacing.get(Objects.requireNonNull(token, "token"));
    }

    public int radius(RadiusToken token) {
        return radii.get(Objects.requireNonNull(token, "token"));
    }

    public Duration animation(AnimationToken token) {
        return animations.get(Objects.requireNonNull(token, "token"));
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    private static <K> void requireComplete(K[] keys, Map<K, ?> values, String category) {
        for (K key : keys) if (!values.containsKey(key)) {
            throw new IllegalStateException("Missing UI theme " + category + " token: " + key);
        }
    }

    public static final class Builder {
        private final EnumMap<ColorToken, Integer> colors = new EnumMap<>(ColorToken.class);
        private final EnumMap<SpacingToken, Integer> spacing = new EnumMap<>(SpacingToken.class);
        private final EnumMap<RadiusToken, Integer> radii = new EnumMap<>(RadiusToken.class);
        private final EnumMap<AnimationToken, Duration> animations = new EnumMap<>(AnimationToken.class);

        private Builder() {
        }

        private Builder(UITheme source) {
            colors.putAll(source.colors);
            spacing.putAll(source.spacing);
            radii.putAll(source.radii);
            animations.putAll(source.animations);
        }

        public Builder color(ColorToken token, int argb) {
            colors.put(Objects.requireNonNull(token, "token"), argb);
            return this;
        }

        public Builder spacing(SpacingToken token, int pixels) {
            if (pixels < 0) throw new IllegalArgumentException("Theme spacing cannot be negative");
            spacing.put(Objects.requireNonNull(token, "token"), pixels);
            return this;
        }

        public Builder radius(RadiusToken token, int pixels) {
            if (pixels < 0) throw new IllegalArgumentException("Theme radius cannot be negative");
            radii.put(Objects.requireNonNull(token, "token"), pixels);
            return this;
        }

        public Builder animation(AnimationToken token, Duration duration) {
            Objects.requireNonNull(duration, "duration");
            if (duration.isNegative()) throw new IllegalArgumentException("Theme animation cannot be negative");
            if (duration.compareTo(Duration.ofHours(1)) > 0) {
                throw new IllegalArgumentException("Theme animation cannot exceed one hour");
            }
            animations.put(Objects.requireNonNull(token, "token"), duration);
            return this;
        }

        public UITheme build() {
            return new UITheme(this);
        }
    }
}
