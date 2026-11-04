package io.github.constructchip.hack.builtin;

import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Regeneration I and Speed I on the host for 6 s. */
public final class AdrenalSurgeHack extends SimpleQuickhack {
    public AdrenalSurgeHack() {
        super(4f, 30 * 20, TargetType.NONE, 0);
    }

    @Override
    public HackResult execute(HackContext ctx) {
        ctx.host().addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0));
        ctx.host().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
        ctx.host().level().playSound(null, ctx.host().blockPosition(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1f, 1.2f);
        return HackResult.success(Component.translatable("message.constructchip.adrenal_surge", ctx.host().getGameProfile().getName()));
    }
}
