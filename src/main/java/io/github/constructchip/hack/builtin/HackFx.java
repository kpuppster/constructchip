package io.github.constructchip.hack.builtin;

import io.github.constructchip.network.HighlightPayload;
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

    static void sparks(ServerLevel level, Entity e) {
        sparks(level, e.position().add(0, e.getBbHeight() * 0.6, 0));
    }
}
