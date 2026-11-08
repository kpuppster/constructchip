package io.github.constructchip.ghost;

import io.github.constructchip.attachment.ModAttachments;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * "Is this entity a construct?" on either side. The server reads the attachment; the client reads the map
 * from {@code ConstructLinksPayload} (construct UUID → host UUID, installed constructs only).
 */
public final class GhostState {
    /** Client mirror. Replaced wholesale on each sync. Empty on the server. */
    private static volatile Map<UUID, UUID> clientLinks = Map.of();

    private GhostState() {}

    public static void setClientLinks(Map<UUID, UUID> links) {
        clientLinks = Map.copyOf(links);
    }

    public static Map<UUID, UUID> clientLinks() {
        return clientLinks;
    }

    /** Host UUID of an installed construct, client side; null otherwise. */
    public static UUID clientHostOf(UUID construct) {
        return clientLinks.get(construct);
    }

    public static boolean isConstruct(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        if (player.level().isClientSide) return clientLinks.containsKey(player.getUUID());
        // a construct in its host's body (Takeover) is a normal player until it lets go
        return player.getData(ModAttachments.CONSTRUCT_STATE).isConstruct()
                && !io.github.constructchip.possession.Possession.isPossessing(player);
    }
}
