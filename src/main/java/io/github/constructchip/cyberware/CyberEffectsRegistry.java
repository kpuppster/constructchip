package io.github.constructchip.cyberware;

import io.github.constructchip.ConstructChip;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Status effects (icons by A24-N22). */
public final class CyberEffectsRegistry {
    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(Registries.MOB_EFFECT, ConstructChip.MODID);

    /** 1 damage per second. Scar Coalescer makes you immune. */
    public static final DeferredHolder<MobEffect, MobEffect> BLEED = REGISTER.register("bleed", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x9B0A12) {
        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 20 == 0; }
        @Override public boolean applyEffectTick(LivingEntity e, int amplifier) {
            e.hurt(e.damageSources().magic(), 1f + amplifier);
            return true;
        }
    });
    /** Frozen Gorilla Arms: heavy slow. */
    public static final DeferredHolder<MobEffect, MobEffect> CRYOLOCK = REGISTER.register("cryolock", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x9FE6FF) {}
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, ConstructChip.id("cryolock"), -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.ATTACK_SPEED, ConstructChip.id("cryolock"), -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    /** Electric Gorilla Arms: twitchy, weak hits. */
    public static final DeferredHolder<MobEffect, MobEffect> ELECTROSHOCK = REGISTER.register("electroshock", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xF3E600) {
        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 10 == 0; }
        @Override public boolean applyEffectTick(LivingEntity e, int amplifier) {
            e.hurt(e.damageSources().lightningBolt(), 0.5f);
            return true;
        }
    }.addAttributeModifier(Attributes.ATTACK_DAMAGE, ConstructChip.id("electroshock"), -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    /** Shown while you have cyberpsychosis (phase I+). Cosmetic; Psychosis drives the symptoms. */
    public static final DeferredHolder<MobEffect, MobEffect> CYBERPSYCHOSIS = REGISTER.register("cyberpsychosis", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xF90759) {});
    /** Shown while immunosuppressants are active. */
    public static final DeferredHolder<MobEffect, MobEffect> IMMUNOSUPPRESSION = REGISTER.register("immunosuppression", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x5EF6FF) {});

    private CyberEffectsRegistry() {}
}
