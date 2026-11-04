package io.github.constructchip.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.GameType;

/**
 * Attached to the CAPTURED player.
 *
 * @param chipId               the chip they live on; empty means they are not a construct
 * @param gameModeBeforeAnchor the game mode to restore when an anchor (spectator camera) ends. Step 4 replaces this
 *                             with the full pre-capture snapshot (inventory, XP, game mode, position)
 */
public record ConstructState(Optional<UUID> chipId, Optional<GameType> gameModeBeforeAnchor) {
    public static final ConstructState NONE = new ConstructState(Optional.empty(), Optional.empty());

    public static final Codec<ConstructState> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("chip_id").forGetter(ConstructState::chipId),
            GameType.CODEC.optionalFieldOf("game_mode_before_anchor").forGetter(ConstructState::gameModeBeforeAnchor)
    ).apply(i, ConstructState::new));

    public static ConstructState of(UUID chipId) {
        return new ConstructState(Optional.of(chipId), Optional.empty());
    }

    public boolean isConstruct() {
        return chipId.isPresent();
    }

    public boolean isAnchored() {
        return gameModeBeforeAnchor.isPresent();
    }

    public ConstructState anchored(GameType previous) {
        return new ConstructState(chipId, Optional.of(previous));
    }

    public ConstructState unanchored() {
        return new ConstructState(chipId, Optional.empty());
    }
}
