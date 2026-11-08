package io.github.constructchip.possession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * A host's belongings that couldn't be handed back right away (the host was offline when a Takeover ended, or the
 * server crashed mid-takeover). Delivered at their next login.
 */
public final class PendingBodies extends SavedData {
    private static final String NAME = "constructchip_pending_bodies";
    private static final SavedData.Factory<PendingBodies> FACTORY = new SavedData.Factory<>(PendingBodies::new, PendingBodies::load, null);
    private final Map<UUID, List<ItemStack>> items = new HashMap<>();

    public static PendingBodies get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public void add(UUID host, List<ItemStack> stacks) {
        if (stacks.isEmpty()) return;
        items.computeIfAbsent(host, k -> new ArrayList<>()).addAll(stacks);
        setDirty();
    }

    public List<ItemStack> take(UUID host) {
        List<ItemStack> out = items.remove(host);
        if (out == null) return List.of();
        setDirty();
        return out;
    }

    private static PendingBodies load(CompoundTag tag, HolderLookup.Provider registries) {
        PendingBodies data = new PendingBodies();
        CompoundTag all = tag.getCompound("hosts");
        for (String key : all.getAllKeys()) {
            List<ItemStack> list = new ArrayList<>();
            for (Tag t : all.getList(key, Tag.TAG_COMPOUND)) ItemStack.parse(registries, t).ifPresent(list::add);
            data.items.put(UUID.fromString(key), list);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        items.forEach((host, list) -> {
            ListTag l = new ListTag();
            for (ItemStack s : list) if (!s.isEmpty()) l.add(s.save(registries));
            all.put(host.toString(), l);
        });
        tag.put("hosts", all);
        return tag;
    }
}
