package io.github.constructchip.cyberware;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.network.Net;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Keeps everyone's view of a player's implants current. */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class ChromeVisualsEvents {
    private ChromeVisualsEvents() {}

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            Net.send(viewer, ChromeVisuals.payload(target));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) ChromeVisuals.broadcast(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) ChromeVisuals.broadcast(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) ChromeVisuals.broadcast(p);
    }
}
