package io.github.constructchip.fx;

import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.Quickhack;
import io.github.constructchip.network.BoltFxPayload;
import io.github.constructchip.network.Net;
import io.github.constructchip.registry.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Server side of the red lightning (drawn on clients by {@code client.CapriBolts}, a port of Ikeda's Capri bolts,
 * used with permission). Every successful quickhack fires one: a bolt from the host's hand to what was hacked, or
 * a crackle over the host's body for hacks without a target. Fry, High Voltage and the Soul Killer capture use
 * their own bigger presets. Sound plays for everyone nearby; the visuals go to every player within
 * {@link #RANGE} blocks.
 */
public final class BoltFx {
    private BoltFx() {}

    public static final double RANGE = 96;
    private static final ResourceLocation SCANNER = ResourceLocation.fromNamespaceAndPath("constructchip", "scanner");

    /** Keep the order: clients receive the ordinal. */
    public enum Preset {
        /** Generic quickhack: a quick red bolt with a thin pale strand. */
        HACK,
        /** Scanner: one thin pale strand. */
        SCAN,
        /** Hack with no target: sparks crawl over the source's body. */
        AURA,
        /** Fry: Capri's hack bolt (thick, branching) plus a ground burst. */
        FRY,
        /** High Voltage: a bolt out of the sky on every target, ground bursts, and arcs from the host's hand. */
        HIGH_VOLTAGE,
        /** Soul Killer, while capturing: bolts from above into the sitter's head, sparks crawling on them. */
        CAPTURE,
        /** Soul Killer, the moment the mind is taken: one huge strike. */
        CAPTURE_END
    }

    public static void send(ServerLevel level, Preset preset, Entity source, Vec3 from, List<Vec3> to) {
        BoltFxPayload payload = new BoltFxPayload(preset.ordinal(), source == null ? -1 : source.getId(), from, to);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(from) <= RANGE * RANGE) Net.send(p, payload);
        }
    }

    public static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void crackle(ServerLevel level, Vec3 at, float volume) {
        sound(level, at, ModSounds.CRACKLE.get(), volume, 0.85f + level.random.nextFloat() * 0.4f);
    }

    /** Roughly where the player's right hand is: a bit below and in front of the eyes, off to the right. */
    public static Vec3 hand(Player p) {
        Vec3 look = p.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        if (right.lengthSqr() < 1e-6) right = new Vec3(1, 0, 0);
        return p.getEyePosition().add(0, -0.45, 0).add(right.scale(0.35)).add(look.scale(0.35));
    }

    public static Vec3 chest(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.6, 0);
    }

    /** Called by the quickhack service after every successful hack. */
    public static void afterHack(HackContext ctx, Quickhack hack) {
        if (hack.drawsOwnFx()) return;
        ServerLevel level = ctx.level();
        ServerPlayer host = ctx.host();
        boolean scan = SCANNER.equals(ctx.hackId());
        Vec3 to = null;
        if (ctx.entity().isPresent()) {
            to = chest(ctx.entity().get());
        } else if (ctx.block().isPresent()) {
            BlockPos pos = ctx.block().get();
            Direction face = ctx.face();
            to = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        }
        if (to == null) {
            send(level, Preset.AURA, host, chest(host), List.of());
            crackle(level, chest(host), 0.6f);
            return;
        }
        send(level, scan ? Preset.SCAN : Preset.HACK, host, hand(host), List.of(to));
        crackle(level, to, scan ? 0.3f : 0.8f);
    }
}
