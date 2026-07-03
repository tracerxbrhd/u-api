package dev.uapi.difficulty;

import net.minecraft.ChatFormatting;
import dev.uapi.config.UApiServerConfig;
import net.minecraft.network.chat.Component;

public enum DifficultyRank {
    E(1.00, 1.00, 1.00, 1.00, 120, 20 * 60, 180, 0, ChatFormatting.GREEN),
    D(1.25, 1.15, 1.10, 1.15, 100, 18 * 60, 160, 5, ChatFormatting.DARK_GREEN),
    C(1.60, 1.35, 1.25, 1.35, 90, 15 * 60, 140, 10, ChatFormatting.BLUE),
    B(2.10, 1.65, 1.45, 1.60, 75, 12 * 60, 120, 20, ChatFormatting.DARK_PURPLE),
    A(2.80, 2.00, 1.70, 1.95, 60, 10 * 60, 100, 35, ChatFormatting.GOLD),
    S(4.00, 2.50, 2.00, 2.50, 45, 8 * 60, 90, 50, ChatFormatting.RED),
    ANOMALY(6.00, 3.25, 2.50, 3.50, 35, 7 * 60, 75, 75, ChatFormatting.DARK_PURPLE);

    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double armorMultiplier;
    private final double rewardMultiplier;
    private final int entrySeconds;
    private final int runSeconds;
    private final int rewardSeconds;
    private final int minimumLevel;
    private final ChatFormatting color;

    DifficultyRank(double healthMultiplier, double damageMultiplier, double armorMultiplier,
                   double rewardMultiplier, int entrySeconds, int runSeconds, int rewardSeconds,
                   int minimumLevel, ChatFormatting color) {
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.armorMultiplier = armorMultiplier;
        this.rewardMultiplier = rewardMultiplier;
        this.entrySeconds = entrySeconds;
        this.runSeconds = runSeconds;
        this.rewardSeconds = rewardSeconds;
        this.minimumLevel = minimumLevel;
        this.color = color;
    }

    public double healthMultiplier() { return UApiServerConfig.difficultyDouble(this, 0, healthMultiplier); }
    public double damageMultiplier() { return UApiServerConfig.difficultyDouble(this, 1, damageMultiplier); }
    public double armorMultiplier() { return UApiServerConfig.difficultyDouble(this, 2, armorMultiplier); }
    public double rewardMultiplier() { return UApiServerConfig.difficultyDouble(this, 3, rewardMultiplier); }
    public int entrySeconds() { return UApiServerConfig.difficultyInt(this, 4, entrySeconds); }
    public int runSeconds() { return UApiServerConfig.difficultyInt(this, 5, runSeconds); }
    public int rewardSeconds() { return UApiServerConfig.difficultyInt(this, 6, rewardSeconds); }
    public int minimumLevel() { return UApiServerConfig.difficultyInt(this, 7, minimumLevel); }
    public ChatFormatting color() { return color; }
    public Component displayName() {
        Component readable = Component.translatable("difficulty.u_api." + name().toLowerCase());
        return this == ANOMALY ? readable.copy().withStyle(style -> style.withColor(color).withObfuscated(true))
            : readable.copy().withStyle(color);
    }

    public static DifficultyRank byName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return E;
        }
    }
}
