package io.github.constructchip.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Per-wearer cyberware disable timers (game time). See {@code CyberwareApi}. */
public record CyberwareState(long disabledUntil, long lockedUntil) {
    public static final CyberwareState NONE = new CyberwareState(0L, 0L);
    public static final Codec<CyberwareState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("disabled_until").forGetter(CyberwareState::disabledUntil),
            Codec.LONG.fieldOf("locked_until").forGetter(CyberwareState::lockedUntil)
    ).apply(i, CyberwareState::new));
}
