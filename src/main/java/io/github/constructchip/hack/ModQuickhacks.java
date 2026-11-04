package io.github.constructchip.hack;

import io.github.constructchip.ConstructChip;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * Code-defined quickhacks (behaviour lives in Java; which ones a chip may use comes from the datapack skill tree via
 * {@code Skill#quickhack}). Synced so the client can list and name them.
 */
public final class ModQuickhacks {
    public static final ResourceKey<Registry<Quickhack>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ConstructChip.id("quickhack"));
    public static final Registry<Quickhack> REGISTRY = new RegistryBuilder<>(REGISTRY_KEY).sync(true).create();

    public static final DeferredRegister<Quickhack> REGISTER = DeferredRegister.create(REGISTRY, ConstructChip.MODID);

    public static final DeferredHolder<Quickhack, ScannerHack> SCANNER = REGISTER.register("scanner", ScannerHack::new);

    private ModQuickhacks() {}

    /** Mod bus. */
    public static void onNewRegistry(NewRegistryEvent event) {
        event.register(REGISTRY);
    }
}
