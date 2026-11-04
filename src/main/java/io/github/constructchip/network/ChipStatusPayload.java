package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.chip.ChipRam;
import io.github.constructchip.chip.ChipSkills;
import io.github.constructchip.chip.ChipTimers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → construct and host: the chip they share. RAM is sent as the lazy pool (stored + stamp), so the client
 * works out the current value itself and nothing needs sending per tick.
 *
 * @param present false = "you have no chip" (clears the client state)
 * @param isHost  true for the host's copy, false for the construct's
 */
public record ChipStatusPayload(boolean present, boolean isHost, String constructName, String hostName,
                                ChipRam ram, ChipTimers timers, ChipSkills skills, float ramCostMultiplier,
                                long nextPointCost) implements CustomPacketPayload {
    public static final Type<ChipStatusPayload> TYPE = new Type<>(ConstructChip.id("chip_status"));
    public static final ChipStatusPayload NONE = new ChipStatusPayload(false, false, "", "",
            new ChipRam(0, 0f, 0f, 0L), ChipTimers.NONE, ChipSkills.DEFAULT, 1f, 0L);

    public static final StreamCodec<RegistryFriendlyByteBuf, ChipStatusPayload> STREAM_CODEC =
            StreamCodec.of(ChipStatusPayload::write, ChipStatusPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, ChipStatusPayload p) {
        buf.writeBoolean(p.present);
        if (!p.present) return;
        buf.writeBoolean(p.isHost);
        buf.writeUtf(p.constructName);
        buf.writeUtf(p.hostName);
        ChipRam.STREAM_CODEC.encode(buf, p.ram);
        ChipTimers.STREAM_CODEC.encode(buf, p.timers);
        ChipSkills.STREAM_CODEC.encode(buf, p.skills);
        buf.writeFloat(p.ramCostMultiplier);
        buf.writeVarLong(p.nextPointCost);
    }

    private static ChipStatusPayload read(RegistryFriendlyByteBuf buf) {
        if (!buf.readBoolean()) return NONE;
        return new ChipStatusPayload(true, buf.readBoolean(), buf.readUtf(), buf.readUtf(),
                ChipRam.STREAM_CODEC.decode(buf), ChipTimers.STREAM_CODEC.decode(buf), ChipSkills.STREAM_CODEC.decode(buf),
                buf.readFloat(), buf.readVarLong());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
