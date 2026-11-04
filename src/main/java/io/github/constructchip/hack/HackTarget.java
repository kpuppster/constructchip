package io.github.constructchip.hack;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the client says it aimed at. Only a claim: the server resolves it in the host's level and re-checks
 * range and line of sight from the host's eyes.
 *
 * @param entityId network id (ENTITY), otherwise 0
 * @param pos      block position (BLOCK), otherwise {@link BlockPos#ZERO}
 */
public record HackTarget(Kind kind, int entityId, BlockPos pos) {
    public static final HackTarget NONE = new HackTarget(Kind.NONE, 0, BlockPos.ZERO);

    public static final StreamCodec<ByteBuf, HackTarget> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(Kind::byId, Kind::ordinal), HackTarget::kind,
            ByteBufCodecs.VAR_INT, HackTarget::entityId,
            BlockPos.STREAM_CODEC, HackTarget::pos,
            HackTarget::new);

    public static HackTarget entity(int entityId) {
        return new HackTarget(Kind.ENTITY, entityId, BlockPos.ZERO);
    }

    public static HackTarget block(BlockPos pos) {
        return new HackTarget(Kind.BLOCK, 0, pos.immutable());
    }

    public enum Kind {
        NONE, ENTITY, BLOCK;

        public static Kind byId(int id) {
            Kind[] values = values();
            return id >= 0 && id < values.length ? values[id] : NONE;
        }
    }
}
