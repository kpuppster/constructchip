package io.github.constructchip.hack;

import io.github.constructchip.skill.ChipStats;

/**
 * A quickhack: the only way a construct affects the world. Implementations hold no per-use state; everything they need
 * comes in through {@link HackContext}. {@link QuickhackService} has already checked unlock, lockout, cooldown, RAM,
 * target type, range and line of sight before {@link #execute} runs. RAM and cooldown are only charged if it
 * returns a success.
 */
public interface Quickhack {
    /** RAM before skills and Overclock. */
    float baseRamCost();

    int cooldownTicks();

    TargetType targetType();

    /** Max distance from the host's eyes to the target, in blocks. Ignored for {@link TargetType#NONE}. */
    double baseRange();

    /** Range after skills; override for hacks that scale (Scanner + Deep Scan). */
    default double range(ChipStats stats) {
        return baseRange();
    }

    default boolean requiresLineOfSight() {
        return true;
    }

    HackResult execute(HackContext context);

    enum TargetType {
        /** Acts on the host or the construct, no target needed. */
        NONE,
        ENTITY,
        BLOCK,
        ENTITY_OR_BLOCK;

        public boolean accepts(HackTarget.Kind kind) {
            return switch (this) {
                case NONE -> true; // a target is simply ignored
                case ENTITY -> kind == HackTarget.Kind.ENTITY;
                case BLOCK -> kind == HackTarget.Kind.BLOCK;
                case ENTITY_OR_BLOCK -> kind != HackTarget.Kind.NONE;
            };
        }
    }
}
