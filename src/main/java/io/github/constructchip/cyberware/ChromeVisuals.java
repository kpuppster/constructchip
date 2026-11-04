package io.github.constructchip.cyberware;

import io.github.constructchip.network.ChromeVisualsPayload;
import io.github.constructchip.network.Net;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Which implants a player visibly wears, for the 3D implant models. The server sends it to the player and everyone
 * nearby; clients keep it here (common class so the item's animation code can read it without client imports).
 */
public final class ChromeVisuals {
    /** @param sandevistanEnds client game time when the Sandevistan (if running) ends; 0 = not running */
    public record Visible(List<ItemStack> installed, boolean armsDeployed, long sandevistanEnds) {}

    private static final Map<Integer, Visible> CLIENT = new ConcurrentHashMap<>();

    private ChromeVisuals() {}

    public static Visible get(Entity e) {
        return CLIENT.get(e.getId());
    }

    public static boolean sandevistanActive(Entity e, long clientNow) {
        Visible v = get(e);
        return v != null && v.sandevistanEnds() > clientNow;
    }

    public static void put(int entityId, Visible v) {
        if (v.installed().stream().allMatch(ItemStack::isEmpty)) CLIENT.remove(entityId);
        else CLIENT.put(entityId, v);
    }

    public static void clear() {
        CLIENT.clear();
    }

    // ---------------------------------------------------------------- server

    public static ChromeVisualsPayload payload(ServerPlayer p) {
        CyberState s = Cyberware.state(p);
        boolean hide = Cyberware.suppressed(p) && io.github.constructchip.tracking.ChipTracker.isConstruct(p);
        List<ItemStack> list = hide ? java.util.Collections.nCopies(CyberSlot.POSITIONS, ItemStack.EMPTY)
                : s.installed.stream().map(ItemStack::copy).toList();
        long left = Math.max(0, s.activeUntil.getOrDefault(CyberSlot.OS.name(), 0L) - p.level().getGameTime());
        return new ChromeVisualsPayload(p.getId(), list, s.armsDeployed, hide ? 0 : (int) left);
    }

    /** To the player and every player within 128 blocks (plus anyone who starts tracking them later). */
    public static void broadcast(ServerPlayer p) {
        ChromeVisualsPayload payload = payload(p);
        for (ServerPlayer other : p.serverLevel().players()) {
            if (other == p || other.distanceToSqr(p) < 128 * 128) Net.send(other, payload);
        }
    }
}
