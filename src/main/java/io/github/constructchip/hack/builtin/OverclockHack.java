package io.github.constructchip.hack.builtin;

import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.chip.ChipTimers;
import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Tier 2: for 30 s (+ skills) the host heals (Regeneration II), hits harder (Strength I) and takes a little less
 * damage (Resistance I), and quickhacks cost half. Afterwards every chip ability is locked out for 60 s (± skills).
 * The timers live on the chip; the service blocks everything during the lockout.
 */
public final class OverclockHack extends SimpleQuickhack {
    public OverclockHack() {
        super(4f, 0, TargetType.NONE, 0);
    }

    @Override
    public HackResult execute(HackContext ctx) {
        ChipTimers timers = ChipStacks.timers(ctx.chip());
        if (timers.isOverclocked(ctx.now())) return HackResult.fail(Component.translatable("message.constructchip.overclock.already"));
        int duration = ctx.stats().overclockTicks();
        ChipStacks.setTimers(ctx.chip(), timers.startOverclock(ctx.now(), duration, ctx.stats().lockoutTicks()));
        ServerPlayer host = ctx.host();
        host.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, true, true));
        host.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, true, true));
        host.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        host.level().playSound(null, host.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.4f);
        host.displayClientMessage(Component.translatable("message.constructchip.overclock.host", duration / 20), true);
        return HackResult.success(Component.translatable("message.constructchip.overclock.on", duration / 20,
                ctx.stats().lockoutTicks() / 20));
    }
}
