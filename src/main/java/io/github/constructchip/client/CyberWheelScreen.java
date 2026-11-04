package io.github.constructchip.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.constructchip.cyberware.CyberEffects;
import io.github.constructchip.cyberware.CyberItems;
import io.github.constructchip.cyberware.CyberSlot;
import io.github.constructchip.cyberware.Implant;
import io.github.constructchip.network.CyberStatusPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The power wheel for players with chrome (constructs get the quickhack wheel). Hold G, point at an implant and
 * release (or click) to fire it: Sandevistan, optic scan, Gorilla Arms deploy, Blood Pump.
 */
public class CyberWheelScreen extends Screen {
    private record Entry(CyberSlot slot, Implant implant, CyberEffects.Action action) {}

    private static final int CELL = 30, CYAN = 0xFF5EF6FF;
    private final List<Entry> entries = new ArrayList<>();
    private int hovered = -1, lastHovered = -1;

    public CyberWheelScreen() {
        super(Component.translatable("gui.constructchip.wheel"));
        add(CyberSlot.OS, CyberEffects.Action.OS);
        add(CyberSlot.FACE, CyberEffects.Action.FACE);
        add(CyberSlot.ARMS, CyberEffects.Action.ARMS);
        add(CyberSlot.CIRCULATORY, CyberEffects.Action.CIRCULATORY);
    }

    private void add(CyberSlot slot, CyberEffects.Action action) {
        Implant imp = ClientCyber.at(slot);
        if (imp != null && (slot != CyberSlot.FACE || imp.hasScan())) entries.add(new Entry(slot, imp, action));
    }

    private int cx(int i) { return width / 2 + (int) (Math.cos(-Math.PI / 2 + i * 2 * Math.PI / entries.size()) * 60); }
    private int cy(int i) { return height / 2 + (int) (Math.sin(-Math.PI / 2 + i * 2 * Math.PI / entries.size()) * 60); }

    private int pick(double mx, double my) {
        double dx = mx - width / 2.0, dy = my - height / 2.0;
        if (entries.isEmpty() || dx * dx + dy * dy < 196) return -1;
        double step = 2 * Math.PI / entries.size();
        return Math.floorMod((int) Math.round((Math.atan2(dy, dx) + Math.PI / 2) / step), entries.size());
    }

    private void fire(int i) {
        UiSounds.blip();
        ClientCyber.activate(entries.get(i).action());
        onClose();
    }

    @Override
    public void tick() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (!InputConstants.isKeyDown(window, Keybinds.WHEEL.getKey().getValue())) {
            if (hovered >= 0) fire(hovered);
            else onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int i = pick(mx, my);
        if (button == 0 && i >= 0) {
            fire(i);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0x88041014, 0xAA02080A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        hovered = pick(mouseX, mouseY);
        if (hovered != lastHovered && hovered >= 0) UiSounds.hover();
        lastHovered = hovered;
        CyberStatusPayload s = ClientCyber.own();
        if (s == null || entries.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("message.constructchip.cyber.nothing_active"), width / 2, height / 2, CYAN);
            return;
        }
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            int x = cx(i) - CELL / 2, y = cy(i) - CELL / 2;
            long cd = s.cooldowns().get(e.slot().ordinal()), active = s.actives().get(e.slot().ordinal());
            boolean hot = i == hovered;
            g.fill(x, y, x + CELL, y + CELL, hot ? 0x5526C6D0 : 0xAA0A1418);
            g.renderOutline(x, y, CELL, CELL, hot ? CYAN : active > 0 ? 0xFFF3E600 : 0xAA2A6E78);
            g.renderItem(new ItemStack(CyberItems.IMPLANTS.get(e.implant()).get()), x + 7, y + 7);
            if (cd > 0 && active <= 0) {
                g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0x99000000);
                String t = String.valueOf((cd + 19) / 20);
                g.drawString(font, t, x + CELL - 2 - font.width(t), y + CELL - 9, 0xFFFFFFFF, true);
            }
        }
        if (hovered >= 0) {
            Entry e = entries.get(hovered);
            String name = Component.translatable(CyberItems.IMPLANTS.get(e.implant()).get().getDescriptionId()).getString().toUpperCase(Locale.ROOT);
            g.drawCenteredString(font, Component.literal(name).withStyle(st -> st.withFont(HudLayer.HUD_FONT)), width / 2, height / 2 - 4, CYAN);
        }
    }
}
