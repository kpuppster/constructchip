package io.github.constructchip;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config ({@code serverconfig/constructchip-server.toml}, per world). */
public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue SKILL_POINT_BASE_XP;
    public static final ModConfigSpec.DoubleValue SKILL_POINT_GROWTH;
    public static final ModConfigSpec.IntValue SKILL_POINT_MAX_XP;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("skill_points");
        SKILL_POINT_BASE_XP = b
                .comment("XP points the first skill point costs (not levels; 160 XP is about level 0 to 10).")
                .defineInRange("baseXp", 160, 1, 1_000_000);
        SKILL_POINT_GROWTH = b
                .comment("Each skill point bought on a chip costs this much more than the one before (1.2 = +20%).")
                .defineInRange("growth", 1.2, 1.0, 10.0);
        SKILL_POINT_MAX_XP = b
                .comment("Upper limit for a single point's price, so late points stay reachable.")
                .defineInRange("maxXp", 20_000, 1, Integer.MAX_VALUE);
        b.pop();
        SPEC = b.build();
    }

    private Config() {}
}
