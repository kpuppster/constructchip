package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Server → nearby clients: draw a Capri-style lightning effect. {@code preset} is an
 * {@link io.github.constructchip.fx.BoltFx.Preset} ordinal; {@code source} the entity the bolt comes from (or -1),
 * so its own client can keep the middle of the screen clear; {@code from} the start point and {@code to} the
 * end points (one per target).
 */
public record BoltFxPayload(int preset, int source, Vec3 from, List<Vec3> to) implements CustomPacketPayload {
    public static final Type<BoltFxPayload> TYPE = new Type<>(ConstructChip.id("bolt_fx"));
    private static final int MAX_TARGETS = 32;

    public static final StreamCodec<FriendlyByteBuf, BoltFxPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.preset);
                buf.writeVarInt(p.source);
                buf.writeVec3(p.from);
                int n = Math.min(MAX_TARGETS, p.to.size());
                buf.writeVarInt(n);
                for (int i = 0; i < n; i++) buf.writeVec3(p.to.get(i));
            },
            buf -> {
                int preset = buf.readVarInt();
                int source = buf.readVarInt();
                Vec3 from = buf.readVec3();
                int n = Math.min(MAX_TARGETS, buf.readVarInt());
                List<Vec3> to = new ArrayList<>(n);
                for (int i = 0; i < n; i++) to.add(buf.readVec3());
                return new BoltFxPayload(preset, source, from, to);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
