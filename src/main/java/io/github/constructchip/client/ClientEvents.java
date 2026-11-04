package io.github.constructchip.client;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.ghost.GhostState;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.util.TriState;

/**
 * A construct is drawn only for its host (and itself, in third person). Everyone else sees nothing: no body, no
 * name tag. This is client-side hiding. A modified client could still see them (accepted in the design).
 */
@EventBusSubscriber(modid = ConstructChip.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    /** True if {@code entity} is a construct this client must not see. */
    static boolean hiddenFromMe(Entity entity) {
        UUID host = GhostState.clientHostOf(entity.getUUID());
        if (host == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return true;
        return !mc.player.getUUID().equals(host) && !mc.player.getUUID().equals(entity.getUUID());
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (hiddenFromMe(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (hiddenFromMe(event.getEntity())) event.setCanRender(TriState.FALSE);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        GhostState.setClientLinks(java.util.Map.of());
        ClientChipState.setChip(io.github.constructchip.network.ChipStatusPayload.NONE);
        ClientHighlights.clear();
        io.github.constructchip.cyberware.ChromeVisuals.clear();
    }
}
