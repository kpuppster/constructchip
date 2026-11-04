package io.github.constructchip.capture;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.network.ConsentRequestPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import io.github.constructchip.network.Net;

/**
 * Pending "do you agree?" questions. A request is answered only by its target, only once, and only before it times
 * out. On accept, the handler re-checks everything (still seated, still in range, rules unchanged) before acting.
 * One open request per target: a new one declines the old one.
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class ConsentManager {
    public static final int DEFAULT_TIMEOUT_TICKS = 30 * 20;

    public interface Handler {
        /** Re-check everything at the moment of the answer. Return null if it's still valid, else the reason it isn't. */
        Component revalidate(MinecraftServer server);

        void accepted(MinecraftServer server);

        /** Declined, timed out, target left, or replaced by a newer request. */
        default void declined(MinecraftServer server, Component reason) {}
    }

    private record Pending(UUID target, long expiresAt, Handler handler) {}

    private static final Map<Integer, Pending> PENDING = new HashMap<>();
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private ConsentManager() {}

    public static int request(ServerPlayer target, Component title, Component body, int timeoutTicks, Handler handler) {
        MinecraftServer server = target.server;
        PENDING.entrySet().removeIf(e -> {
            if (!e.getValue().target().equals(target.getUUID())) return false;
            e.getValue().handler().declined(server, Component.translatable("message.constructchip.consent.replaced"));
            return true;
        });
        int id = NEXT_ID.getAndIncrement();
        PENDING.put(id, new Pending(target.getUUID(), server.getTickCount() + (long) timeoutTicks, handler));
        Net.send(target, new ConsentRequestPayload(id, title, body, timeoutTicks));
        return id;
    }

    public static boolean hasPending(UUID target) {
        return PENDING.values().stream().anyMatch(p -> p.target().equals(target));
    }

    public static void reply(ServerPlayer from, int id, boolean accept) {
        Pending pending = PENDING.get(id);
        if (pending == null || !pending.target().equals(from.getUUID())) return; // not theirs, or already answered
        PENDING.remove(id);
        MinecraftServer server = from.server;
        if (!accept) {
            pending.handler().declined(server, Component.translatable("message.constructchip.consent.declined", from.getGameProfile().getName()));
            return;
        }
        Component problem = pending.handler().revalidate(server);
        if (problem != null) {
            pending.handler().declined(server, problem);
            from.displayClientMessage(problem, true);
            return;
        }
        pending.handler().accepted(server);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        MinecraftServer server = event.getServer();
        long now = server.getTickCount();
        Iterator<Map.Entry<Integer, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Pending p = it.next().getValue();
            boolean gone = server.getPlayerList().getPlayer(p.target()) == null;
            if (gone || now > p.expiresAt()) {
                it.remove();
                p.handler().declined(server, Component.translatable(gone
                        ? "message.constructchip.consent.left" : "message.constructchip.consent.timeout"));
            }
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }
}
