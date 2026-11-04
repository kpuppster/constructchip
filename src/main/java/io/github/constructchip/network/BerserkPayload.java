package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → player: a berserk episode against {@code targetId} for {@code ticks} (0 = it's over). */
public record BerserkPayload(int targetId, int ticks) implements CustomPacketPayload {
    public static final Type<BerserkPayload> TYPE = new Type<>(ConstructChip.id("berserk"));
    public static final StreamCodec<ByteBuf, BerserkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BerserkPayload::targetId,
            ByteBufCodecs.VAR_INT, BerserkPayload::ticks,
            BerserkPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
