package io.github.constructchip.client;

import io.github.constructchip.cyberware.CyberSlot;
import io.github.constructchip.network.CyberStatusPayload;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/** Chrome readouts: your load / cyberpsychosis (bottom-left), optic HP readout, the Phase V countdown. */
final class CyberHud {
    private static final String[] PHASES = {"", "I", "II", "III", "IV", "V"};

    private CyberHud() {}

    static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        CyberStatusPayload s = ClientCyber.own();
        if (s == null || (!s.any() && s.phase() == 0) || ClientChipState.isConstruct()) return;
        int x = 6, y = g.guiHeight() - 46;
        g.fill(x - 3, y - 3, x + 128, y + 38, 0x88000000);
        g.fill(x - 3, y - 3, x - 2, y + 38, 0xFF5EF6FF);
        boolean over = s.load() > s.capacity();
        g.drawString(font, hud(Component.translatable("hud.constructchip.chrome.load", s.load(), s.capacity())), x, y, over ? 0xFFFF4A4A : 0xFF5EF6FF);
        bar(g, x, y + 10, 100, s.load() / (float) Math.max(1, s.capacity()), over ? 0xFFFF4A4A : 0xFF3FD0E0);
        int pc = s.phase() >= 4 ? 0xFFFF2040 : s.phase() >= 2 ? 0xFFFFAA00 : 0xFFE8B4B4;
        Component psy = s.phase() > 0
                ? Component.translatable("hud.constructchip.chrome.phase", PHASES[s.phase()])
                : Component.translatable("hud.constructchip.chrome.stable");
        g.drawString(font, hud(psy), x, y + 16, pc);
        bar(g, x, y + 26, 100, s.pressure() / 1000f, 0xFFF90759);
        bar(g, x, y + 30, 100, s.degradation() / 1000f, 0xFF8E43E8);
        if (s.immunoLeft() > 0) g.drawString(font, hud(Component.translatable("hud.constructchip.chrome.immuno", s.immunoLeft() / 1200 + 1)), x + 70, y + 16, 0xFF5EF6FF);

        // cooldowns of actives, right of the panel
        int cx = x + 132;
        for (CyberSlot slot : new CyberSlot[]{CyberSlot.OS, CyberSlot.FACE, CyberSlot.ARMS, CyberSlot.CIRCULATORY}) {
            if (s.implants().get(slot.index).isEmpty()) continue;
            long active = s.actives().get(slot.ordinal()), cd = s.cooldowns().get(slot.ordinal());
            String label = Component.translatable(slot.key()).getString().substring(0, Math.min(4, Component.translatable(slot.key()).getString().length())).toUpperCase(Locale.ROOT);
            String state = slot == CyberSlot.ARMS ? (s.armsDeployed() ? "ON " + Math.round(s.fury()) : "OFF")
                    : active > 0 ? (active / 20) + "s" : cd > 0 ? "-" + (cd / 20) + "s" : "OK";
            g.drawString(font, hud(Component.literal(label + " " + state)), cx, y + 2, active > 0 || s.armsDeployed() && slot == CyberSlot.ARMS ? 0xFF5EF6FF : cd > 0 ? 0xFF888888 : 0xFFFFFFFF);
            y += 9;
        }

        // Phase V: the terminal window, front and centre
        if (s.terminalLeft() > 0) {
            Component t = hud(Component.translatable("hud.constructchip.chrome.terminal", s.terminalLeft() / 20));
            g.pose().pushPose();
            g.pose().scale(2f, 2f, 1f);
            g.drawCenteredString(font, t, g.guiWidth() / 4, g.guiHeight() / 8, 0xFFFF2040);
            g.pose().popPose();
        }

        // optics: HP of whatever you're aiming at
        if (ClientCyber.at(CyberSlot.FACE) != null && mc.crosshairPickEntity instanceof LivingEntity target) {
            String hp = String.format(Locale.ROOT, "%.0f / %.0f", target.getHealth(), target.getMaxHealth());
            g.drawCenteredString(font, hud(Component.literal(hp)), g.guiWidth() / 2, g.guiHeight() / 2 + 10, 0xFFFF4A4A);
        }
    }

    /** Compact host-chrome lines for the construct HUD (Chrome Link skill). Returns lines drawn. */
    static int renderHostLines(GuiGraphics g, int x, int y) {
        CyberStatusPayload h = ClientCyber.host();
        if (h == null || !h.any()) return 0;
        Font font = Minecraft.getInstance().font;
        g.drawString(font, hud(Component.translatable("hud.constructchip.chrome.host", h.load(), h.capacity(),
                h.phase() > 0 ? PHASES[h.phase()] : "-")), x, y, h.phase() >= 3 ? 0xFFFF4A4A : 0xFF5EF6FF);
        return 1;
    }

    private static void bar(GuiGraphics g, int x, int y, int w, float frac, int color) {
        g.fill(x, y, x + w, y + 3, 0xFF202830);
        g.fill(x, y, x + Math.round(w * Math.max(0, Math.min(1, frac))), y + 3, color);
    }

    private static Component hud(Component c) {
        return c.copy().withStyle(st -> st.withFont(HudLayer.HUD_FONT));
    }
}
