package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.hack.HackTarget;
import io.github.constructchip.hack.QuickhackService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: "the construct wants to run this quickhack on this target". Everything is re-validated. */
public record UseQuickhackPayload(ResourceLocation hack, HackTarget target) implements CustomPacketPayload {
    public static final Type<UseQuickhackPayload> TYPE = new Type<>(ConstructChip.id("use_quickhack"));

    public static final StreamCodec<ByteBuf, UseQuickhackPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, UseQuickhackPayload::hack,
            HackTarget.STREAM_CODEC, UseQuickhackPayload::target,
            UseQuickhackPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(UseQuickhackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer construct)) return;
            QuickhackService.deliver(construct, QuickhackService.use(construct, payload.hack(), payload.target()));
        });
    }
}
