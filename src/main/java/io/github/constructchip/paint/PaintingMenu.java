package io.github.constructchip.paint;

import io.github.constructchip.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One slot for a paintable implant, plus the player's inventory. The item comes back when you close it. */
public class PaintingMenu extends AbstractContainerMenu {
    public final Container input = new SimpleContainer(1) {
        @Override public void setChanged() { super.setChanged(); slotsChanged(this); }
    };

    public PaintingMenu(int id, Inventory inv) {
        super(ModMenus.PAINTING.get(), id);
        addSlot(new Slot(input, 0, 8, 18) {
            @Override public boolean mayPlace(ItemStack stack) { return !Paint.textures(stack).isEmpty(); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inv, 9 + row * 9 + col, 48 + col * 18, 178 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 48 + col * 18, 236));
    }

    public ItemStack implant() {
        return input.getItem(0);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (Paint.textures(stack).isEmpty() || !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player p) { return p.isAlive(); }

    @Override
    public void removed(Player p) {
        super.removed(p);
        clearContainer(p, input);
    }
}
