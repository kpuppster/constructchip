package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client: a player's cyberware. {@code ofHost} = this is your host's chrome (sent to a construct),
 * otherwise it's your own. Times are "ticks left", so the client needs no clock sync.
 */
public record CyberStatusPayload(List<String> implants, List<Integer> levels, int load, int capacity, float pressure,
                                 float degradation, int phase, long terminalLeft, List<Long> cooldowns, List<Long> actives,
                                 boolean armsDeployed, float fury, long immunoLeft, boolean suppressed, boolean ofHost)
        implements CustomPacketPayload {
    public static final Type<CyberStatusPayload> TYPE = new Type<>(ConstructChip.id("cyber_status"));

    public CyberStatusPayload(List<String> implants, List<Integer> levels, int load, int capacity, float pressure,
                              float degradation, int phase, long terminalLeft, List<Long> cooldowns, List<Long> actives,
                              boolean armsDeployed, float fury, long immunoLeft, boolean suppressed) {
        this(implants, levels, load, capacity, pressure, degradation, phase, terminalLeft, cooldowns, actives, armsDeployed,
                fury, immunoLeft, suppressed, false);
    }

    public CyberStatusPayload forSelf() { return withHost(false); }
    public CyberStatusPayload forConstruct() { return withHost(true); }

    private CyberStatusPayload withHost(boolean host) {
        return new CyberStatusPayload(implants, levels, load, capacity, pressure, degradation, phase, terminalLeft, cooldowns,
                actives, armsDeployed, fury, immunoLeft, suppressed, host);
    }

    public boolean any() {
        for (String s : implants) if (!s.isEmpty()) return true;
        return false;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CyberStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)).encode(buf, p.implants);
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(16)).encode(buf, p.levels);
                buf.writeVarInt(p.load);
                buf.writeVarInt(p.capacity);
                buf.writeFloat(p.pressure);
                buf.writeFloat(p.degradation);
                buf.writeVarInt(p.phase);
                buf.writeVarLong(p.terminalLeft);
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(16)).encode(buf, p.cooldowns);
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(16)).encode(buf, p.actives);
                buf.writeBoolean(p.armsDeployed);
                buf.writeFloat(p.fury);
                buf.writeVarLong(p.immunoLeft);
                buf.writeBoolean(p.suppressed);
                buf.writeBoolean(p.ofHost);
            },
            buf -> new CyberStatusPayload(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)).decode(buf),
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(16)).decode(buf),
                    buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarLong(),
                    ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(16)).decode(buf),
                    ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list(16)).decode(buf),
                    buf.readBoolean(), buf.readFloat(), buf.readVarLong(), buf.readBoolean(), buf.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
