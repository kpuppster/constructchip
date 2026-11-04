package io.github.constructchip.hack.builtin;

import io.github.constructchip.network.GlitchPayload;
import io.github.constructchip.network.HighlightPayload;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import io.github.constructchip.network.Net;

/** Shared feedback for hacks: client-only highlights, a quiet glitch sound for the construct, sparks on the target. */
final class HackFx {
    private HackFx() {}

    static void highlight(List<Integer> ids, int ticks, ServerPlayer... viewers) {
        HighlightPayload payload = new HighlightPayload(ids, ticks);
        for (ServerPlayer p : viewers) Net.send(p, payload);
    }

    static void glitch(ServerPlayer construct) {
        construct.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    static void sparks(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.05);
    }

    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1f, 0.08f, 0.12f), 1.1f);

    /**
     * A jagged red arc from {@code from} to {@code to}: segments jittered sideways, dust along each one, and a
     * burst at the end. Server-side particles, so everyone nearby sees the lightning (the construct stays hidden).
     */
    static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        RandomSource r = level.random;
        Vec3 prev = from;
        int segments = Math.max(3, (int) (from.distanceTo(to) / 1.5));
        for (int i = 1; i <= segments; i++) {
            Vec3 point = from.lerp(to, i / (double) segments);
            if (i < segments) point = point.add((r.nextDouble() - 0.5) * 0.7, (r.nextDouble() - 0.5) * 0.7, (r.nextDouble() - 0.5) * 0.7);
            int steps = Math.max(2, (int) (prev.distanceTo(point) * 5));
            for (int j = 0; j < steps; j++) {
                Vec3 p = prev.lerp(point, j / (double) steps);
                level.sendParticles(RED, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            prev = point;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 10, 0.25, 0.25, 0.25, 0.08);
        level.sendParticles(RED, to.x, to.y, to.z, 8, 0.3, 0.3, 0.3, 0);
    }

    static Vec3 chest(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.6, 0);
    }

    /** Tell the construct's client to flash its glitch overlay. */
    static void glitchFlash(ServerPlayer construct, int ticks) {
        Net.send(construct, new GlitchPayload(ticks));
    }

    static void sparks(ServerLevel level, Entity e) {
        sparks(level, e.position().add(0, e.getBbHeight() * 0.6, 0));
    }
}
