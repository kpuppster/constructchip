package io.github.constructchip.chip;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Identity of a captured construct.
 *
 * @param chipId       unique per capture. Dupe guard: only the stack whose chipId matches the registry entry
 *                     (and the captured player's attachment) is valid.
 * @param capturedUuid the captured player's UUID
 * @param capturedName the captured player's name at capture time (display only, never used for lookups)
 */
public record ChipIdentity(UUID chipId, UUID capturedUuid, String capturedName) {
    public static final Codec<ChipIdentity> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("chip_id").forGetter(ChipIdentity::chipId),
            UUIDUtil.CODEC.fieldOf("captured_uuid").forGetter(ChipIdentity::capturedUuid),
            Codec.STRING.fieldOf("captured_name").forGetter(ChipIdentity::capturedName)
    ).apply(i, ChipIdentity::new));

    public static final StreamCodec<ByteBuf, ChipIdentity> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ChipIdentity::chipId,
            UUIDUtil.STREAM_CODEC, ChipIdentity::capturedUuid,
            ByteBufCodecs.STRING_UTF8, ChipIdentity::capturedName,
            ChipIdentity::new);

    public static ChipIdentity fresh(UUID capturedUuid, String capturedName) {
        return new ChipIdentity(UUID.randomUUID(), capturedUuid, capturedName);
    }
}
