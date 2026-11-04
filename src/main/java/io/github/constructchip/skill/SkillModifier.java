package io.github.constructchip.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** A passive stat change granted by an unlocked skill. Amounts of the same type add up. */
public record SkillModifier(Type type, float amount) {
    public static final Codec<SkillModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
            Type.CODEC.fieldOf("type").forGetter(SkillModifier::type),
            Codec.FLOAT.fieldOf("amount").forGetter(SkillModifier::amount)
    ).apply(i, SkillModifier::new));

    public enum Type implements StringRepresentable {
        /** + max RAM. */
        MAX_RAM,
        /** + RAM per second. */
        RAM_REGEN,
        /** Added to the RAM cost multiplier (-0.1 = 10% cheaper). Stacks with Overclock's halving. */
        RAM_COST,
        /** + Overclock duration in ticks. */
        OVERCLOCK_TICKS,
        /** + post-Overclock lockout in ticks (negative shortens it). */
        LOCKOUT_TICKS,
        /** + Scanner range in blocks. */
        SCAN_RANGE,
        /** Added to the host's damage-taken multiplier (-0.1 = 10% less) while the chip is installed. */
        HOST_DAMAGE_TAKEN,
        /** Any amount > 0: quickhacks ignore cooldowns while Overclocked (Neural Overdrive). */
        NO_COOLDOWNS_WHILE_OVERCLOCKED;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
