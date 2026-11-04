package io.github.constructchip.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.constructchip.chip.ChipTimers;
import io.github.constructchip.hack.ModQuickhacks;
import io.github.constructchip.hack.Quickhack;
import io.github.constructchip.network.ChipStatusPayload;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * The construct's power wheel (inspired by the Capri mod's wheel). Hold the wheel key: unlocked quickhacks fan out
 * in a ring around the cursor. Point at one and release to select it; left-click to fire it right away.
 * Same red/cyan menu style as the consent screen. More than {@link #PER_PAGE} quickhacks are split into pages:
 * scroll the mouse wheel to turn them.
 */
public class PowerWheelScreen extends Screen {
    private static final int RED = 0xFFFF4A4A, CYAN = 0xFF5EF6FF, CELL = 26;
    /** Most quickhacks on the ring at once; more go on further pages so cells never overlap. */
    static final int PER_PAGE = 8;
    private final List<ResourceLocation> all;
    private List<ResourceLocation> hacks;
    private int page;
    private int hovered = -1;
    private int lastHovered = -1;

    public PowerWheelScreen() {
        super(Component.translatable("gui.constructchip.wheel"));
        this.all = ClientChipState.unlockedHacks();
        int sel = all.indexOf(ClientChipState.selectedHack());
        this.page = sel < 0 ? 0 : sel / PER_PAGE; // open on the page with the selected hack
        showPage();
    }

    private int pages() {
        return Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private void showPage() {
        int from = page * PER_PAGE;
        hacks = all.subList(Math.min(from, all.size()), Math.min(from + PER_PAGE, all.size()));
        hovered = -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && pages() > 1) {
            page = Math.floorMod(page + (scrollY < 0 ? 1 : -1), pages());
            showPage();
            UiSounds.hover();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private double radius() {
        return Math.max(48, Math.min(height * 0.32, 18 + hacks.size() * 9));
    }

    private int cellX(int i) {
        double a = -Math.PI / 2 + i * (Math.PI * 2 / hacks.size());
        return width / 2 + (int) Math.round(Math.cos(a) * radius());
    }

    private int cellY(int i) {
        double a = -Math.PI / 2 + i * (Math.PI * 2 / hacks.size());
        return height / 2 + (int) Math.round(Math.sin(a) * radius());
    }

    private int pick(double mx, double my) {
        double dx = mx - width / 2.0, dy = my - height / 2.0;
        if (dx * dx + dy * dy < 14 * 14 || hacks.isEmpty()) return -1; // dead zone in the middle
        double a = Math.atan2(dy, dx) + Math.PI / 2;
        double step = Math.PI * 2 / hacks.size();
        return Math.floorMod((int) Math.round(a / step), hacks.size());
    }

    @Override
    public void tick() {
        // released the key: keep the pointed-at hack selected and close
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (!InputConstants.isKeyDown(window, Keybinds.WHEEL.getKey().getValue())) {
            if (hovered >= 0) {
                ClientChipState.selectHack(hacks.get(hovered));
                UiSounds.blip();
            }
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int i = pick(mx, my);
        if (button == 0 && i >= 0 && minecraft != null && minecraft.player != null) {
            ClientChipState.selectHack(hacks.get(i));
            onClose();
            ClientInput.send(minecraft.player, hacks.get(i));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0x88100408, 0xAA0A0206);
    }

    private static MutableComponent hud(Component c) {
        return c.copy().withStyle(s -> s.withFont(HudLayer.HUD_FONT));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        hovered = pick(mouseX, mouseY);
        if (hovered != lastHovered && hovered >= 0) UiSounds.hover();
        lastHovered = hovered;

        ChipStatusPayload chip = ClientChipState.chip();
        long now = ClientChipState.now();
        ChipTimers timers = chip.timers();
        float ram = chip.ram().current(now);
        ResourceLocation selected = ClientChipState.selectedHack();

        // ring guide
        int cx = width / 2, cy = height / 2;
        double r = radius();
        for (int k = 0; k < 64; k++) {
            double a = k * Math.PI * 2 / 64;
            int px = cx + (int) (Math.cos(a) * r), py = cy + (int) (Math.sin(a) * r);
            g.fill(px, py, px + 1, py + 1, 0x669B1C28);
        }

        for (int i = 0; i < hacks.size(); i++) {
            ResourceLocation id = hacks.get(i);
            Quickhack hack = ModQuickhacks.REGISTRY.get(id);
            float cost = hack == null ? 0 : hack.baseRamCost() * chip.ramCostMultiplier() * (timers.isOverclocked(now) ? 0.5f : 1f);
            long cd = timers.cooldownRemaining(id, now);
            boolean ready = cd <= 0 && ram >= cost && !timers.isLockedOut(now);
            int x = cellX(i) - CELL / 2, y = cellY(i) - CELL / 2;
            boolean hot = i == hovered;
            g.fill(x, y, x + CELL, y + CELL, hot ? 0x5526C6D0 : 0xAA1A0A0E);
            g.renderOutline(x, y, CELL, CELL, hot ? CYAN : (id.equals(selected) ? 0xFFF3E600 : 0xAA9B1C28));
            g.renderItem(ClientChipState.iconFor(id), x + 5, y + 5);
            if (!ready) g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0x99000000);
            if (cd > 0) {
                String s = String.format(Locale.ROOT, "%.0f", Math.ceil(cd / 20.0));
                g.drawString(font, s, x + CELL - 2 - font.width(s), y + CELL - 9, 0xFFFFFFFF, true);
            }
        }

        // centre readout for the pointed-at (or selected) hack
        ResourceLocation focus = hovered >= 0 ? hacks.get(hovered) : selected;
        if (focus != null) {
            Quickhack hack = ModQuickhacks.REGISTRY.get(focus);
            MutableComponent name = hud(Component.literal(HudLayer.hackName(focus).getString().toUpperCase(Locale.ROOT)));
            g.drawCenteredString(font, name, cx, cy - 10, hovered >= 0 ? CYAN : RED);
            if (hack != null) {
                float cost = hack.baseRamCost() * chip.ramCostMultiplier() * (timers.isOverclocked(now) ? 0.5f : 1f);
                g.drawCenteredString(font, hud(Component.translatable("gui.constructchip.wheel.cost",
                        String.format(Locale.ROOT, "%.1f", cost), String.format(Locale.ROOT, "%.1f", ram))), cx, cy + 2, 0xFFE8B4B4);
            }
        }
        g.drawCenteredString(font, hud(Component.translatable("gui.constructchip.wheel.hint")), cx, (int) (cy + r + CELL), 0xAAE8B4B4);
        if (pages() > 1) {
            g.drawCenteredString(font, hud(Component.translatable("gui.constructchip.wheel.page", page + 1, pages())),
                    cx, (int) (cy + r + CELL + 12), CYAN);
        }
        if (timers.isLockedOut(now)) {
            g.drawCenteredString(font, hud(Component.translatable("hud.constructchip.lockout", (timers.lockoutUntil() - now) / 20)),
                    cx, (int) (cy - r - CELL), RED);
        }
    }
}
