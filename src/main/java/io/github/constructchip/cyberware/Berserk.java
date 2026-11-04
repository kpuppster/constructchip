package io.github.constructchip.cyberware;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.ghost.GhostState;
import io.github.constructchip.network.BerserkPayload;
import io.github.constructchip.network.Net;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Phase V berserk episodes (Project: Edgerunner's "the chrome takes over"). During the terminal window, every so
 * often the chrome grabs control while a target is in reach. The client forces third person, locks input, aims,
 * chases and swings at full charge. Meanwhile you're invulnerable and projectiles bounce off you.
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class Berserk {
    public static final int EPISODE_TICKS = 200, GAP_TICKS = 500, RANGE = 16;
    private static final Map<UUID, Long> ACTIVE_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> NEXT_EPISODE = new HashMap<>();

    private Berserk() {}

    public static boolean isBerserk(Player p) {
        Long until = ACTIVE_UNTIL.get(p.getUUID());
        return until != null && until > p.level().getGameTime();
    }

    /** Called once a second from Psychosis while the terminal window runs. */
    static void tick(ServerPlayer p, CyberState s, long now) {
        if (s.phase < 5 || s.terminalUntil <= 0) {
            end(p);
            return;
        }
        if (isBerserk(p)) return;
        long next = NEXT_EPISODE.getOrDefault(p.getUUID(), now + 100);
        NEXT_EPISODE.putIfAbsent(p.getUUID(), next);
        if (now < next) return;
        LivingEntity target = p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(RANGE),
                        e -> e != p && e.isAlive() && !GhostState.isConstruct(e) && p.hasLineOfSight(e))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
        if (target == null) return; // only runs while a valid target exists
        ACTIVE_UNTIL.put(p.getUUID(), now + EPISODE_TICKS);
        NEXT_EPISODE.put(p.getUUID(), now + EPISODE_TICKS + GAP_TICKS);
        Net.send(p, new BerserkPayload(target.getId(), EPISODE_TICKS));
        Cyberware.sendGlitch(p, 30, 1f, 0.8f);
        CyberEffects.play(p, "er_cyberpsychosis_stinger_episode", 1f);
    }

    static void end(ServerPlayer p) {
        if (ACTIVE_UNTIL.remove(p.getUUID()) != null) Net.send(p, new BerserkPayload(-1, 0));
        NEXT_EPISODE.remove(p.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide && isBerserk(p)
                && !event.getSource().is(Psychosis.DAMAGE)) event.setCanceled(true);
    }

    /** Projectiles bounce off a berserk player. */
    @SubscribeEvent
    public static void onProjectile(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof Player p
                && !p.level().isClientSide && isBerserk(p)) {
            Projectile proj = event.getProjectile();
            event.setCanceled(true);
            proj.setDeltaMovement(proj.getDeltaMovement().scale(-0.6));
            proj.hurtMarked = true;
        }
    }
}
