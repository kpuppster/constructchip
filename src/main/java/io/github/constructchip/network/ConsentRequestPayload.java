package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → target: "do you agree?" Shown as a screen with Accept / Decline. */
public record ConsentRequestPayload(int requestId, Component title, Component body, int timeoutTicks)
        implements CustomPacketPayload {
    public static final Type<ConsentRequestPayload> TYPE = new Type<>(ConstructChip.id("consent_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConsentRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ConsentRequestPayload::requestId,
            ComponentSerialization.STREAM_CODEC, ConsentRequestPayload::title,
            ComponentSerialization.STREAM_CODEC, ConsentRequestPayload::body,
            ByteBufCodecs.VAR_INT, ConsentRequestPayload::timeoutTicks,
            ConsentRequestPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
