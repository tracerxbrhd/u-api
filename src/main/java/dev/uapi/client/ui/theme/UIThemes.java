package dev.uapi.client.ui.theme;

import java.time.Duration;
import static dev.uapi.client.ui.theme.UITheme.AnimationToken.*;
import static dev.uapi.client.ui.theme.UITheme.ColorToken.*;
import static dev.uapi.client.ui.theme.UITheme.SpacingToken.*;

/** Built-in neutral theme intended as a safe fallback, not a mod's visual identity. */
public final class UIThemes {
    public static final UITheme DEFAULT = UITheme.builder()
        .color(BACKGROUND_PRIMARY, 0xE6101117)
        .color(BACKGROUND_SECONDARY, 0xE61A1C25)
        .color(BACKGROUND_PANEL, 0xEE242735)
        .color(BORDER_DEFAULT, 0xFF4B5064)
        .color(BORDER_FOCUSED, 0xFF8AB4FF)
        .color(TEXT_PRIMARY, 0xFFF4F5F8)
        .color(TEXT_SECONDARY, 0xFFCDD1DC)
        .color(TEXT_MUTED, 0xFF8C92A3)
        .color(ACCENT_PRIMARY, 0xFF6699FF)
        .color(ACCENT_SUCCESS, 0xFF5BCB79)
        .color(ACCENT_WARNING, 0xFFF0B34C)
        .color(ACCENT_DANGER, 0xFFE05A67)
        .spacing(SMALL, 4)
        .spacing(MEDIUM, 8)
        .spacing(LARGE, 14)
        .radius(UITheme.RadiusToken.SMALL, 2)
        .radius(UITheme.RadiusToken.MEDIUM, 4)
        .animation(FAST, Duration.ofMillis(90))
        .animation(NORMAL, Duration.ofMillis(180))
        .build();

    /**
     * Shared dark-purple presentation used by the first-party U-API screens.
     *
     * <p>Keeping this palette in U-API prevents each feature mod from approximating the
     * Soul Ascension look with a separate set of hard-coded colors.</p>
     */
    public static final UITheme ARCANE = DEFAULT.toBuilder()
        .color(BACKGROUND_PRIMARY, 0xF0080612)
        .color(BACKGROUND_SECONDARY, 0xF0140F1D)
        .color(BACKGROUND_PANEL, 0xF0231533)
        .color(BORDER_DEFAULT, 0xFF8E4BC4)
        .color(BORDER_FOCUSED, 0xFFD66BFF)
        .color(TEXT_PRIMARY, 0xFFF1E9FF)
        .color(TEXT_SECONDARY, 0xFFBDB2CA)
        .color(TEXT_MUTED, 0xFF9B91AA)
        .color(ACCENT_PRIMARY, 0xFFD66BFF)
        .color(ACCENT_SUCCESS, 0xFFB9FFDB)
        .color(ACCENT_WARNING, 0xFFFFD76A)
        .color(ACCENT_DANGER, 0xFFFF7777)
        .build();

    private UIThemes() {
    }
}
