package io.github.constructchip.client;

import io.github.constructchip.network.PossessionPayload;
import io.github.constructchip.possession.Possession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Client view of running Takeovers: a possessing construct looks like (skin) and is named as its host; HUD timers. */
public final class ClientPossession {
    private ClientPossession() {}

    private static Map<UUID, PossessionPayload.Entry> byConstruct = Map.of();

    static void set(PossessionPayload payload) {
        Map<UUID, PossessionPayload.Entry> m = new HashMap<>();
        Map<UUID, UUID> pairs = new HashMap<>();
        for (PossessionPayload.Entry e : payload.entries()) {
            m.put(e.construct(), e);
            pairs.put(e.construct(), e.host());
        }
        byConstruct = Map.copyOf(m);
        Possession.clientPairs = Map.copyOf(pairs);
    }

    static void clear() {
        byConstruct = Map.of();
        Possession.clientPairs = Map.of();
    }

    private static PlayerInfo hostInfo(UUID construct) {
        UUID host = Possession.clientPairs.get(construct);
        var conn = Minecraft.getInstance().getConnection();
        return host == null || conn == null ? null : conn.getPlayerInfo(host);
    }

    /** For the AbstractClientPlayer mixin: the host's skin for a possessing construct, else null. */
    public static PlayerSkin skinFor(UUID player) {
        PlayerInfo info = hostInfo(player);
        return info == null ? null : info.getSkin();
    }

    /** For the Player display-name mixin (installed as Possession.clientBodyName). */
    static Component bodyName(Player p) {
        PlayerInfo info = hostInfo(p.getUUID());
        return info == null ? null : Component.literal(info.getProfile().getName());
    }

    private static String clock(long ticks) {
        long s = Math.max(0, ticks) / 20;
        return String.format(java.util.Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    static void renderHud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || byConstruct.isEmpty()) return;
        long now = mc.level.getGameTime();
        UUID me = mc.player.getUUID();
        Component line = null;
        PossessionPayload.Entry mine = byConstruct.get(me);
        if (mine != null) {
            line = Component.translatable("hud.constructchip.takeover", clock(mine.endsAt() - now));
        } else {
            for (PossessionPayload.Entry e : byConstruct.values()) {
                if (!e.host().equals(me)) continue;
                PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(e.construct());
                String who = info == null ? "?" : info.getProfile().getName();
                line = Component.translatable("hud.constructchip.takeover_host", who, clock(e.endsAt() - now));
            }
        }
        if (line == null) return;
        Component styled = line.copy().withStyle(s -> s.withFont(HudLayer.HUD_FONT));
        int w = mc.font.width(styled);
        int x = (g.guiWidth() - w) / 2, y = 8;
        g.fill(x - 6, y - 3, x + w + 6, y + 11, 0xAA1A0A0E);
        g.renderOutline(x - 6, y - 3, w + 12, 14, 0xFFFF5F55);
        g.drawString(mc.font, styled, x, y, 0xFFFF5F55, false);
    }
}
