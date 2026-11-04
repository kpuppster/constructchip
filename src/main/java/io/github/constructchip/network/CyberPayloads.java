package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.cyberware.CyberEffects;
import io.github.constructchip.cyberware.Cyberware;
import io.github.constructchip.cyberware.CyberState;
import io.github.constructchip.cyberware.Implant;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server cyberware requests. */
public final class CyberPayloads {
    private CyberPayloads() {}

    /** Activate an implant: OS, ARMS, FACE, CIRCULATORY or SLAM. */
    public record Activate(int action) implements CustomPacketPayload {
        public static final Type<Activate> TYPE = new Type<>(ConstructChip.id("cyber_activate"));
        public static final StreamCodec<ByteBuf, Activate> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Activate::new, Activate::action);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void handle(Activate payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer p)) return;
                CyberEffects.Action[] all = CyberEffects.Action.values();
                if (payload.action < 0 || payload.action >= all.length) return;
                Component problem = CyberEffects.activate(p, all[payload.action]);
                if (problem != null) p.displayClientMessage(problem, true);
            });
        }
    }

    /** Legs: the client did a double jump (Tendons) or a charged jump (Ankles); the server forgives the fall. */
    public record Jump(boolean charged) implements CustomPacketPayload {
        public static final Type<Jump> TYPE = new Type<>(ConstructChip.id("cyber_jump"));
        public static final StreamCodec<ByteBuf, Jump> STREAM_CODEC = ByteBufCodecs.BOOL.map(Jump::new, Jump::charged);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void handle(Jump payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer p) || Cyberware.suppressed(p)) return;
                CyberState s = Cyberware.state(p);
                Implant needed = payload.charged ? Implant.FORTIFIED_ANKLES : Implant.REINFORCED_TENDONS;
                if (!s.has(needed)) return;
                p.resetFallDistance();
                s.pressure = Math.min(1000, s.pressure + 10);
            });
        }
    }
}
