package io.github.constructchip.skill;

import io.github.constructchip.Config;
import net.minecraft.world.entity.player.Player;

/**
 * Skill points are bought with experience. The price is in XP points (not levels), so it is the same whatever
 * level the buyer is at. It rises with each point the chip has ever bought:
 * {@code cost(n) = min(maxXp, round(baseXp * growth^n))}, where n is {@code ChipSkills.purchased}.
 *
 * <p>Defaults (30, x1.05, cap 500): 1st point 30 XP, 5th 36, 10th 47, 15th 59, 20th 76. Ten points total
 * about 380 XP (~level 16 from zero), twenty about 990 XP (~level 25), and the whole tree about 7800 XP. The counter lives on the chip, so selling or
 * handing over a chip doesn't reset the price.
 */
public final class SkillPointCost {
    private SkillPointCost() {}

    /** XP for the point after {@code alreadyPurchased} points. */
    public static int costOf(int alreadyPurchased) {
        double raw = Config.SKILL_POINT_BASE_XP.get() * Math.pow(Config.SKILL_POINT_GROWTH.get(), alreadyPurchased);
        return (int) Math.min(Config.SKILL_POINT_MAX_XP.get(), Math.round(raw));
    }

    /** XP for buying {@code count} points in a row. */
    public static long costOf(int alreadyPurchased, int count) {
        long total = 0;
        for (int i = 0; i < count; i++) total += costOf(alreadyPurchased + i);
        return total;
    }

    /**
     * The player's XP as points, worked out from level + progress. Vanilla's totalExperience field drifts, because
     * enchanting and anvils take levels without updating it.
     */
    public static long totalXp(Player player) {
        int level = player.experienceLevel;
        return xpForLevel(level) + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    /** Total XP needed to go from level 0 to {@code level} (the vanilla curve). */
    public static long xpForLevel(int level) {
        long l = level;
        if (l <= 16) return l * l + 6 * l;
        if (l <= 31) return Math.round(2.5 * l * l - 40.5 * l + 360);
        return Math.round(4.5 * l * l - 162.5 * l + 2220);
    }

    /** Creative players buy for free. */
    public static boolean canAfford(Player player, long cost) {
        return player.getAbilities().instabuild || totalXp(player) >= cost;
    }

    /** Takes {@code cost} XP; vanilla's giveExperiencePoints handles negative amounts and level-downs. */
    public static void charge(Player player, long cost) {
        if (player.getAbilities().instabuild) return;
        long left = cost;
        while (left > 0) {
            int step = (int) Math.min(Integer.MAX_VALUE, left);
            player.giveExperiencePoints(-step);
            left -= step;
        }
    }
}
