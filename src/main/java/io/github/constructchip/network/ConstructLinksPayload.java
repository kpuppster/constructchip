package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → every client: which players are installed constructs, and whose head they're in. Drives render hiding. */
public record ConstructLinksPayload(Map<UUID, UUID> constructToHost) implements CustomPacketPayload {
    public static final Type<ConstructLinksPayload> TYPE = new Type<>(ConstructChip.id("construct_links"));
    public static final StreamCodec<ByteBuf, ConstructLinksPayload> STREAM_CODEC = ByteBufCodecs
            .<ByteBuf, UUID, UUID, Map<UUID, UUID>>map(HashMap::new, UUIDUtil.STREAM_CODEC, UUIDUtil.STREAM_CODEC)
            .map(ConstructLinksPayload::new, ConstructLinksPayload::constructToHost);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
