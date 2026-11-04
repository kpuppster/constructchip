package io.github.constructchip.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server → client sends that skip players whose connection doesn't know the payload (vanilla clients, fake players).
 * Sending an unknown payload would otherwise throw.
 */
public final class Net {
    private Net() {}

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player instanceof FakePlayer) return;
        if (player.connection != null && player.connection.hasChannel(payload.type())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static void sendToAll(MinecraftServer server, CustomPacketPayload payload) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) send(player, payload);
    }
}
