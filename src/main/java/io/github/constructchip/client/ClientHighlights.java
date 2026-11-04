package io.github.constructchip.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Entities outlined on this client only (MinecraftMixin asks this). Keyed by network id, with an expiry tick. */
public final class ClientHighlights {
    private static final Map<Integer, Long> UNTIL = new HashMap<>();

    private ClientHighlights() {}

    static void add(List<Integer> ids, int ticks) {
        long until = ClientChipState.now() + ticks;
        for (int id : ids) UNTIL.put(id, until);
    }

    public static boolean isHighlighted(Entity entity) {
        if (UNTIL.isEmpty()) return false;
        Long until = UNTIL.get(entity.getId());
        if (until == null) return false;
        if (ClientChipState.now() > until) {
            UNTIL.remove(entity.getId());
            return false;
        }
        return true;
    }

    static void clear() { UNTIL.clear(); }

    static boolean anyActive() { return !UNTIL.isEmpty() && Minecraft.getInstance().level != null; }
}
