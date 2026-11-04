package io.github.constructchip.hack.builtin;

import io.github.constructchip.api.CyberwareApi;
import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Short Circuit (10 s) and System Lock (30 s, can't be re-enabled, active buffs cut). Both disable the target's
 * cyberware through {@link CyberwareApi} (other mods' {@code Hackable} items react). On anything without cyberware they still hit
 * the nervous system: slowness, weakness and a jolt, so the hacks are useful in vanilla.
 */
public final class CyberwareHacks {
    private CyberwareHacks() {}

    public static final class ShortCircuit extends SimpleQuickhack {
        public ShortCircuit() { super(4f, 15 * 20, TargetType.ENTITY, 16); }

        @Override
        public HackResult execute(HackContext ctx) {
            if (!(ctx.entity().orElseThrow() instanceof LivingEntity target)) {
                return HackResult.fail(Component.translatable("message.constructchip.hack.wrong_target"));
            }
            int ticks = 200;
            int hacked = CyberwareApi.disable(target, ticks, false);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
            DamageSource jolt = ctx.host().damageSources().indirectMagic(ctx.construct(), ctx.host());
            target.hurt(jolt, 2f);
            if (target instanceof Mob mob) mob.setTarget(null);
            HackFx.sparks(ctx.level(), target);
            HackFx.highlight(List.of(target.getId()), 40, ctx.construct(), ctx.host());
            return HackResult.success(Component.translatable("message.constructchip.short_circuit", target.getDisplayName(), hacked));
        }
    }

    public static final class SystemLock extends SimpleQuickhack {
        public SystemLock() { super(6f, 40 * 20, TargetType.ENTITY, 16); }

        @Override
        public HackResult execute(HackContext ctx) {
            Entity e = ctx.entity().orElseThrow();
            if (!(e instanceof LivingEntity target)) return HackResult.fail(Component.translatable("message.constructchip.hack.wrong_target"));
            int ticks = 600;
            int hacked = CyberwareApi.disable(target, ticks, true);
            List<MobEffectInstance> buffs = target.getActiveEffects().stream().filter(x -> x.getEffect().value().isBeneficial()).toList();
            for (MobEffectInstance buff : buffs) target.removeEffect(buff.getEffect());
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 1));
            if (target instanceof Mob mob) mob.setTarget(null);
            HackFx.sparks(ctx.level(), target);
            HackFx.highlight(List.of(target.getId()), 60, ctx.construct(), ctx.host());
            return HackResult.success(Component.translatable("message.constructchip.system_lock", target.getDisplayName(),
                    hacked, buffs.size()));
        }
    }
}
