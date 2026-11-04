package io.github.constructchip.hack.builtin;

import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import io.github.constructchip.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * The construct's red lightning. Damage is credited to the host (indirect magic: the construct is the direct source,
 * the host the attacker), so kills, aggro and loot work as if the host did it.
 */
public final class LightningHacks {
    private LightningHacks() {}

    static DamageSource source(HackContext ctx) {
        return ctx.host().damageSources().indirectMagic(ctx.construct(), ctx.host());
    }

    /** One creature, 6 damage. */
    public static final class RedLightning extends SimpleQuickhack {
        public RedLightning() { super(5f, 8 * 20, TargetType.ENTITY, 16); }

        @Override
        public HackResult execute(HackContext ctx) {
            if (!(ctx.entity().orElseThrow() instanceof LivingEntity target)) {
                return HackResult.fail(Component.translatable("message.constructchip.hack.wrong_target"));
            }
            ServerLevel level = ctx.level();
            HackFx.arc(level, ctx.host().getEyePosition().add(0, -0.3, 0), HackFx.chest(target));
            target.hurt(source(ctx), 6f);
            level.playSound(null, target.blockPosition(), ModSounds.BOLT.get(), SoundSource.PLAYERS, 1f, 1f);
            HackFx.glitchFlash(ctx.construct(), 10, 0.4f, 0.5f);
            return HackResult.success(Component.translatable("message.constructchip.red_lightning", target.getDisplayName()));
        }
    }

    /** Every hostile within 12 blocks (nearest 12), 5 damage each. */
    public static final class Storm extends SimpleQuickhack {
        public static final double RADIUS = 12;

        public Storm() { super(8f, 60 * 20, TargetType.NONE, 0); }

        @Override
        public HackResult execute(HackContext ctx) {
            ServerPlayer host = ctx.host();
            ServerLevel level = ctx.level();
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, host.getBoundingBox().inflate(RADIUS),
                            e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(host) <= RADIUS * RADIUS && host.hasLineOfSight(e))
                    .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(host))).limit(12).toList();
            if (targets.isEmpty()) return HackResult.fail(Component.translatable("message.constructchip.storm.none"));
            DamageSource src = source(ctx);
            for (LivingEntity target : targets) {
                HackFx.arc(level, host.getEyePosition().add(0, -0.3, 0), HackFx.chest(target));
                target.hurt(src, 5f);
            }
            level.playSound(null, host.blockPosition(), ModSounds.STORM.get(), SoundSource.PLAYERS, 1f, 1f);
            HackFx.glitchFlash(ctx.construct(), 20, 0.8f, 0.8f);
            return HackResult.success(Component.translatable("message.constructchip.storm", targets.size()));
        }
    }
}
