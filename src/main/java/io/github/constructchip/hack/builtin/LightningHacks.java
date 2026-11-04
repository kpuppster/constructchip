package io.github.constructchip.hack.builtin;

import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import io.github.constructchip.fx.BoltFx;
import java.util.ArrayList;
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
import net.minecraft.world.phys.Vec3;

/**
 * The construct's red lightning: Fry (one target, id {@code red_lightning}) and High Voltage (every hostile around,
 * id {@code lightning_storm}). Both draw Capri's bolts through {@link BoltFx}. Damage is credited to the host (indirect magic: the construct is the direct source,
 * the host the attacker), so kills, aggro and loot work as if the host did it.
 */
public final class LightningHacks {
    private LightningHacks() {}

    static DamageSource source(HackContext ctx) {
        return ctx.host().damageSources().indirectMagic(ctx.construct(), ctx.host());
    }

    /** Fry: one creature, 6 damage. */
    public static final class RedLightning extends SimpleQuickhack {
        public RedLightning() { super(5f, 8 * 20, TargetType.ENTITY, 16); }

        @Override public boolean drawsOwnFx() { return true; }

        @Override
        public HackResult execute(HackContext ctx) {
            if (!(ctx.entity().orElseThrow() instanceof LivingEntity target)) {
                return HackResult.fail(Component.translatable("message.constructchip.hack.wrong_target"));
            }
            ServerLevel level = ctx.level();
            BoltFx.send(level, BoltFx.Preset.FRY, ctx.host(), BoltFx.hand(ctx.host()), List.of(BoltFx.chest(target), target.position()));
            target.hurt(source(ctx), 6f);
            level.playSound(null, target.blockPosition(), ModSounds.BOLT.get(), SoundSource.PLAYERS, 1f, 1f);
            BoltFx.crackle(level, BoltFx.chest(target), 1f);
            HackFx.glitchFlash(ctx.construct(), 10, 0.4f, 0.5f);
            return HackResult.success(Component.translatable("message.constructchip.red_lightning", target.getDisplayName()));
        }
    }

    /**
     * High Voltage: every creature within 12 blocks of the host that the host or the construct can see (nearest 12,
     * monsters and anything attacking the host first), 5 damage each. Spares players, constructs, the host's own pets,
     * villagers, golems and allays.
     */
    public static final class Storm extends SimpleQuickhack {
        public static final double RADIUS = 12;

        public Storm() { super(8f, 60 * 20, TargetType.NONE, 0); }

        @Override public boolean drawsOwnFx() { return true; }

        @Override
        public HackResult execute(HackContext ctx) {
            ServerPlayer host = ctx.host();
            ServerLevel level = ctx.level();
            ServerPlayer construct = ctx.construct();
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, host.getBoundingBox().inflate(RADIUS),
                            e -> e.isAlive() && e != host && e != construct && !spared(e, host)
                                    && e.distanceToSqr(host) <= RADIUS * RADIUS
                                    && (host.hasLineOfSight(e) || construct.hasLineOfSight(e)))
                    .stream()
                    .sorted(Comparator.<LivingEntity>comparingInt(e -> hostile(e, host) ? 0 : 1)
                            .thenComparingDouble(e -> e.distanceToSqr(host)))
                    .limit(12).toList();
            if (targets.isEmpty()) return HackResult.fail(Component.translatable("message.constructchip.storm.none"));
            DamageSource src = source(ctx);
            List<Vec3> points = new ArrayList<>();
            for (LivingEntity target : targets) {
                points.add(BoltFx.chest(target)); // chest and feet pairs: the sky bolt lands on the chest, the burst on the ground
                points.add(target.position());
                target.hurt(src, 5f);
                BoltFx.crackle(level, BoltFx.chest(target), 0.7f);
            }
            BoltFx.send(level, BoltFx.Preset.HIGH_VOLTAGE, host, BoltFx.hand(host), points);
            level.playSound(null, host.blockPosition(), ModSounds.STORM.get(), SoundSource.PLAYERS, 1f, 1f);
            HackFx.glitchFlash(ctx.construct(), 20, 0.8f, 0.8f);
            return HackResult.success(Component.translatable("message.constructchip.storm", targets.size()));
        }
    }

    /** Monsters, and any mob currently going after the host. */
    static boolean hostile(LivingEntity e, ServerPlayer host) {
        return e instanceof Enemy || (e instanceof net.minecraft.world.entity.Mob m && m.getTarget() == host);
    }

    /** Never hit by High Voltage: players, constructs, the host's pets, villagers, golems, allays, armor stands. */
    static boolean spared(LivingEntity e, ServerPlayer host) {
        if (e instanceof net.minecraft.world.entity.player.Player || io.github.constructchip.ghost.GhostState.isConstruct(e)) return true;
        if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) return true;
        if (e instanceof net.minecraft.world.entity.OwnableEntity o && host.getUUID().equals(o.getOwnerUUID())) return true;
        if (hostile(e, host)) return false;
        return e instanceof net.minecraft.world.entity.npc.AbstractVillager || e instanceof net.minecraft.world.entity.animal.AbstractGolem
                || e instanceof net.minecraft.world.entity.animal.allay.Allay;
    }
}
