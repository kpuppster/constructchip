package io.github.constructchip.network;

import io.github.constructchip.ConstructChip;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Small client → server requests. Every one is re-validated on the server. */
public final class ServerboundPayloads {
    private ServerboundPayloads() {}

    /** Construct: open the read-only view of the host's inventory. */
    public record OpenHostInventory() implements CustomPacketPayload {
        public static final Type<OpenHostInventory> TYPE = new Type<>(ConstructChip.id("open_host_inventory"));
        public static final StreamCodec<ByteBuf, OpenHostInventory> STREAM_CODEC = StreamCodec.unit(new OpenHostInventory());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Construct or host: unlock a skill on the shared chip with its unspent points. */
    public record UnlockSkill(ResourceLocation skill) implements CustomPacketPayload {
        public static final Type<UnlockSkill> TYPE = new Type<>(ConstructChip.id("unlock_skill"));
        public static final StreamCodec<ByteBuf, UnlockSkill> STREAM_CODEC =
                ResourceLocation.STREAM_CODEC.map(UnlockSkill::new, UnlockSkill::skill);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Host only: buy skill points for the installed chip with their own XP. */
    public record BuySkillPoint(int count) implements CustomPacketPayload {
        public static final Type<BuySkillPoint> TYPE = new Type<>(ConstructChip.id("buy_skill_point"));
        public static final StreamCodec<ByteBuf, BuySkillPoint> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(BuySkillPoint::new, BuySkillPoint::count);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Target's answer to a ConsentRequestPayload. */
    public record ConsentReply(int requestId, boolean accept) implements CustomPacketPayload {
        public static final Type<ConsentReply> TYPE = new Type<>(ConstructChip.id("consent_reply"));
        public static final StreamCodec<ByteBuf, ConsentReply> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ConsentReply::requestId,
                ByteBufCodecs.BOOL, ConsentReply::accept,
                ConsentReply::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
