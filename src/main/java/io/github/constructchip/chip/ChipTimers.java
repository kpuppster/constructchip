package io.github.constructchip.chip;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Absolute game-time deadlines stored on the chip (not potion effects), so they survive the chip changing hands.
 *
 * @param overclockUntil game time when Overclock ends (0 = never active)
 * @param lockoutUntil   game time when the post-Overclock lockout ends; all chip abilities are blocked before it
 * @param cooldowns      per-quickhack id: game time when it is usable again (immutable)
 */
public record ChipTimers(long overclockUntil, long lockoutUntil, Map<ResourceLocation, Long> cooldowns) {
    public static final ChipTimers NONE = new ChipTimers(0L, 0L, Map.of());

    /** Overclock lasts 30 s, followed by a 60 s lockout. */
    public static final int OVERCLOCK_TICKS = 30 * 20;
    public static final int LOCKOUT_TICKS = 60 * 20;

    public static final Codec<ChipTimers> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("overclock_until", 0L).forGetter(ChipTimers::overclockUntil),
            Codec.LONG.optionalFieldOf("lockout_until", 0L).forGetter(ChipTimers::lockoutUntil),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)
                    .optionalFieldOf("cooldowns", Map.of()).forGetter(ChipTimers::cooldowns)
    ).apply(i, ChipTimers::new));

    public static final StreamCodec<ByteBuf, ChipTimers> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, ChipTimers::overclockUntil,
            ByteBufCodecs.VAR_LONG, ChipTimers::lockoutUntil,
            ByteBufCodecs.<ByteBuf, ResourceLocation, Long, Map<ResourceLocation, Long>>map(
                    HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_LONG), ChipTimers::cooldowns,
            ChipTimers::new);

    public ChipTimers {
        cooldowns = Map.copyOf(cooldowns);
    }

    public boolean isOverclocked(long now) {
        return now < overclockUntil;
    }

    /** True during the lockout that follows Overclock (Overclock itself is not a lockout). */
    public boolean isLockedOut(long now) {
        return !isOverclocked(now) && now < lockoutUntil;
    }

    public long cooldownRemaining(ResourceLocation hack, long now) {
        Long readyAt = cooldowns.get(hack);
        return readyAt == null ? 0L : Math.max(0L, readyAt - now);
    }

    public ChipTimers startOverclock(long now) {
        return startOverclock(now, OVERCLOCK_TICKS, LOCKOUT_TICKS);
    }

    /** Durations after skill modifiers (see ChipStats). */
    public ChipTimers startOverclock(long now, int overclockTicks, int lockoutTicks) {
        return new ChipTimers(now + overclockTicks, now + overclockTicks + lockoutTicks, cooldowns);
    }

    /** Sets a cooldown and drops expired entries so the map doesn't grow forever. */
    public ChipTimers withCooldown(ResourceLocation hack, int ticks, long now) {
        Map<ResourceLocation, Long> next = new HashMap<>();
        cooldowns.forEach((id, readyAt) -> {
            if (readyAt > now) next.put(id, readyAt);
        });
        next.put(hack, now + ticks);
        return new ChipTimers(overclockUntil, lockoutUntil, next);
    }
}
