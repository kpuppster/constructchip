package io.github.constructchip.menu;

import io.github.constructchip.registry.ModMenus;
import io.github.constructchip.tracking.ChipTracker;
import io.github.constructchip.hack.QuickhackService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The construct's read-only window into the host's inventory. Every slot refuses both taking and placing, clicks do
 * nothing, and shift-click moves nothing. Live: it shows the host's actual inventory as it changes.
 * Layout (5 rows of 9): armor (head→feet) + offhand, then the 27 main slots, then the hotbar.
 */
public class HostInventoryMenu extends AbstractContainerMenu {
    public static final int SIZE = 41;
    private final Container container;
    private final ServerPlayer host; // null on the client

    /** Client constructor (contents arrive through normal slot sync). */
    public HostInventoryMenu(int id, Inventory viewerInventory) {
        this(id, new SimpleContainer(SIZE), null);
    }

    public HostInventoryMenu(int id, Container hostInventory, ServerPlayer host) {
        super(ModMenus.HOST_INVENTORY.get(), id);
        this.container = hostInventory;
        this.host = host;
        int left = 8, top = 18;
        // row 0: helmet, chest, legs, boots (inventory slots 39..36), gap, offhand (40)
        for (int i = 0; i < 4; i++) addSlot(new ReadOnlySlot(container, 39 - i, left + i * 18, top));
        addSlot(new ReadOnlySlot(container, 40, left + 8 * 18, top));
        // rows 1-3: main inventory 9..35
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new ReadOnlySlot(container, 9 + row * 9 + col, left + col * 18, top + 18 + row * 18));
        // row 4: hotbar 0..8
        for (int col = 0; col < 9; col++) addSlot(new ReadOnlySlot(container, col, left + col * 18, top + 4 * 18 + 4));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // read-only: ignore every click
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (host == null) return true; // client
        if (!(player instanceof ServerPlayer construct) || host.isRemoved()) return false;
        return ChipTracker.isConstruct(construct) && QuickhackService.hostOf(construct).map(h -> h == host).orElse(false);
    }

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean isHighlightable() { return true; }
    }
}
