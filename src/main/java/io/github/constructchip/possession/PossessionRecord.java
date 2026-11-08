package io.github.constructchip.possession;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.GameType;

/**
 * Saved on both players while a Takeover runs, so a crash mid-takeover can be undone at the next login.
 * On the host: {@code construct} + the game mode to give back. On the construct: {@code host}.
 */
public record PossessionRecord(Optional<UUID> construct, Optional<UUID> host, Optional<GameType> previousMode) {
    public static final PossessionRecord NONE = new PossessionRecord(Optional.empty(), Optional.empty(), Optional.empty());
    public static final Codec<PossessionRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("construct").forGetter(PossessionRecord::construct),
            UUIDUtil.CODEC.optionalFieldOf("host").forGetter(PossessionRecord::host),
            GameType.CODEC.optionalFieldOf("previous_mode").forGetter(PossessionRecord::previousMode)
    ).apply(i, PossessionRecord::new));

    static PossessionRecord onHost(UUID construct, GameType previous) {
        return new PossessionRecord(Optional.of(construct), Optional.empty(), Optional.of(previous));
    }

    static PossessionRecord onConstruct(UUID host) {
        return new PossessionRecord(Optional.empty(), Optional.of(host), Optional.empty());
    }
}
