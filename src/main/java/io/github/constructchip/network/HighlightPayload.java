package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → construct/host: outline these entities (network ids) for {@code ticks}, on this client only. */
public record HighlightPayload(List<Integer> entityIds, int ticks) implements CustomPacketPayload {
    public static final Type<HighlightPayload> TYPE = new Type<>(ConstructChip.id("highlight"));
    public static final StreamCodec<ByteBuf, HighlightPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(256)), HighlightPayload::entityIds,
            ByteBufCodecs.VAR_INT, HighlightPayload::ticks,
            HighlightPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
