package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → construct (or host): flash the red glitch overlay for {@code ticks}. */
public record GlitchPayload(int ticks) implements CustomPacketPayload {
    public static final Type<GlitchPayload> TYPE = new Type<>(ConstructChip.id("glitch"));
    public static final StreamCodec<ByteBuf, GlitchPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(GlitchPayload::new, GlitchPayload::ticks);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
