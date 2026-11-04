package io.github.constructchip;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config ({@code serverconfig/constructchip-server.toml}, per world). */
public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue SKILL_POINT_BASE_XP;
    public static final ModConfigSpec.DoubleValue SKILL_POINT_GROWTH;
    public static final ModConfigSpec.IntValue SKILL_POINT_MAX_XP;

    public static final ModConfigSpec.DoubleValue SEAT_X;
    public static final ModConfigSpec.DoubleValue SEAT_Y;
    public static final ModConfigSpec.DoubleValue SEAT_Z;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("soul_killer");
        b.comment("Where the sitter sits, in blocks from the Soul Killer block's center, for a chair facing north",
                "(rotated with the chair). Nudge these if the player doesn't line up with the cushion.");
        SEAT_X = b.defineInRange("seatX", 0.25, -3.0, 3.0);
        SEAT_Y = b.defineInRange("seatY", 0.45, -1.0, 3.0);
        SEAT_Z = b.defineInRange("seatZ", 0.44, -3.0, 3.0);
        b.pop();
        b.push("skill_points");
        SKILL_POINT_BASE_XP = b
                .comment("XP points the first skill point costs (not levels; 30 XP is about level 0 to 3).")
                .defineInRange("baseXp", 30, 1, 1_000_000);
        SKILL_POINT_GROWTH = b
                .comment("Each skill point bought on a chip costs this much more than the one before (1.05 = +5%).")
                .defineInRange("growth", 1.05, 1.0, 10.0);
        SKILL_POINT_MAX_XP = b
                .comment("Upper limit for a single point's price, so late points stay reachable.")
                .defineInRange("maxXp", 500, 1, Integer.MAX_VALUE);
        b.pop();
        SPEC = b.build();
    }

    private Config() {}
}
