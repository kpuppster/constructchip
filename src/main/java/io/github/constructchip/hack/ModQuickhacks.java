package io.github.constructchip.hack;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.hack.builtin.AdrenalSurgeHack;
import io.github.constructchip.hack.builtin.CyberwareHacks;
import io.github.constructchip.hack.builtin.DeviceHacks;
import io.github.constructchip.hack.builtin.LightningHacks;
import io.github.constructchip.hack.builtin.OverclockHack;
import io.github.constructchip.hack.builtin.PatchJobHack;
import io.github.constructchip.hack.builtin.PingHack;
import io.github.constructchip.hack.builtin.WeakPointHack;
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
    public static final DeferredHolder<Quickhack, OverclockHack> OVERCLOCK = REGISTER.register("overclock", OverclockHack::new);
    public static final DeferredHolder<Quickhack, PingHack> PING = REGISTER.register("ping", PingHack::new);
    public static final DeferredHolder<Quickhack, WeakPointHack> WEAK_POINT = REGISTER.register("weak_point", WeakPointHack::new);
    public static final DeferredHolder<Quickhack, CyberwareHacks.ShortCircuit> SHORT_CIRCUIT =
            REGISTER.register("short_circuit", CyberwareHacks.ShortCircuit::new);
    public static final DeferredHolder<Quickhack, CyberwareHacks.SystemLock> SYSTEM_LOCK =
            REGISTER.register("system_lock", CyberwareHacks.SystemLock::new);
    public static final DeferredHolder<Quickhack, LightningHacks.RedLightning> RED_LIGHTNING =
            REGISTER.register("red_lightning", LightningHacks.RedLightning::new);
    public static final DeferredHolder<Quickhack, LightningHacks.Storm> LIGHTNING_STORM =
            REGISTER.register("lightning_storm", LightningHacks.Storm::new);
    public static final DeferredHolder<Quickhack, PatchJobHack> PATCH_JOB = REGISTER.register("patch_job", PatchJobHack::new);
    public static final DeferredHolder<Quickhack, AdrenalSurgeHack> ADRENAL_SURGE = REGISTER.register("adrenal_surge", AdrenalSurgeHack::new);
    public static final DeferredHolder<Quickhack, DeviceHacks.RemoteSwitch> REMOTE_SWITCH =
            REGISTER.register("remote_switch", DeviceHacks.RemoteSwitch::new);
    public static final DeferredHolder<Quickhack, DeviceHacks.SignalInject> SIGNAL_INJECT =
            REGISTER.register("signal_inject", DeviceHacks.SignalInject::new);

    private ModQuickhacks() {}

    /** Mod bus. */
    public static void onNewRegistry(NewRegistryEvent event) {
        event.register(REGISTRY);
    }
}
