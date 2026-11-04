package io.github.constructchip.chat;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.hack.QuickhackService;
import io.github.constructchip.network.ConstructSayPayload;
import io.github.constructchip.network.Net;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * A construct installed in someone's head talks only to that host. Their chat message isn't broadcast: the host
 * gets it in the speech box (styled after Capri's). Nothing is echoed back to the construct. Constructs whose chip isn't installed in a host chat normally.
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class ConstructChat {
    private ConstructChat() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChat(ServerChatEvent event) {
        ServerPlayer construct = event.getPlayer();
        Optional<ServerPlayer> host = QuickhackService.hostOf(construct);
        if (host.isEmpty() || host.get() == construct) return;
        event.setCanceled(true);
        String text = event.getRawText();
        ServerPlayer h = host.get();
        String name = construct.getGameProfile().getName();
        if (h.connection != null && h.connection.hasChannel(ConstructSayPayload.TYPE)) {
            Net.send(h, new ConstructSayPayload(name, text));
        } else {
            h.sendSystemMessage(Component.translatable("chat.constructchip.from_construct", name, text).withStyle(ChatFormatting.RED));
        }
    }
}
