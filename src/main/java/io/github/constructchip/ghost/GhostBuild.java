package io.github.constructchip.ghost;

import io.github.constructchip.ConstructChip;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A construct's body must never get in the way of building. Vanilla refuses a block placed where any player stands,
 * and the construct is invisible, untargetable and floats around its host, so the host's (or anyone's) blocks would
 * flash into place and vanish as soon as the construct drifted into that spot. Runs on both sides so the client's
 * placement prediction agrees with the server.
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class GhostBuild {
    private GhostBuild() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        boolean shouldBlock = !GhostState.isConstruct(p);
        if (p.blocksBuilding != shouldBlock) p.blocksBuilding = shouldBlock;
    }
}
