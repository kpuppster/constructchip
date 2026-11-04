package io.github.constructchip.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    /** Bump when a payload's format changes; mismatched clients are refused at login. */
    private static final String VERSION = "1";

    private ModNetwork() {}

    /** Mod bus. */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(UseQuickhackPayload.TYPE, UseQuickhackPayload.STREAM_CODEC, UseQuickhackPayload::handle);
    }
}
