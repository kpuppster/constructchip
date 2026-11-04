package io.github.constructchip.client;

import io.github.constructchip.cyberware.CyberSlot;
import io.github.constructchip.cyberware.CyberwareItem;
import io.github.constructchip.cyberware.Implant;
import io.github.constructchip.cyberware.SurgeryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Ripperdoc surgery: body slots with labels, a load/capacity bar, per-implant and capacity upgrade buttons. */
public class SurgeryScreen extends AbstractContainerScreen<SurgeryMenu> {
    private static final String[] LABEL_KEYS = {"os", "cortex", "face", "arms", "circulatory", "legs", "skeleton", "skeleton"};
    private final Button[] upgrade = new Button[CyberSlot.POSITIONS];
    private Button capacity;

    public SurgeryScreen(SurgeryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = 230;
        inventoryLabelY = 137;
        inventoryLabelX = 30;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < CyberSlot.POSITIONS; i++) {
            Slot slot = menu.slots.get(i);
            final int id = i;
            upgrade[i] = addRenderableWidget(Button.builder(Component.literal("+"),
                    b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id))
                    .bounds(leftPos + slot.x + 18, topPos + slot.y, 12, 16).build());
        }
        capacity = addRenderableWidget(Button.builder(Component.translatable("gui.constructchip.surgery.capacity"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SurgeryMenu.CAPACITY_BUTTON))
                .bounds(leftPos + 154, topPos + 98, 58, 14).build());
    }

    @Override
    protected void containerTick() {
        for (int i = 0; i < CyberSlot.POSITIONS; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            int lvl = CyberwareItem.upgrade(stack);
            upgrade[i].visible = CyberwareItem.implantOf(stack).isPresent() && lvl < Implant.MAX_UPGRADE;
            upgrade[i].setTooltip(Tooltip.create(Component.translatable("gui.constructchip.surgery.upgrade", lvl + 1, 2, lvl + 1)));
        }
        int cl = menu.capacityLevel();
        capacity.visible = cl < 5;
        capacity.setTooltip(Tooltip.create(Component.translatable("gui.constructchip.surgery.capacity.tip", cl + 1, 3, cl + 1)));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF00A0608);
        g.fillGradient(leftPos, topPos, leftPos + imageWidth, topPos + 134, 0xC03A0C12, 0xC0240810);
        g.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xAA9B1C28);
        for (Slot slot : menu.slots) {
            int sx = leftPos + slot.x - 1, sy = topPos + slot.y - 1;
            boolean body = menu.slots.indexOf(slot) < CyberSlot.POSITIONS;
            g.fill(sx, sy, sx + 18, sy + 18, body ? 0xFF5EF6FF : 0xFF3A2228);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF120A0C);
        }
        // load bar
        int load = menu.load(), cap = Math.max(1, menu.capacity());
        int bx = leftPos + 8, by = topPos + 111, bw = 138;
        g.fill(bx, by, bx + bw, by + 6, 0xFF202830);
        g.fill(bx, by, bx + Math.min(bw, bw * load / cap), by + 6, load > cap ? 0xFFFF4A4A : 0xFF3FD0E0);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0xFFF3E600, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE8B4B4, false);
        for (int i = 0; i < CyberSlot.POSITIONS; i++) {
            Slot slot = menu.slots.get(i);
            String label = Component.translatable("cyberslot.constructchip." + LABEL_KEYS[i]).getString();
            int max = (i < 4 ? 112 : imageWidth - 4) - (slot.x + 32);
            if (font.width(label) > max) label = font.plainSubstrByWidth(label, max - font.width("…")) + "…";
            g.drawString(font, label, slot.x + 32, slot.y + 4, 0xFFFF4A4A, false);
        }
        int load = menu.load(), cap = menu.capacity();
        g.drawString(font, Component.translatable("gui.constructchip.surgery.load", load, cap), 8, 101, load > cap ? 0xFFFF4A4A : 0xFF5EF6FF, false);
        String[] ph = {"-", "I", "II", "III", "IV", "V"};
        String psyche = (load > cap ? Component.translatable("gui.constructchip.surgery.overload")
                : Component.translatable("gui.constructchip.surgery.psyche", ph[Math.min(5, menu.phase())], menu.pressure())).getString();
        if (font.width(psyche) > imageWidth - 16) psyche = font.plainSubstrByWidth(psyche, imageWidth - 16 - font.width("…")) + "…";
        g.drawString(font, psyche, 8, 121, load > cap ? 0xFFFF4A4A : 0xFFE8B4B4, false);
        g.drawString(font, Component.translatable("gui.constructchip.surgery.mult", menu.capacityLevel()), 156, 115, 0xFFAAAAAA, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
