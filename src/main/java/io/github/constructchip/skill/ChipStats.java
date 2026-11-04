package io.github.constructchip.skill;

import io.github.constructchip.chip.ChipRam;
import io.github.constructchip.chip.ChipSkills;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.chip.ChipTimers;
import net.minecraft.core.Registry;
import net.minecraft.world.item.ItemStack;

/**
 * A chip's effective numbers after skill modifiers. Worked out on demand from the unlocked skills; nothing here is stored.
 *
 * @param maxRam                  base max RAM + MAX_RAM modifiers
 * @param ramRegenPerSecond       base regen + RAM_REGEN modifiers
 * @param ramCostMultiplier       1 + RAM_COST modifiers (at least 0.1); Overclock halves it again
 * @param overclockTicks          Overclock duration
 * @param lockoutTicks            lockout after Overclock (at least 0)
 * @param scanRangeBonus          extra Scanner range in blocks
 * @param hostDamageTaken         multiplier on damage the host takes while the chip is installed
 * @param noCooldownsWhileOverclocked Neural Overdrive
 */
public record ChipStats(int maxRam, float ramRegenPerSecond, float ramCostMultiplier, int overclockTicks,
                        int lockoutTicks, float scanRangeBonus, float hostDamageTaken,
                        boolean noCooldownsWhileOverclocked) {

    public static ChipStats of(ChipSkills skills, Registry<Skill> registry) {
        float maxRam = ChipStacks.BASE_MAX_RAM + SkillTree.total(skills, registry, SkillModifier.Type.MAX_RAM);
        float regen = ChipStacks.BASE_REGEN_PER_SECOND + SkillTree.total(skills, registry, SkillModifier.Type.RAM_REGEN);
        float cost = 1f + SkillTree.total(skills, registry, SkillModifier.Type.RAM_COST);
        float overclock = ChipTimers.OVERCLOCK_TICKS + SkillTree.total(skills, registry, SkillModifier.Type.OVERCLOCK_TICKS);
        float lockout = ChipTimers.LOCKOUT_TICKS + SkillTree.total(skills, registry, SkillModifier.Type.LOCKOUT_TICKS);
        float damageTaken = 1f + SkillTree.total(skills, registry, SkillModifier.Type.HOST_DAMAGE_TAKEN);
        return new ChipStats(
                Math.max(1, Math.round(maxRam)),
                Math.max(0f, regen),
                Math.max(0.1f, cost),
                Math.max(20, Math.round(overclock)),
                Math.max(0, Math.round(lockout)),
                Math.max(0f, SkillTree.total(skills, registry, SkillModifier.Type.SCAN_RANGE)),
                Math.max(0.1f, damageTaken),
                SkillTree.total(skills, registry, SkillModifier.Type.NO_COOLDOWNS_WHILE_OVERCLOCKED) > 0f);
    }

    public static ChipStats of(ItemStack chip, Registry<Skill> registry) {
        return of(ChipStacks.skills(chip), registry);
    }

    /** RAM cost of a quickhack after skills and Overclock. */
    public float ramCost(float baseCost, ChipTimers timers, long now) {
        float cost = baseCost * ramCostMultiplier;
        return timers.isOverclocked(now) ? cost * 0.5f : cost;
    }

    /**
     * Makes the stored RAM pool match these stats (max and regen change when skills are unlocked, or vanish when a
     * compat mod is removed). Re-stamps at {@code now} so the change never applies to past ticks. Returns true if changed.
     */
    public boolean applyRam(ItemStack chip, long now) {
        ChipRam ram = ChipStacks.ram(chip);
        if (ram.max() == maxRam && ram.regenPerSecond() == ramRegenPerSecond) return false;
        ChipStacks.setRam(chip, ram.withStats(maxRam, ramRegenPerSecond, now));
        return true;
    }
}
