package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → everyone: running Takeovers (construct, host, game time it ends). Sent whole on every change. */
public record PossessionPayload(List<Entry> entries) implements CustomPacketPayload {
    public record Entry(UUID construct, UUID host, long endsAt) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Entry::construct,
                UUIDUtil.STREAM_CODEC, Entry::host,
                ByteBufCodecs.VAR_LONG, Entry::endsAt,
                Entry::new);
    }

    public static final Type<PossessionPayload> TYPE = new Type<>(ConstructChip.id("possession"));
    public static final StreamCodec<ByteBuf, PossessionPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(64)), PossessionPayload::entries,
            PossessionPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
