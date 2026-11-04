package io.github.constructchip.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    /** Bump when a payload's format changes; mismatched clients are refused at login. */
    private static final String VERSION = "8";

    private ModNetwork() {}

    /** Mod bus. */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(UseQuickhackPayload.TYPE, UseQuickhackPayload.STREAM_CODEC, UseQuickhackPayload::handle);
        registrar.playToServer(ServerboundPayloads.OpenHostInventory.TYPE, ServerboundPayloads.OpenHostInventory.STREAM_CODEC,
                ServerHandlers::openHostInventory);
        registrar.playToServer(ServerboundPayloads.UnlockSkill.TYPE, ServerboundPayloads.UnlockSkill.STREAM_CODEC,
                ServerHandlers::unlockSkill);
        registrar.playToServer(ServerboundPayloads.BuySkillPoint.TYPE, ServerboundPayloads.BuySkillPoint.STREAM_CODEC,
                ServerHandlers::buySkillPoint);
        registrar.playToServer(CyberPayloads.Activate.TYPE, CyberPayloads.Activate.STREAM_CODEC, CyberPayloads.Activate::handle);
        registrar.playToServer(CyberPayloads.Jump.TYPE, CyberPayloads.Jump.STREAM_CODEC, CyberPayloads.Jump::handle);
        registrar.playToClient(CyberStatusPayload.TYPE, CyberStatusPayload.STREAM_CODEC,
                (p, c) -> io.github.constructchip.client.ClientPayloadHandlers.cyberStatus(p, c));
        registrar.playToClient(ChromeVisualsPayload.TYPE, ChromeVisualsPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> io.github.constructchip.cyberware.ChromeVisuals.put(p.entityId(),
                        new io.github.constructchip.cyberware.ChromeVisuals.Visible(p.installed(), p.armsDeployed(),
                                p.sandevistanTicks() > 0 ? io.github.constructchip.client.ClientChipState.now() + p.sandevistanTicks() : 0L))));
        registrar.playToServer(PaintPayload.TYPE, PaintPayload.STREAM_CODEC, PaintPayload::handle);
        registrar.playToClient(BerserkPayload.TYPE, BerserkPayload.STREAM_CODEC,
                (p, c) -> io.github.constructchip.client.ClientPayloadHandlers.berserk(p, c));
        registrar.playToServer(ServerboundPayloads.ConsentReply.TYPE, ServerboundPayloads.ConsentReply.STREAM_CODEC,
                ServerHandlers::consentReply);

        // Client handlers sit behind lambdas so the client-only class is never loaded on a dedicated server.
        registrar.playToClient(ConstructLinksPayload.TYPE, ConstructLinksPayload.STREAM_CODEC,
                (p, c) -> io.github.constructchip.client.ClientPayloadHandlers.links(p, c));
        registrar.playToClient(ChipStatusPayload.TYPE, ChipStatusPayload.STREAM_CODEC,
                (p, c) -> io.github.constructchip.client.ClientPayloadHandlers.chipStatus(p, c));
        registrar.playToClient(HostStatusPayload.TYPE, HostStatusPayload.STREAM_CODEC,
                (p, c) -> io.github.constructchip.client.ClientPayloadHandlers.hostStatus(p, c));
        registrar.playToClient(HighlightPayload.TYPE, HighlightPayload.STREAM_CODEC,
                (p, c) -> io.github.constructchip.client.ClientPayloadHandlers.highlight(p, c));
        registrar.playToClient(GlitchPayload.TYPE, GlitchPayload.STREAM_CODEC,
                (p, c) -> io.github.constructchip.client.ClientPayloadHandlers.glitch(p, c));
        registrar.playToClient(ConsentRequestPayload.TYPE, ConsentRequestPayload.STREAM_CODEC,
                (p, c) -> io.github.constructchip.client.ClientPayloadHandlers.consent(p, c));
    }
}
