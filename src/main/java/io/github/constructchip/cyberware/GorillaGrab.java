package io.github.constructchip.cyberware;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.ghost.GhostState;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Gorilla Arms grab & throw: with the arms deployed and an empty hand, right-click a creature to lift it, then
 * right-click (anywhere) to throw it. It takes damage when it hits something. Targets below 30% health are finished
 * off by the throw. Bosses and anything wider than 1.6 blocks can't be lifted.
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class GorillaGrab {
    private record Held(int entityId, long since) {}
    private record Thrown(UUID thrower, float damage, long until) {}

    private static final Map<UUID, Held> HELD = new HashMap<>();
    private static final Map<Integer, Thrown> THROWN = new HashMap<>();

    private GorillaGrab() {}

    public static boolean isHolding(ServerPlayer p) {
        return HELD.containsKey(p.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.getMainHandItem().isEmpty() || Cyberware.suppressed(p)) return;
        CyberState s = Cyberware.state(p);
        if (!s.armsDeployed || !s.has(Implant.GORILLA_ARMS)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (isHolding(p)) {
            throwHeld(p);
            return;
        }
        Entity t = event.getTarget();
        if (!(t instanceof LivingEntity target) || !target.isAlive() || target instanceof EnderDragon || t instanceof EnderDragonPart
                || target instanceof WitherBoss || target.getBbWidth() > 1.6f || GhostState.isConstruct(target)) return;
        HELD.put(p.getUUID(), new Held(target.getId(), p.level().getGameTime()));
        CyberEffects.play(p, "er_extend_shrink_gorilla_arms", 0.6f);
    }

    /** Called for the Slam key (right-click with an empty hand): if holding something, throw it instead. */
    public static boolean tryThrow(ServerPlayer p) {
        if (!isHolding(p)) return false;
        throwHeld(p);
        return true;
    }

    private static void throwHeld(ServerPlayer p) {
        Held held = HELD.remove(p.getUUID());
        if (held == null || !(p.level().getEntity(held.entityId()) instanceof LivingEntity target)) return;
        CyberState s = Cyberware.state(p);
        Vec3 look = p.getLookAngle();
        target.setDeltaMovement(look.x * 1.8, 0.35 + look.y * 1.2, look.z * 1.8);
        target.hurtMarked = true;
        float damage = (6 + s.fury / 10f) * (s.has(Implant.DENSE_MARROW) ? 1.3f : 1f) * (1f + 0.085f * Math.max(0, s.level(Implant.GORILLA_ARMS)));
        if (target.getHealth() / target.getMaxHealth() < 0.3f) damage *= 3; // finisher
        THROWN.put(target.getId(), new Thrown(p.getUUID(), damage, p.level().getGameTime() + 60));
        s.fury = Math.min(100, s.fury + 15);
        CyberEffects.play(p, "er_gorilla_arms_throw", 1f);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (HELD.isEmpty() && THROWN.isEmpty()) return;
        Iterator<Map.Entry<UUID, Held>> it = HELD.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Held> e = it.next();
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(e.getKey());
            Entity target = p == null ? null : p.level().getEntity(e.getValue().entityId());
            CyberState s = p == null ? null : Cyberware.state(p);
            if (p == null || target == null || !target.isAlive() || s == null || !s.armsDeployed
                    || p.level().getGameTime() - e.getValue().since() > 100 || target.distanceToSqr(p) > 36) {
                it.remove();
                continue;
            }
            Vec3 at = p.getEyePosition().add(p.getLookAngle().scale(1.6)).subtract(0, target.getBbHeight() * 0.5, 0);
            target.teleportTo(at.x, at.y, at.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.resetFallDistance();
        }
        Iterator<Map.Entry<Integer, Thrown>> th = THROWN.entrySet().iterator();
        while (th.hasNext()) {
            Map.Entry<Integer, Thrown> e = th.next();
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(e.getValue().thrower());
            Entity target = p == null ? null : p.level().getEntity(e.getKey());
            if (p == null || target == null || !target.isAlive() || p.level().getGameTime() > e.getValue().until()) {
                th.remove();
                continue;
            }
            boolean hit = target.horizontalCollision || target.verticalCollision && target.getDeltaMovement().y <= 0 && target.tickCount > 0;
            if (hit && target instanceof LivingEntity living) {
                living.hurt(p.damageSources().playerAttack(p), e.getValue().damage());
                ((ServerLevel) p.level()).sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5, target.getZ(), 2, 0.2, 0.2, 0.2, 0);
                CyberEffects.play(p, "er_ground_slam_weak", 0.7f);
                th.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        HELD.clear();
        THROWN.clear();
    }
}
