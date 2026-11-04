package io.github.constructchip.tracking;

import com.mojang.serialization.Codec;
import io.github.constructchip.ConstructChip;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server-wide record of every captured chip, keyed by chipId. Stored in the overworld's data folder.
 * This is the single source of truth for "which stack is the real one" and "where is it".
 */
public final class ChipRegistry extends SavedData {
    private static final String NAME = ConstructChip.MODID + "_chips";
    private static final Codec<Map<UUID, ChipEntry>> MAP_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, ChipEntry.CODEC);
    /** Heartbeats that only move the timestamp are written to disk at most this often. */
    private static final long DIRTY_TIMESTAMP_INTERVAL = 200L;

    private static final SavedData.Factory<ChipRegistry> FACTORY =
            new SavedData.Factory<>(ChipRegistry::new, ChipRegistry::load, null);

    private final Map<UUID, ChipEntry> chips = new HashMap<>();
    private long lastTimestampFlush;

    public static ChipRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** Time base for lastSeenTick: always the overworld clock, so dimensions don't disagree. */
    public static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static ChipRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        ChipRegistry registry = new ChipRegistry();
        if (!tag.contains("chips")) return registry;
        MAP_CODEC.parse(NbtOps.INSTANCE, tag.get("chips"))
                .resultOrPartial(err -> ConstructChip.LOGGER.error("Failed to load chip registry: {}", err))
                .ifPresent(registry.chips::putAll);
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        MAP_CODEC.encodeStart(NbtOps.INSTANCE, chips)
                .resultOrPartial(err -> ConstructChip.LOGGER.error("Failed to save chip registry: {}", err))
                .ifPresent(nbt -> tag.put("chips", nbt));
        return tag;
    }

    public Optional<ChipEntry> get(UUID chipId) {
        return Optional.ofNullable(chips.get(chipId));
    }

    /** The chip a player is captured on, if any. */
    public Optional<UUID> chipOf(UUID capturedUuid) {
        for (Map.Entry<UUID, ChipEntry> e : chips.entrySet()) {
            if (e.getValue().capturedUuid().equals(capturedUuid)) return Optional.of(e.getKey());
        }
        return Optional.empty();
    }

    public Map<UUID, ChipEntry> all() {
        return Collections.unmodifiableMap(chips);
    }

    public void register(UUID chipId, ChipEntry entry) {
        chips.put(chipId, entry);
        setDirty();
    }

    public void remove(UUID chipId) {
        if (chips.remove(chipId) != null) setDirty();
    }

    /** Applies {@code change}; only marks dirty when something other than a recent timestamp changed. */
    public Optional<ChipEntry> update(UUID chipId, UnaryOperator<ChipEntry> change) {
        ChipEntry old = chips.get(chipId);
        if (old == null) return Optional.empty();
        ChipEntry next = change.apply(old);
        chips.put(chipId, next);
        boolean onlyTimestamp = old.serial() == next.serial() && old.location().samePlace(next.location());
        long seen = next.location().lastSeenTick();
        if (!onlyTimestamp || seen - lastTimestampFlush >= DIRTY_TIMESTAMP_INTERVAL) {
            lastTimestampFlush = seen;
            setDirty();
        }
        return Optional.of(next);
    }
}
