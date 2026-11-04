package io.github.constructchip.skill;

import io.github.constructchip.ConstructChip;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** Skills are a synced datapack registry, so packs (and compat conditions) can add, change or remove nodes. */
public final class ModSkills {
    public static final ResourceKey<Registry<Skill>> REGISTRY_KEY = ResourceKey.createRegistryKey(ConstructChip.id("skill"));

    private ModSkills() {}

    /** Mod bus. Network codec = the same codec, so the client has the tree for the skill screen. */
    public static void onNewDatapackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY_KEY, Skill.CODEC, Skill.CODEC);
    }

    public static Registry<Skill> get(RegistryAccess access) {
        return access.registryOrThrow(REGISTRY_KEY);
    }
}
