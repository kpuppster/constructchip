package io.github.constructchip.tracking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.StringRepresentable;

/**
 * Where a chip was last seen.
 *
 * @param state        what kind of place it is
 * @param holder       INSTALLED/CARRIED: the player (or entity) holding it. DROPPED: the chip ItemEntity's UUID
 * @param pos          last known position (dimension + block)
 * @param lastSeenTick overworld game time of the last heartbeat or hand-over
 */
public record ChipLocation(State state, Optional<UUID> holder, Optional<GlobalPos> pos, long lastSeenTick) {
    public static final Codec<ChipLocation> CODEC = RecordCodecBuilder.create(i -> i.group(
            State.CODEC.fieldOf("state").forGetter(ChipLocation::state),
            UUIDUtil.CODEC.optionalFieldOf("holder").forGetter(ChipLocation::holder),
            GlobalPos.CODEC.optionalFieldOf("pos").forGetter(ChipLocation::pos),
            Codec.LONG.fieldOf("last_seen").forGetter(ChipLocation::lastSeenTick)
    ).apply(i, ChipLocation::new));

    public static ChipLocation of(State state, UUID holder, GlobalPos pos, long now) {
        return new ChipLocation(state, Optional.ofNullable(holder), Optional.ofNullable(pos), now);
    }

    public boolean isHeldBy(UUID uuid) {
        return holder.isPresent() && holder.get().equals(uuid);
    }

    /** Same place as {@code other}, ignoring the timestamp (used to avoid needless setDirty calls). */
    public boolean samePlace(ChipLocation other) {
        return state == other.state && holder.equals(other.holder) && pos.equals(other.pos);
    }

    public ChipLocation seenAt(long now) {
        return new ChipLocation(state, holder, pos, now);
    }

    public ChipLocation asStored() {
        return new ChipLocation(State.STORED, Optional.empty(), pos, lastSeenTick);
    }

    public enum State implements StringRepresentable {
        /** Inside a host's head ({@code installedChip} attachment). */
        INSTALLED,
        /** On the ground as a chip ItemEntity. */
        DROPPED,
        /** In a player's (or entity's) inventory, not installed. */
        CARRIED,
        /** Not heartbeating: in a container, an unloaded chunk, a mob's hands, or destroyed without us seeing it. */
        STORED,
        /** Construct set free. Entries are deleted on release, so a missing entry also means released. */
        RELEASED;

        public static final Codec<State> CODEC = StringRepresentable.fromEnum(State::values);

        /** Construct is pinned to the chip (no abilities) in every state except INSTALLED. */
        public boolean anchorsConstruct() {
            return this != INSTALLED && this != RELEASED;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
