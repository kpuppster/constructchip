package io.github.constructchip.client;

import io.github.constructchip.ghost.GhostState;
import io.github.constructchip.network.ChipStatusPayload;
import io.github.constructchip.network.ConsentRequestPayload;
import io.github.constructchip.network.ConstructLinksPayload;
import io.github.constructchip.network.HighlightPayload;
import io.github.constructchip.network.HostStatusPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-side payload handling. Only ever loaded on the client (ModNetwork calls these from lambdas). */
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {}

    public static void links(ConstructLinksPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> GhostState.setClientLinks(payload.constructToHost()));
    }

    public static void chipStatus(ChipStatusPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientChipState.setChip(payload));
    }

    public static void hostStatus(HostStatusPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientChipState.setHost(payload));
    }

    public static void highlight(HighlightPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientHighlights.add(payload.entityIds(), payload.ticks()));
    }

    public static void glitch(io.github.constructchip.network.GlitchPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientGlitch.flash(payload.ticks(), payload.red(), payload.flash()));
    }

    public static void say(io.github.constructchip.network.ConstructSayPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ConstructTalk.say(payload.name(), payload.text()));
    }

    public static void bolt(io.github.constructchip.network.BoltFxPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CapriBolts.handle(payload));
    }

    public static void consent(ConsentRequestPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new ConsentScreen(payload)));
    }
}
