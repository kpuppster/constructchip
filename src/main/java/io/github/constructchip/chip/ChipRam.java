package io.github.constructchip.chip;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * RAM pool stored on the chip (not the player), regenerated lazily:
 * {@code current = min(max, stored + regenPerSecond * (now - stampTick) / 20)} with {@code now = level.getGameTime()}.
 * Nothing is written per tick; a new instance is only written when RAM is spent or the pool changes.
 *
 * @param max            maximum RAM
 * @param regenPerSecond RAM regained per second (20 game ticks)
 * @param stored         RAM at {@code stampTick}
 * @param stampTick      game time when {@code stored} was recorded
 */
public record ChipRam(int max, float regenPerSecond, float stored, long stampTick) {
    public static final Codec<ChipRam> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max").forGetter(ChipRam::max),
            Codec.floatRange(0f, Float.MAX_VALUE).fieldOf("regen_per_second").forGetter(ChipRam::regenPerSecond),
            Codec.FLOAT.fieldOf("stored").forGetter(ChipRam::stored),
            Codec.LONG.fieldOf("stamp_tick").forGetter(ChipRam::stampTick)
    ).apply(i, ChipRam::new));

    public static final StreamCodec<ByteBuf, ChipRam> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChipRam::max,
            ByteBufCodecs.FLOAT, ChipRam::regenPerSecond,
            ByteBufCodecs.FLOAT, ChipRam::stored,
            ByteBufCodecs.VAR_LONG, ChipRam::stampTick,
            ChipRam::new);

    /** A full pool stamped at {@code now}. */
    public static ChipRam full(int max, float regenPerSecond, long now) {
        return new ChipRam(max, regenPerSecond, max, now);
    }

    public float current(long now) {
        long elapsed = Math.max(0L, now - stampTick); // never negative, so a stamp from the future cannot drain RAM
        float value = stored + regenPerSecond * elapsed / 20f;
        return Math.max(0f, Math.min(max, value));
    }

    public boolean canAfford(float cost, long now) {
        return current(now) >= cost;
    }

    /** Returns the pool after spending {@code cost}. Caller must check {@link #canAfford} first. */
    public ChipRam spend(float cost, long now) {
        return new ChipRam(max, regenPerSecond, Math.max(0f, current(now) - cost), now);
    }

    /** Re-stamps the pool at {@code now} so changes to max/regen don't retroactively apply to past ticks. */
    public ChipRam withStats(int newMax, float newRegenPerSecond, long now) {
        return new ChipRam(newMax, newRegenPerSecond, Math.min(newMax, current(now)), now);
    }

    /** Ticks until the pool holds at least {@code amount}, or -1 if it never will. */
    public long ticksUntil(float amount, long now) {
        float cur = current(now);
        if (cur >= amount) return 0L;
        if (amount > max || regenPerSecond <= 0f) return -1L;
        return (long) Math.ceil((amount - cur) * 20f / regenPerSecond);
    }
}
