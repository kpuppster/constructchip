package io.github.constructchip.skill;

import io.github.constructchip.chip.ChipSkills;
import java.util.Locale;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

/** Unlock rules and passive stat totals. Pure functions over {@link ChipSkills}; callers write the result back. */
public final class SkillTree {
    private SkillTree() {}

    public enum UnlockResult {
        OK, UNKNOWN_SKILL, ALREADY_UNLOCKED, TIER_TOO_LOW, MISSING_PREREQUISITE, NOT_ENOUGH_POINTS;

        public String translationKey() {
            return "message.constructchip.skill." + name().toLowerCase(Locale.ROOT);
        }
    }

    public static UnlockResult check(ChipSkills skills, ResourceLocation id, Registry<Skill> registry) {
        Skill skill = registry.get(id);
        if (skill == null) return UnlockResult.UNKNOWN_SKILL;
        if (skills.has(id)) return UnlockResult.ALREADY_UNLOCKED;
        if (skill.tier() > skills.tier()) return UnlockResult.TIER_TOO_LOW;
        for (ResourceLocation parent : skill.requires()) {
            if (!skills.has(parent) || !registry.containsKey(parent)) return UnlockResult.MISSING_PREREQUISITE;
        }
        if (skills.points() < skill.cost()) return UnlockResult.NOT_ENOUGH_POINTS;
        return UnlockResult.OK;
    }

    /** Returns the skills after unlocking {@code id}, or the same instance if {@link #check} would fail. */
    public static ChipSkills unlock(ChipSkills skills, ResourceLocation id, Registry<Skill> registry) {
        if (check(skills, id, registry) != UnlockResult.OK) return skills;
        Skill skill = registry.get(id);
        return skills.unlock(id).withPoints(skills.points() - skill.cost());
    }

    /** Sum of one modifier type over all unlocked skills that still exist (e.g. Vista skills vanish without Vista). */
    public static float total(ChipSkills skills, Registry<Skill> registry, SkillModifier.Type type) {
        float sum = 0f;
        for (ResourceLocation id : skills.unlocked()) {
            Skill skill = registry.get(id);
            if (skill == null) continue;
            for (SkillModifier m : skill.modifiers()) {
                if (m.type() == type) sum += m.amount();
            }
        }
        return sum;
    }

    /** True if the quickhack is granted by any unlocked, existing skill. */
    public static boolean grantsQuickhack(ChipSkills skills, Registry<Skill> registry, ResourceLocation hack) {
        for (ResourceLocation id : skills.unlocked()) {
            Skill skill = registry.get(id);
            if (skill != null && skill.quickhack().map(hack::equals).orElse(false)) return true;
        }
        return false;
    }
}
