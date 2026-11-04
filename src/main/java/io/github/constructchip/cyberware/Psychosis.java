package io.github.constructchip.cyberware;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.skill.ModSkills;
import io.github.constructchip.tracking.ChipTracker;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Cyberpsychosis (Project: Edgerunner's design, our implementation). Pressure (0..1000) is short-term stress from
 * overload and running implants. Degradation (0..1000) is the disease, which climbs while Pressure is high.
 * Five phases with hysteresis. Phase V starts a 120 s terminal window: stabilize or flatline.
 * Immunosuppressants raise capacity, drain pressure and can roll a phase back.
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class Psychosis {
    static final int[] ENTER = {140, 340, 600, 840, 980};
    static final int[] EXIT = {90, 260, 500, 730, 930};
    public static final int TERMINAL_TICKS = 120 * 20;
    public static final int IMMUNO_TICKS = 30 * 60 * 20;
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ConstructChip.id("cyberpsychosis"));
    public static final ResourceLocation NEURAL_BUFFER = ConstructChip.id("neural_buffer");

    private Psychosis() {}

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().getGameTime() % 20 != 0 || ChipTracker.isConstruct(p)) return;
        update(p);
    }

    /** One second of cyberpsychosis: pressure, degradation, phase, symptoms. */
    public static void update(ServerPlayer p) {
        CyberState s = Cyberware.state(p);
        if (!s.any() && s.phase == 0 && s.pressure <= 0 && s.degradation <= 0) return;
        s.rollIfNeeded(p.getRandom());
        long now = p.level().getGameTime();
        boolean buffer = hasNeuralBuffer(p);

        // ---- pressure
        int load = Cyberware.load(p), cap = Cyberware.capacity(p);
        float gain = 0;
        if (load > cap) gain += (load - cap) / (float) cap * 600f;
        boolean running = false;
        if (s.isActive(CyberSlot.OS.name(), now)) { gain += 160; running = true; }
        if (s.armsDeployed) { gain += 140; running = true; }
        if (s.isActive(CyberSlot.FACE.name(), now)) { gain += 120; running = true; }
        if (s.has(Implant.RARA_AVIS) && now - s.lastCombat < 100) gain += 40;
        if (s.immunoUntil > now) gain -= 80;
        if (s.has(Implant.EPIMORPHIC_SKELETON)) gain -= 20 + 5 * s.level(Implant.EPIMORPHIC_SKELETON);
        if (s.boosterUntil > now) gain -= 40;
        if (!running) gain -= buffer ? 80 : 60;
        s.pressure = Math.max(0, Math.min(1000, s.pressure + gain));

        // ---- degradation
        float p0 = s.pressure, d = 0;
        if (p0 >= 720) d = 4; else if (p0 >= 560) d = 3; else if (p0 >= 420) d = 2; else if (p0 >= 300) d = 1;
        else if (p0 < 60) d = -2; else if (p0 < 120) d = -1;
        if (d > 0 && buffer) d *= 0.75f;
        if (s.immunoUntil > now) d -= 1.5f;
        if (s.phase >= 4 && p0 >= 300) d += 1;
        s.degradation = Math.max(0, Math.min(1000, s.degradation + d));

        // ---- phase with hysteresis
        int phase = s.phase;
        while (phase < 5 && s.degradation >= ENTER[phase]) phase++;
        while (phase > 0 && s.degradation < EXIT[phase - 1]) phase--;
        if (phase != s.phase) onPhaseChange(p, s, s.phase, phase, now);
        s.phase = phase;
        symptoms(p, s, now);
        CyberSync.send(p);
    }

    private static void onPhaseChange(ServerPlayer p, CyberState s, int from, int to, long now) {
        if (to > from) {
            play(p, to >= 4 ? "er_cyberpsychosis_stinger_critical" : "er_cyberpsychosis_stinger_phase", 1f);
            Cyberware.sendGlitch(p, 30, 0.6f, 0.4f);
            p.displayClientMessage(Component.translatable("message.constructchip.psychosis.phase", to), false);
        }
        if (to == 5 && from < 5) s.terminalUntil = now + TERMINAL_TICKS;
        if (to < 5) s.terminalUntil = 0;
    }

    private static void symptoms(ServerPlayer p, CyberState s, long now) {
        RandomSource r = p.getRandom();
        if (s.phase > 0) p.addEffect(new MobEffectInstance(CyberEffectsRegistry.CYBERPSYCHOSIS, 40, s.phase - 1, true, true, true));
        if (s.phase >= 1 && r.nextFloat() < 0.08f + 0.04f * s.phase) {
            Cyberware.sendGlitch(p, 6 + 3 * s.phase, 0.1f * s.phase, 0f);
            if (r.nextFloat() < 0.3f) p.displayClientMessage(Component.translatable("message.constructchip.psychosis.error." + r.nextInt(4)), true);
        }
        if (s.phase >= 2) {
            if (r.nextFloat() < 0.06f) play(p, r.nextBoolean() ? "er_cyberpsychosis_stinger_hallucination" : "er_cyberpsychosis_whisper", 0.8f);
            if (now % 600 == 0) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            if (r.nextFloat() < 0.03f) { // misfire: a random implant locks up
                String[] keys = {CyberSlot.OS.name(), CyberSlot.FACE.name(), CyberSlot.CIRCULATORY.name()};
                String k = keys[r.nextInt(keys.length)];
                s.cooldownUntil.put(k, Math.max(s.cooldownUntil.getOrDefault(k, 0L), now + 200));
            }
        }
        if (s.phase >= 3) {
            if (r.nextFloat() < 0.05f) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            if (r.nextFloat() < 0.08f) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            if (r.nextFloat() < 0.04f) play(p, "er_cyberpsychosis_stinger_watcher", 0.8f);
        }
        if (s.phase >= 4) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 0, true, false));
            if (now % 40 == 0) play(p, "er_cyberpsychosis_heartbeat", 1f);
        }
        if (s.phase == 5 && s.terminalUntil > 0) {
            // berserk: the chrome takes over
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false));
            if (now >= s.terminalUntil) {
                s.terminalUntil = 0;
                DamageSource src = new DamageSource(p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE));
                p.hurt(src, Float.MAX_VALUE);
            }
        }
    }

    /** Dying in Phase V brings you back in Phase IV crisis (their rule), not cured. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.isEndConquered()) return;
        CyberState s = Cyberware.state(p);
        if (s.phase == 5) {
            s.phase = 4;
            s.degradation = 840;
            s.pressure = 300;
            s.terminalUntil = 0;
        }
        Cyberware.refreshAttributes(p);
        CyberSync.send(p);
    }

    public static void dose(ServerPlayer p) {
        CyberState s = Cyberware.state(p);
        long now = p.level().getGameTime();
        s.immunoDoses = s.immunoUntil > now ? s.immunoDoses + 1 : 1;
        s.immunoUntil = now + IMMUNO_TICKS;
        s.pressure = Math.max(0, s.pressure - (80 + 30 * s.phase));
        s.degradation = Math.max(0, s.degradation - (60 + 30 * s.phase));
        p.addEffect(new MobEffectInstance(CyberEffectsRegistry.IMMUNOSUPPRESSION, IMMUNO_TICKS, Math.min(4, s.immunoDoses - 1)));
        play(p, "er_immunosupresors_inject", 1f);
        CyberSync.send(p);
    }

    /** The construct's Neural Buffer skill (on the chip in your head) calms your chrome. */
    static boolean hasNeuralBuffer(ServerPlayer host) {
        ItemStack chip = ChipTracker.installedChip(host);
        return ChipStacks.isCaptured(chip) && ChipStacks.skills(chip).has(NEURAL_BUFFER)
                && ModSkills.get(host.server.registryAccess()).containsKey(NEURAL_BUFFER);
    }

    private static void play(ServerPlayer p, String name, float volume) {
        SoundEvent e = CyberEffects.sound(name);
        if (e != null) p.playNotifySound(e, SoundSource.AMBIENT, volume, 1f);
    }
}
