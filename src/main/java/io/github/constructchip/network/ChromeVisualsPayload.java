package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Server → clients: a player's installed implants (with paint and element) for the 3D models. */
public record ChromeVisualsPayload(int entityId, List<ItemStack> installed, boolean armsDeployed, int sandevistanTicks) implements CustomPacketPayload {
    public static final Type<ChromeVisualsPayload> TYPE = new Type<>(ConstructChip.id("chrome_visuals"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChromeVisualsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChromeVisualsPayload::entityId,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(16)), ChromeVisualsPayload::installed,
            ByteBufCodecs.BOOL, ChromeVisualsPayload::armsDeployed,
            ByteBufCodecs.VAR_INT, ChromeVisualsPayload::sandevistanTicks,
            ChromeVisualsPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
