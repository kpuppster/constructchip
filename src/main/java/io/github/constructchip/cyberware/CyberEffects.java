package io.github.constructchip.cyberware;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.api.CyberwareApi;
import io.github.constructchip.network.HighlightPayload;
import io.github.constructchip.network.Net;
import io.github.constructchip.registry.ModSounds;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

/**
 * What implants do. Actives: Sandevistan (OS), optic scan (Face), Gorilla Arms deploy + Slam (Arms), Blood Pump
 * (Circulatory). Passives: Axolotl, legs, and the skeleton frames, with resistances multiplied toward Project:
 * Edgerunner's caps (70% physical, 55% everything else). Our own implementation of A24-N22's design.
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class CyberEffects {
    public static final float CAP_PHYSICAL = 0.70f, CAP_OTHER = 0.55f;
    private static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(1f, 0.15f, 0.2f), 0.8f);
    private static final DustParticleOptions HAZARD = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.1f), 1.2f);

    private CyberEffects() {}

    static SoundEvent sound(String name) {
        return BuiltInRegistries.SOUND_EVENT.get(ConstructChip.id(name));
    }

    static void play(ServerPlayer p, String name, float volume) {
        SoundEvent e = sound(name);
        if (e != null) p.level().playSound(null, p.blockPosition(), e, SoundSource.PLAYERS, volume, 1f);
    }

    // ---------------------------------------------------------------- activation

    public enum Action { OS, ARMS, FACE, CIRCULATORY, SLAM }

    /** A key press from the player (or a construct's Override on its host). Returns a reason on failure, else null. */
    public static Component activate(ServerPlayer p, Action action) {
        CyberState s = Cyberware.state(p);
        if (Cyberware.suppressed(p)) return Component.translatable("message.constructchip.cyber.locked");
        if (CyberwareApi.isDisabled(p)) return Component.translatable("message.constructchip.cyber.disabled");
        long now = p.level().getGameTime();
        CyberSlot slot = switch (action) {
            case OS -> CyberSlot.OS;
            case FACE -> CyberSlot.FACE;
            case CIRCULATORY -> CyberSlot.CIRCULATORY;
            case ARMS, SLAM -> CyberSlot.ARMS;
        };
        var implant = s.implantAt(slot.index);
        if (implant.isEmpty()) return Component.translatable("message.constructchip.cyber.none", Component.translatable(slot.key()));
        Implant imp = implant.get();
        int lvl = Math.max(0, s.level(imp));
        String key = slot.name();
        if (action == Action.SLAM) return GorillaGrab.tryThrow(p) ? null : slam(p, s);
        long cd = s.cooldown(key, now);
        if (cd > 0) return Component.translatable("message.constructchip.hack.on_cooldown", String.format("%.1f", cd / 20f));

        switch (action) {
            case OS -> {
                int duration = Math.round(imp.activeTicks * (imp == Implant.FALCON ? 1f + 0.05f * lvl : 1f) * (imp == Implant.DYNALAR && lvl >= 4 ? 1.25f : 1f));
                s.activeUntil.put(key, now + duration);
                s.cooldownUntil.put(key, now + duration + Math.round(imp.cooldownTicks * (1f - 0.06f * lvl)));
                play(p, "er_sandevistan_activation", 1f);
                Cyberware.sendGlitch(p, 10, 0f, 0.4f);
            }
            case FACE -> {
                if (!imp.hasScan()) return Component.translatable("message.constructchip.cyber.no_scan");
                s.activeUntil.put(key, now + imp.activeTicks);
                s.cooldownUntil.put(key, now + imp.activeTicks + imp.cooldownTicks);
                play(p, "er_activate_optics", 0.8f);
            }
            case CIRCULATORY -> {
                float[] heal = {6f, 9f, 13f, 17.5f, 22f};
                p.heal(heal[Math.min(4, lvl)]);
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, imp.activeTicks, Math.min(2, lvl / 2)));
                s.cooldownUntil.put(key, now + Math.round(imp.cooldownTicks * (1f - 0.05f * lvl)));
                s.pressure = Math.min(1000, s.pressure + imp.pressurePerSecond);
                play(p, "er_heal_bloodpump", 1f);
            }
            case ARMS -> {
                s.armsDeployed = !s.armsDeployed;
                s.cooldownUntil.put(key, now + imp.cooldownTicks);
                play(p, "er_extend_shrink_gorilla_arms", 1f);
                Cyberware.refreshAttributes(p);
                ChromeVisuals.broadcast(p);
            }
            default -> { }
        }
        CyberSync.send(p);
        return null;
    }

    private static Component slam(ServerPlayer p, CyberState s) {
        if (!s.armsDeployed) return Component.translatable("message.constructchip.arms.not_deployed");
        if (s.fury < 50) return Component.translatable("message.constructchip.arms.fury", Math.round(s.fury));
        ServerLevel level = p.serverLevel();
        boolean dense = s.has(Implant.DENSE_MARROW);
        double radius = 3 + s.fury / 50.0;
        float damage = (4 + s.fury / 10f) * (dense ? 1.3f : 1f) * (1f + 0.085f * Math.max(0, s.level(Implant.GORILLA_ARMS)));
        DamageSource src = p.damageSources().playerAttack(p);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius), e -> e != p && e.isAlive())) {
            if (e.distanceToSqr(p) > radius * radius || e instanceof Player other && io.github.constructchip.ghost.GhostState.isConstruct(other)) continue;
            e.hurt(src, damage);
            Vec3 push = e.position().subtract(p.position()).normalize().scale(0.8);
            e.push(push.x, 0.45, push.z);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(), p.getZ(), 6, radius / 3, 0.2, radius / 3, 0);
        play(p, s.fury >= 90 ? "er_ground_slam_strong" : "er_ground_slam_weak", 1f);
        s.fury = 0;
        Cyberware.sendGlitch(p, 6, 0.2f, 0.2f);
        CyberSync.send(p);
        return null;
    }

    // ---------------------------------------------------------------- ticking

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        CyberState s = Cyberware.state(p);
        if (!s.any()) return;
        long now = p.level().getGameTime();
        if (now % 20 == 0) {
            Cyberware.refreshAttributes(p);
            if (s.fury > 0) s.fury = Math.max(0, s.fury - 1);
        }
        if (Cyberware.suppressed(p)) return;
        ServerLevel level = p.serverLevel();

        // Sandevistan: everything around you crawls; you get speed and haste
        if (s.isActive(CyberSlot.OS.name(), now)) {
            Implant os = s.implantAt(CyberSlot.OS.index).orElse(null);
            if (os != null) {
                int amp = Math.max(0, Math.min(6, Math.round(os.param * 8) - 1));
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(20), e -> e != p && e.isAlive())) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, amp, false, false));
                    e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 5, amp, false, false));
                }
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5, 1, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 5, 1, false, false));
                if (os == Implant.WARP_DANCER) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 5, 0, false, false));
                if (now % 10 == 0) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 3, 0.3, 0.5, 0.3, 0);
                if (s.activeUntil.get(CyberSlot.OS.name()) == now + 1) play(p, "er_sandevistan_exit", 1f);
            }
        }

        // Optics scan
        if (s.isActive(CyberSlot.FACE.name(), now) && now % 20 == 0) {
            Implant face = s.implantAt(CyberSlot.FACE.index).orElse(null);
            if (face != null) {
                if (face != Implant.DOOMSAYER) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false));
                if (face.scansHostiles()) {
                    double r = face == Implant.CLAIRVOYANT ? 19.5 : 53;
                    List<LivingEntity> hostiles = level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                            e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(p) <= r * r);
                    Net.send(p, new HighlightPayload(hostiles.stream().map(Entity::getId).toList(), 30));
                    if (face.tracksTrails()) {
                        for (LivingEntity e : hostiles) {
                            Vec3 back = e.position().subtract(e.getDeltaMovement().scale(6));
                            level.sendParticles(p, TRAIL, true, back.x, back.y + 0.1, back.z, 4, 0.2, 0, 0.2, 0);
                        }
                    }
                }
                if (face.scansHazards()) markHazards(p, level, 29);
            }
            if (s.activeUntil.get(CyberSlot.FACE.name()) <= now + 20) play(p, "er_deactivate_optics", 0.6f);
        }
    }

    private static void markHazards(ServerPlayer p, ServerLevel level, int r) {
        BlockPos c = p.blockPosition();
        int found = 0;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -8, -r), c.offset(r, 8, r))) {
            BlockState st = level.getBlockState(pos);
            if (st.is(Blocks.TNT) || st.is(Blocks.PISTON) || st.is(Blocks.STICKY_PISTON) || st.is(Blocks.SCULK_SENSOR)
                    || st.is(Blocks.CALIBRATED_SCULK_SENSOR) || st.is(Blocks.SCULK_SHRIEKER) || st.is(Blocks.TRIPWIRE)
                    || st.is(Blocks.TRIPWIRE_HOOK) || st.is(Blocks.POINTED_DRIPSTONE) || st.is(Blocks.MAGMA_BLOCK) || st.is(Blocks.POWDER_SNOW)) {
                level.sendParticles(p, HAZARD, true, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.15, 0.1, 0.15, 0);
                if (++found > 120) return;
            }
        }
    }

    // ---------------------------------------------------------------- damage model

    private static boolean physical(DamageSource src) {
        return !src.is(DamageTypeTags.IS_FIRE) && !src.is(DamageTypeTags.IS_EXPLOSION) && !src.is(DamageTypeTags.IS_PROJECTILE)
                && !src.is(DamageTypeTags.BYPASSES_ARMOR) && !src.is(DamageTypeTags.IS_FREEZING) && !src.is(DamageTypeTags.IS_LIGHTNING);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource src = event.getSource();
        long now = victim.level().getGameTime();

        // attacker side
        if (src.getEntity() instanceof ServerPlayer attacker && src.getDirectEntity() == attacker && !Cyberware.suppressed(attacker)) {
            CyberState a = Cyberware.state(attacker);
            if (a.any()) {
                float mult = 1f;
                a.lastCombat = now;
                if (a.armsDeployed && a.has(Implant.GORILLA_ARMS)) {
                    int lvl = Math.max(0, a.level(Implant.GORILLA_ARMS));
                    mult += 0.12f + 0.085f * lvl;
                    victim.addEffect(new MobEffectInstance(CyberEffectsRegistry.BLEED, 100 + 20 * lvl, 0));
                    a.fury = Math.min(100, a.fury + (a.has(Implant.DENSE_MARROW) ? 15 : 10));
                    applyElement(attacker, a, victim, now);
                }
                float hp = attacker.getHealth() / attacker.getMaxHealth();
                if (a.has(Implant.RARA_AVIS) && hp < 0.3f) mult += 0.20f;
                if (a.has(Implant.SCAR_COALESCER) && hp < 0.4f) mult += 0.10f;
                if (a.isActive(CyberSlot.OS.name(), now)) {
                    Implant os = a.implantAt(CyberSlot.OS.index).orElse(null);
                    if (os == Implant.ZETATECH) mult += victim.onGround() ? 0.12f : 0.60f;
                    if (os == Implant.DYNALAR) mult += 0.10f;
                    if (os == Implant.APOGEE) mult += 0.18f;
                }
                event.setAmount(event.getAmount() * mult);
            }
        }

        // victim side
        if (!(victim instanceof ServerPlayer p) || Cyberware.suppressed(p)) return;
        CyberState s = Cyberware.state(p);
        if (!s.any()) return;
        s.lastCombat = now;
        boolean phys = physical(src);
        boolean proj = src.is(DamageTypeTags.IS_PROJECTILE);
        boolean braced = s.has(Implant.SCARAB) && p.isCrouching();
        if (s.has(Implant.KINETIC_FRAME) && p.isSprinting() && !braced && p.getRandom().nextFloat() < 0.20f + 0.03f * s.level(Implant.KINETIC_FRAME)) {
            event.setCanceled(true);
            return;
        }
        float keep = 1f;
        if (s.has(Implant.TITANIUM_BONES)) keep *= 1f - (phys ? 0.08f + 0.03f * s.level(Implant.TITANIUM_BONES) : 0.04f);
        if (s.has(Implant.PARA_BELLUM)) keep *= 1f - (0.12f + 0.03f * s.level(Implant.PARA_BELLUM));
        if (s.has(Implant.RARA_AVIS)) keep *= 0.75f;
        if (braced && (phys || proj)) keep *= 0.65f;
        if (s.has(Implant.SCAR_COALESCER) && phys && p.getHealth() / p.getMaxHealth() < 0.4f) keep *= 0.80f;
        if (s.boosterUntil > now) keep *= 0.90f;
        float reduction = Math.min(phys ? CAP_PHYSICAL : CAP_OTHER, 1f - keep);
        event.setAmount(event.getAmount() * (1f - reduction));
        if (s.has(Implant.SCAR_COALESCER)) p.removeEffect(CyberEffectsRegistry.BLEED);
    }

    private static void applyElement(ServerPlayer attacker, CyberState a, LivingEntity victim, long now) {
        String element = CyberwareItem.element(a.at(CyberSlot.ARMS.index));
        if (element.isEmpty() || now - a.lastElemental < 300) return;
        a.lastElemental = now;
        switch (element) {
            case "thermal" -> victim.igniteForSeconds(10);
            case "electric" -> victim.addEffect(new MobEffectInstance(CyberEffectsRegistry.ELECTROSHOCK, 100, 0));
            case "toxic" -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
            case "frozen" -> victim.addEffect(new MobEffectInstance(CyberEffectsRegistry.CRYOLOCK, 100, 0));
            default -> { }
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || Cyberware.suppressed(p)) return;
        CyberState s = Cyberware.state(p);
        if (!s.any()) return;
        long now = p.level().getGameTime();
        int axo = s.level(Implant.AXOLOTL);
        if (axo >= 0) {
            float cut = axo >= 2 ? 0.075f : 0.05f;
            for (Map.Entry<String, Long> e : s.cooldownUntil.entrySet()) {
                long left = e.getValue() - now;
                if (left > 0) e.setValue(now + Math.round(left * (1f - cut)));
            }
        }
        if (s.isActive(CyberSlot.OS.name(), now)) {
            Implant os = s.implantAt(CyberSlot.OS.index).orElse(null);
            if (os == Implant.FALCON) p.heal(p.getMaxHealth() * 0.10f);
            if (os == Implant.APOGEE) {
                int extend = Math.round(os.activeTicks * (s.level(os) >= 2 ? 0.2f : 0.1f));
                s.activeUntil.computeIfPresent(CyberSlot.OS.name(), (k, v) -> v + extend);
                s.cooldownUntil.computeIfPresent(CyberSlot.OS.name(), (k, v) -> v + extend);
            }
        }
        CyberSync.send(p);
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || Cyberware.suppressed(p)) return;
        CyberState s = Cyberware.state(p);
        if (s.has(Implant.EPIMORPHIC_SKELETON)) event.setAmount(event.getAmount() * 1.2f);
        if (s.has(Implant.UNIVERSAL_BOOSTER) && event.getAmount() >= 1f) s.boosterUntil = p.level().getGameTime() + 100;
    }

    /** Spring Joints: a hard landing turns into a rebound. */
    @SubscribeEvent
    public static void onFall(net.neoforged.neoforge.event.entity.living.LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || Cyberware.suppressed(p)) return;
        if (Cyberware.state(p).has(Implant.SPRING_JOINTS) && event.getDistance() > 4) {
            p.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 1, false, true));
        }
    }

    /** Gorilla Arms: bare hands dig like an iron pickaxe. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player p = event.getEntity();
        if (!p.getMainHandItem().isEmpty() || Cyberware.suppressed(p)) return;
        CyberState s = Cyberware.state(p);
        if (s.armsDeployed && s.has(Implant.GORILLA_ARMS)) event.setNewSpeed(Math.max(event.getNewSpeed(), 6f));
    }

    @SubscribeEvent
    public static void onHarvest(PlayerEvent.HarvestCheck event) {
        Player p = event.getEntity();
        if (!p.getMainHandItem().isEmpty() || Cyberware.suppressed(p)) return;
        CyberState s = Cyberware.state(p);
        if (s.armsDeployed && s.has(Implant.GORILLA_ARMS) && !event.getTargetBlock().is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL)) {
            event.setCanHarvest(true);
        }
    }

}
