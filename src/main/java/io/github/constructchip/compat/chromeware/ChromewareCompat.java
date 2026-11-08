package io.github.constructchip.compat.chromeware;

import io.github.constructchip.ConstructChip;
import net.minecraft.network.chat.Component;
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
 * This class never touches Chromeware's types itself; {@link ChromewareHooks} does, and is only loaded when Chromeware
 * is installed. If a call fails (a Chromeware update changed something at runtime), the compat turns itself off with
 * one warning and our hacks keep working without it.
 */
public final class ChromewareCompat {
    public static final String MOD_ID = "chromeware";
    public static final boolean LOADED = ModList.get().isLoaded(MOD_ID);
    private static volatile boolean broken;

    private ChromewareCompat() {}

    /** True if Chromeware is installed and its hooks haven't failed. */
    public static boolean available() {
        return LOADED && !broken;
    }

    /** Does this entity carry Chromeware chrome? Players with any implant capacity used, or Chromeware's mechanical mobs. */
    public static boolean chromed(LivingEntity e) {
        if (!available()) return false;
        try {
            return ChromewareHooks.chromed(e);
        } catch (RuntimeException | LinkageError t) {
            fail(t);
            return false;
        }
    }

    /** Short Circuit / System Lock on a chromed target, cast as the host. True if Chromeware's quickhack landed. */
    public static boolean hack(Player host, LivingEntity target, boolean lock) {
        if (!available() || !chromed(target)) return false;
        try {
            return ChromewareHooks.hack(host, target, lock);
        } catch (RuntimeException | LinkageError t) {
            fail(t);
            return false;
        }
    }

    /** Chrome Override: fire the host's first installed ability. Returns its name, or null if they have none. */
    public static Component override(Player host) {
        if (!available()) return null;
        try {
            return ChromewareHooks.override(host);
        } catch (RuntimeException | LinkageError t) {
            fail(t);
            return null;
        }
    }

    private static void fail(Throwable t) {
        if (!broken) ConstructChip.LOGGER.warn("Chromeware compat call failed (version changed?); turning it off", t);
        broken = true;
    }
}
