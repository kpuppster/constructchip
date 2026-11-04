package io.github.constructchip.cyberware;

import io.github.constructchip.network.CyberStatusPayload;
import io.github.constructchip.network.Net;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/** Pushes a player's cyberware state to their own client (HUD, keys) and to their construct (host chrome readout). */
public final class CyberSync {
    private CyberSync() {}

    public static CyberStatusPayload snapshot(ServerPlayer p) {
        CyberState s = Cyberware.state(p);
        long now = p.level().getGameTime();
        List<String> implants = new ArrayList<>();
        List<Integer> levels = new ArrayList<>();
        for (int i = 0; i < CyberSlot.POSITIONS; i++) {
            implants.add(s.implantAt(i).map(Implant::id).orElse(""));
            levels.add(CyberwareItem.upgrade(s.at(i)));
        }
        List<Long> cooldowns = new ArrayList<>(), actives = new ArrayList<>();
        for (CyberSlot slot : CyberSlot.values()) {
            cooldowns.add(s.cooldown(slot.name(), now));
            actives.add(Math.max(0L, s.activeUntil.getOrDefault(slot.name(), 0L) - now));
        }
        return new CyberStatusPayload(implants, levels, Cyberware.load(p), Cyberware.capacity(p), s.pressure, s.degradation,
                s.phase, Math.max(0, s.terminalUntil - now), cooldowns, actives, s.armsDeployed, s.fury,
                Math.max(0, s.immunoUntil - now), Cyberware.suppressed(p));
    }

    public static void send(ServerPlayer p) {
        CyberStatusPayload payload = snapshot(p);
        Net.send(p, payload.forSelf());
    }
}
