package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.paint.Paint;
import io.github.constructchip.paint.PaintingMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: save the painting of one texture onto the implant in the open painting station. */
public record PaintPayload(String texture, String data) implements CustomPacketPayload {
    public static final Type<PaintPayload> TYPE = new Type<>(ConstructChip.id("paint"));
    public static final StreamCodec<ByteBuf, PaintPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), PaintPayload::texture,
            ByteBufCodecs.stringUtf8(Paint.MAX_LENGTH), PaintPayload::data,
            PaintPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    static void handle(PaintPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p) || !(p.containerMenu instanceof PaintingMenu menu)) return;
            var stack = menu.implant();
            if (!Paint.textures(stack).contains(payload.texture())) return;
            Paint.set(stack, payload.texture(), payload.data());
            menu.input.setChanged();
        });
    }
}
