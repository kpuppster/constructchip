package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → construct: run the glitch screen effect for {@code ticks}. {@code red} washes the screen red,
 * {@code flash} is a white-red hit on the first frames (lightning). All 0..1.
 */
public record GlitchPayload(int ticks, float red, float flash) implements CustomPacketPayload {
    public static final Type<GlitchPayload> TYPE = new Type<>(ConstructChip.id("glitch"));
    public static final StreamCodec<ByteBuf, GlitchPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GlitchPayload::ticks,
            ByteBufCodecs.FLOAT, GlitchPayload::red,
            ByteBufCodecs.FLOAT, GlitchPayload::flash,
            GlitchPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
