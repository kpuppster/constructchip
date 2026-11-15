package io.github.constructchip.compat.securitycraft;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.hack.HackResult;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Optional compatibility with SecurityCraft ({@code securitycraft}, MIT). Nothing from it is included; our hacks use
 * its public API when it is installed:
 * <ul>
 *   <li>Short Circuit / System Lock on a Sentry shut it down for 10 / 30 s (it stops shooting).</li>
 *   <li>Remote Switch on a Security Camera or Sonic Security System shuts it down for 20 s (again: switches it back
 *       on); on a mine it defuses it (again: arms it); on a keypad, keypad door, keypad chest or furnace, key panel,
 *       display case or keycard reader it cracks the lock like a Codebreaker, as the host.</li>
 * </ul>
 * Hacking SecurityCraft devices is the point of these hacks, so it works on other players' devices too. This class
 * never touches SecurityCraft's types; {@link SecurityCraftHooks} does, only once the mod is known to be loaded.
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class SecurityCraftCompat {
    public static final String MOD_ID = "securitycraft";
    public static final boolean LOADED = ModList.get().isLoaded(MOD_ID);
    public static final int DEVICE_SHUTDOWN_TICKS = 20 * 20;
    private static volatile boolean broken;

    private record Pending(ServerLevel level, long at, Runnable action) {}
    private static final List<Pending> PENDING = new ArrayList<>();

    private SecurityCraftCompat() {}

    private static boolean available() {
        return LOADED && !broken;
    }

    /** Short Circuit / System Lock: true if the target was a SecurityCraft machine and is now shut down. */
    public static boolean shutDown(Entity target, int ticks) {
        if (!available() || target.level().isClientSide) return false;
        try {
            return SecurityCraftHooks.shutDownEntity(target, ticks);
        } catch (RuntimeException | LinkageError t) {
            fail(t);
            return false;
        }
    }

    /** Remote Switch: the result for a SecurityCraft device, or null if the block isn't one. */
    public static HackResult remoteSwitch(ServerLevel level, BlockPos pos, ServerPlayer host) {
        if (!available()) return null;
        try {
            return SecurityCraftHooks.remoteSwitch(level, pos, host, DEVICE_SHUTDOWN_TICKS);
        } catch (RuntimeException | LinkageError t) {
            fail(t);
            return null;
        }
    }

    static void later(ServerLevel level, int ticks, Runnable action) {
        synchronized (PENDING) {
            PENDING.add(new Pending(level, level.getGameTime() + ticks, action));
        }
    }

    /** Devices come back on when their shutdown runs out. */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        List<Runnable> due = new ArrayList<>();
        synchronized (PENDING) {
            for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
                Pending p = it.next();
                if (p.level().getGameTime() >= p.at()) {
                    due.add(p.action());
                    it.remove();
                }
            }
        }
        for (Runnable r : due) {
            try {
                r.run();
            } catch (RuntimeException | LinkageError t) {
                fail(t);
            }
        }
    }

    /** Switch everything we shut down back on before the world saves, so nothing stays off for good. */
    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        List<Runnable> all;
        synchronized (PENDING) {
            all = PENDING.stream().map(Pending::action).toList();
            PENDING.clear();
        }
        for (Runnable r : all) {
            try {
                r.run();
            } catch (RuntimeException | LinkageError ignored) {
                // shutting down anyway
            }
        }
    }

    private static void fail(Throwable t) {
        if (!broken) ConstructChip.LOGGER.warn("SecurityCraft compat call failed (version changed?); turning it off", t);
        broken = true;
    }
}
