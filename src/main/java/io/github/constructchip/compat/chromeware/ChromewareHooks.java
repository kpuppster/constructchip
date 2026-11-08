package io.github.constructchip.compat.chromeware;

import com.chromeware.cyberware.CyberwareCapacity;
import com.chromeware.cyberware.ImplantAbility;
import com.chromeware.netrunning.Netrunning;
import com.chromeware.netrunning.Quickhack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The calls into Create: Chromeware, compiled against chromeware-1.2.1. Only ever loaded through
 * {@link ChromewareCompat}, which checks that Chromeware is installed first.
 */
final class ChromewareHooks {
    private ChromewareHooks() {}

    /** Order Chrome Override tries the host's abilities in. */
    private static final ImplantAbility[] OVERRIDE_ORDER = {ImplantAbility.SANDEVISTAN, ImplantAbility.BERSERK,
            ImplantAbility.BLOOD_PUMP, ImplantAbility.OPTICAL_CAMO, ImplantAbility.OVERCLOCK, ImplantAbility.ARM_IMPLANT};

    static boolean chromed(LivingEntity e) {
        if (e.getType().is(Netrunning.MECHANICAL)) return true;
        return e instanceof Player p && CyberwareCapacity.used(p) > 0;
    }

    static boolean hack(Player host, LivingEntity target, boolean lock) {
        return Netrunning.apply(host, target, lock ? Quickhack.CYBERWARE_MALFUNCTION : Quickhack.SHORT_CIRCUIT);
    }

    static Component override(Player host) {
        for (ImplantAbility ability : OVERRIDE_ORDER) {
            if (ability.installed(host)) {
                ability.activate(host);
                return ability.label(host);
            }
        }
        return null;
    }
}
