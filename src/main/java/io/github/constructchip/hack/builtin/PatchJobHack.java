package io.github.constructchip.hack.builtin;

import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Field repair on the host: +10 health, 4 extra hearts (Absorption II) for 30 s, poison, wither and fire cleared. */
public final class PatchJobHack extends SimpleQuickhack {
    public PatchJobHack() {
        super(6f, 60 * 20, TargetType.NONE, 0);
    }

    @Override
    public HackResult execute(HackContext ctx) {
        ServerPlayer host = ctx.host();
        host.heal(10f);
        host.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
        host.removeEffect(MobEffects.POISON);
        host.removeEffect(MobEffects.WITHER);
        host.clearFire();
        ctx.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, host.getX(), host.getY() + 1, host.getZ(), 12, 0.4, 0.6, 0.4, 0);
        host.level().playSound(null, host.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 2f);
        return HackResult.success(Component.translatable("message.constructchip.patch_job", host.getGameProfile().getName()));
    }
}
