package io.github.constructchip.compat.chromeware;

import io.github.constructchip.ConstructChip;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * Optional compatibility with breenihilation's Create: Chromeware ({@code chromeware}), used with permission. Nothing
 * from that mod is included: our hacks call its own quickhacks and implant abilities when it is installed.
 *
 * <ul>
 *   <li>Short Circuit: Chromeware's Short Circuit (true damage that scales with the target's chrome, implants off for
 *       3 s, +3 s on its control quickhacks already on the target).</li>
 *   <li>System Lock: Chromeware's Cyberware Malfunction (chrome disabled for 10 s, +15% damage taken; a second one
 *       before it wears off disables the chrome until death, as in Chromeware).</li>
 *   <li>Chrome Override: fires one of the host's own implant abilities (Sandevistan first, then Berserk, Blood Pump,
 *       Optical Camo, Overclock, arm implant), with all of Chromeware's own checks and cooldowns.</li>
 * </ul>
 *
 * Chromeware needs Create, which our build doesn't load, so it's reached by name instead of compiled against. Every
 * name below was checked against chromeware-1.2.1. If a later version renames something, the hooks switch themselves
 * off with one warning in the log and our hacks keep working without them.
 */
public final class ChromewareCompat {
    public static final String MOD_ID = "chromeware";
    public static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    /** Order Chrome Override tries the host's abilities in. */
    private static final List<String> OVERRIDE_ORDER = List.of("SANDEVISTAN", "BERSERK", "BLOOD_PUMP", "OPTICAL_CAMO", "OVERCLOCK", "ARM_IMPLANT");
    /** Chromeware's tag for robots and the like ("mechanical" mobs count as chromed). */
    private static final TagKey<EntityType<?>> MECHANICAL = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "mechanical_targets"));

    private static boolean resolved, ok;
    private static MethodHandle apply;       // Netrunning.apply(Player, Entity, Quickhack) -> boolean
    private static MethodHandle capacityUsed; // CyberwareCapacity.used(Player) -> int
    private static Class<? extends Enum> quickhackType, abilityType;
    private static MethodHandle abilityInstalled, abilityActivate, abilityLabel;

    private ChromewareCompat() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static synchronized boolean ready() {
        if (resolved) return ok;
        resolved = true;
        if (!LOADED) return ok = false;
        try {
            MethodHandles.Lookup l = MethodHandles.publicLookup();
            ClassLoader cl = ChromewareCompat.class.getClassLoader();
            Class<?> netrunning = Class.forName("com.chromeware.netrunning.Netrunning", true, cl);
            quickhackType = (Class<? extends Enum>) Class.forName("com.chromeware.netrunning.Quickhack", true, cl);
            Class<?> capacity = Class.forName("com.chromeware.cyberware.CyberwareCapacity", true, cl);
            abilityType = (Class<? extends Enum>) Class.forName("com.chromeware.cyberware.ImplantAbility", true, cl);
            apply = l.findStatic(netrunning, "apply", MethodType.methodType(boolean.class, Player.class, Entity.class, quickhackType));
            capacityUsed = l.findStatic(capacity, "used", MethodType.methodType(int.class, Player.class));
            abilityInstalled = l.findVirtual(abilityType, "installed", MethodType.methodType(boolean.class, Player.class));
            abilityActivate = l.findVirtual(abilityType, "activate", MethodType.methodType(void.class, Player.class));
            abilityLabel = l.findVirtual(abilityType, "label", MethodType.methodType(Component.class, Player.class));
            Enum.valueOf(quickhackType, "SHORT_CIRCUIT");
            Enum.valueOf(quickhackType, "CYBERWARE_MALFUNCTION");
            return ok = true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            ConstructChip.LOGGER.warn("Chromeware is installed but its hooks didn't match (version changed?); Chromeware compat is off", e);
            return ok = false;
        }
    }

    /** True if Chromeware is installed and its hooks resolved. */
    public static boolean available() {
        return ready();
    }

    /** Does this entity carry Chromeware chrome? Players with any implant capacity used, or Chromeware's mechanical mobs. */
    public static boolean chromed(LivingEntity e) {
        if (!ready()) return false;
        if (e.getType().is(MECHANICAL)) return true;
        if (!(e instanceof Player p)) return false;
        try {
            return (int) capacityUsed.invoke(p) > 0;
        } catch (Throwable t) {
            fail(t);
            return false;
        }
    }

    /**
     * Short Circuit / System Lock on a chromed target, cast as the host. Returns true if Chromeware's quickhack landed.
     * Does nothing (false) for targets without Chromeware chrome.
     */
    @SuppressWarnings("unchecked")
    public static boolean hack(Player host, LivingEntity target, boolean lock) {
        if (!ready() || !chromed(target)) return false;
        try {
            Object hack = Enum.valueOf(quickhackType, lock ? "CYBERWARE_MALFUNCTION" : "SHORT_CIRCUIT");
            return (boolean) apply.invoke(host, (Entity) target, hack);
        } catch (Throwable t) {
            fail(t);
            return false;
        }
    }

    /** Chrome Override: fire the host's first installed ability. Returns its name, or null if they have none. */
    @SuppressWarnings("unchecked")
    public static Component override(Player host) {
        if (!ready()) return null;
        try {
            for (String name : OVERRIDE_ORDER) {
                Object ability = Enum.valueOf(abilityType, name);
                if ((boolean) abilityInstalled.invoke(ability, host)) {
                    abilityActivate.invoke(ability, host);
                    return (Component) abilityLabel.invoke(ability, host);
                }
            }
            return null;
        } catch (Throwable t) {
            fail(t);
            return null;
        }
    }

    private static void fail(Throwable t) {
        ConstructChip.LOGGER.warn("Chromeware compat call failed; turning it off", t);
        ok = false;
    }
}
