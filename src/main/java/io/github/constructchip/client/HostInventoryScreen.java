package io.github.constructchip.client;

import io.github.constructchip.menu.HostInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Plain panel for the read-only host inventory (no texture needed). */
public class HostInventoryScreen extends AbstractContainerScreen<HostInventoryMenu> {
    public HostInventoryScreen(HostInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 120;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE1A1F26);
        g.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF3FD0E0);
        for (Slot slot : menu.slots) {
            int sx = leftPos + slot.x - 1, sy = topPos + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF2C3440);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF12161C);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0xFF55FFFF, false);
        g.drawString(font, Component.translatable("container.constructchip.read_only"), 8, imageHeight - 12, 0xFF888888, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
