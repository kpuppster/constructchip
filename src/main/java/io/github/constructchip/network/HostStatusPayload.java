package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

/**
 * Server → construct, twice a second: the host's vitals for the HUD. Effects and worn-out gear are only filled in
 * with the Vitals Link skill.
 */
public record HostStatusPayload(float health, float maxHealth, int food, float saturation, int armor, int air,
                                List<MobEffectInstance> effects, List<ItemStack> wornOut) implements CustomPacketPayload {
    public static final Type<HostStatusPayload> TYPE = new Type<>(ConstructChip.id("host_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HostStatusPayload> STREAM_CODEC =
            StreamCodec.of(HostStatusPayload::write, HostStatusPayload::read);

    private static final StreamCodec<RegistryFriendlyByteBuf, List<MobEffectInstance>> EFFECTS =
            MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list(32));
    private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> ITEMS =
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(41));

    private static void write(RegistryFriendlyByteBuf buf, HostStatusPayload p) {
        buf.writeFloat(p.health);
        buf.writeFloat(p.maxHealth);
        buf.writeVarInt(p.food);
        buf.writeFloat(p.saturation);
        buf.writeVarInt(p.armor);
        buf.writeVarInt(p.air);
        EFFECTS.encode(buf, p.effects);
        ITEMS.encode(buf, p.wornOut);
    }

    private static HostStatusPayload read(RegistryFriendlyByteBuf buf) {
        return new HostStatusPayload(buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readFloat(),
                buf.readVarInt(), buf.readVarInt(), EFFECTS.decode(buf), ITEMS.decode(buf));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
