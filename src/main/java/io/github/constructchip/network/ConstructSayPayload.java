package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → host: the construct in your head said {@code text}. Shown in the Capri-style speech box, not in chat. */
public record ConstructSayPayload(String name, String text) implements CustomPacketPayload {
    public static final Type<ConstructSayPayload> TYPE = new Type<>(ConstructChip.id("construct_say"));
    public static final StreamCodec<ByteBuf, ConstructSayPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), ConstructSayPayload::name,
            ByteBufCodecs.stringUtf8(512), ConstructSayPayload::text,
            ConstructSayPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
