package dev.uapi.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import dev.uapi.difficulty.DifficultyRank;

public final class UApiServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_ACTIVE_INSTANCES;
    public static final ModConfigSpec.IntValue MAX_PLAYERS_PER_INSTANCE;
    public static final ModConfigSpec.IntValue TIMER_CHECK_INTERVAL_TICKS;
    public static final ModConfigSpec.BooleanValue RECOVER_PLAYERS_AFTER_RESTART;
    public static final ModConfigSpec.BooleanValue LOG_SKIPPED_REWARDS;
    public static final ModConfigSpec.ConfigValue<String> DIFFICULTY_E;
    public static final ModConfigSpec.ConfigValue<String> DIFFICULTY_D;
    public static final ModConfigSpec.ConfigValue<String> DIFFICULTY_C;
    public static final ModConfigSpec.ConfigValue<String> DIFFICULTY_B;
    public static final ModConfigSpec.ConfigValue<String> DIFFICULTY_A;
    public static final ModConfigSpec.ConfigValue<String> DIFFICULTY_S;
    public static final ModConfigSpec.ConfigValue<String> DIFFICULTY_ANOMALY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Core limits shared by all instance-providing modules.").push("general");
        MAX_ACTIVE_INSTANCES = builder.comment(
            "Maximum simultaneously active instances on this server.",
            "Valid range: 0..100000. Default: 0 (unlimited).")
            .defineInRange("maxActive", 0, 0, 100000);
        MAX_PLAYERS_PER_INSTANCE = builder.comment(
            "Maximum participants that may be assigned to one instance.",
            "Valid range: 1..64. Default: 4.")
            .defineInRange("maxPlayers", 4, 1, 64);
        RECOVER_PLAYERS_AFTER_RESTART = builder.comment(
            "Return players from unfinished instances after a server restart.",
            "Default: true. Disable only if another module handles recovery.")
            .define("recoverPlayersAfterRestart", true);
        builder.pop();

        builder.comment("Performance-related lifecycle scheduling.").push("performance");
        TIMER_CHECK_INTERVAL_TICKS = builder.comment(
            "Ticks between instance lifecycle checks; 20 ticks is approximately one second.",
            "Valid range: 1..1200. Default: 20. Higher values reduce checks but make timers less precise.")
            .defineInRange("timerCheckIntervalTicks", 20, 1, 1200);
        builder.pop();

        builder.comment(
            "Difficulty profiles shared by dungeon modules.",
            "Format: health|damage|armor|reward|entrySeconds|runSeconds|rewardSeconds|minLevel.",
            "Multipliers must be non-negative; time values are seconds.")
            .push("difficulty");
        DIFFICULTY_E = builder.define("E", "1.0|1.0|1.0|1.0|120|1200|180|0");
        DIFFICULTY_D = builder.define("D", "1.25|1.15|1.10|1.15|100|1080|160|5");
        DIFFICULTY_C = builder.define("C", "1.60|1.35|1.25|1.35|90|900|140|10");
        DIFFICULTY_B = builder.define("B", "2.10|1.65|1.45|1.60|75|720|120|20");
        DIFFICULTY_A = builder.define("A", "2.80|2.00|1.70|1.95|60|600|100|35");
        DIFFICULTY_S = builder.define("S", "4.00|2.50|2.00|2.50|45|480|90|50");
        DIFFICULTY_ANOMALY = builder.define("ANOMALY", "6.00|3.25|2.50|3.50|35|420|75|75");
        builder.pop();
        builder.comment("Diagnostic logging. These options do not change gameplay.").push("debug");
        LOG_SKIPPED_REWARDS = builder.comment(
            "Log optional rewards skipped because their provider mod is unavailable.",
            "Default: true.")
            .define("logSkippedOptionalRewards", true);
        builder.pop();
        SPEC = builder.build();
    }

    private UApiServerConfig() {}

    public static double difficultyDouble(DifficultyRank rank, int index, double fallback) {
        try {
            String[] values = profile(rank).split("\\|");
            return values.length > index ? Double.parseDouble(values[index].trim()) : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public static int difficultyInt(DifficultyRank rank, int index, int fallback) {
        return Math.max(0, (int) Math.round(difficultyDouble(rank, index, fallback)));
    }

    private static String profile(DifficultyRank rank) {
        return switch (rank) {
            case E -> DIFFICULTY_E.get();
            case D -> DIFFICULTY_D.get();
            case C -> DIFFICULTY_C.get();
            case B -> DIFFICULTY_B.get();
            case A -> DIFFICULTY_A.get();
            case S -> DIFFICULTY_S.get();
            case ANOMALY -> DIFFICULTY_ANOMALY.get();
        };
    }
}
