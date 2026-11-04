package io.github.constructchip.hack;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the client says it aimed at. Only a claim: the server resolves it in the host's level and re-checks
 * range and line of sight from the host's eyes.
 *
 * @param entityId network id (ENTITY), otherwise 0
 * @param pos      block position (BLOCK), otherwise {@link BlockPos#ZERO}
 * @param face     the block face that was aimed at (BLOCK); UP when unknown
 */
public record HackTarget(Kind kind, int entityId, BlockPos pos, Direction face) {
    public static final HackTarget NONE = new HackTarget(Kind.NONE, 0, BlockPos.ZERO, Direction.UP);

    public static final StreamCodec<ByteBuf, HackTarget> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(Kind::byId, Kind::ordinal), HackTarget::kind,
            ByteBufCodecs.VAR_INT, HackTarget::entityId,
            BlockPos.STREAM_CODEC, HackTarget::pos,
            Direction.STREAM_CODEC, HackTarget::face,
            HackTarget::new);

    public static HackTarget entity(int entityId) {
        return new HackTarget(Kind.ENTITY, entityId, BlockPos.ZERO, Direction.UP);
    }

    /** Block with an unknown face (command targets): UP. */
    public static HackTarget block(BlockPos pos) {
        return block(pos, Direction.UP);
    }

    /** @param face the side that was looked at (Signal Inject powers through it) */
    public static HackTarget block(BlockPos pos, Direction face) {
        return new HackTarget(Kind.BLOCK, 0, pos.immutable(), face);
    }

    public enum Kind {
        NONE, ENTITY, BLOCK;

        public static Kind byId(int id) {
            Kind[] values = values();
            return id >= 0 && id < values.length ? values[id] : NONE;
        }
    }
}
