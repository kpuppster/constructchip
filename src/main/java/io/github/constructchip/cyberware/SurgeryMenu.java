package io.github.constructchip.cyberware;

import io.github.constructchip.registry.ModMenus;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Ripperdoc surgery (sneak + right-click the Soul Killer station). Drag implants into body slots; changes apply when
 * the menu closes. Buttons 0-7 upgrade the implant in that slot (costs 2 Internal Parts of the next tier);
 * button 8 upgrades capacity (3 Internal Parts of the next tier).
 * Data slots: 0 load, 1 capacity, 2 pressure, 3 phase, 4 capacity level.
 */
public class SurgeryMenu extends AbstractContainerMenu {
    public static final int CAPACITY_BUTTON = 8;
    final Container implants;
    final ContainerData data;
    private final Player player;

    public SurgeryMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(CyberSlot.POSITIONS), new SimpleContainerData(5));
    }

    public SurgeryMenu(int id, Inventory inv, Container implants, ContainerData data) {
        super(ModMenus.SURGERY.get(), id);
        this.implants = implants;
        this.data = data;
        this.player = inv.player;
        // body slots: two columns on the left
        int[][] pos = {{8, 20}, {8, 40}, {8, 60}, {8, 80}, {114, 20}, {114, 40}, {114, 60}, {114, 80}};
        for (int i = 0; i < CyberSlot.POSITIONS; i++) {
            final int p = i;
            addSlot(new Slot(implants, i, pos[i][0], pos[i][1]) {
                @Override public boolean mayPlace(ItemStack stack) { return fits(stack, p); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inv, 9 + row * 9 + col, 30 + col * 18, 148 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 30 + col * 18, 206));
        addDataSlots(data);
    }

    boolean fits(ItemStack stack, int pos) {
        Optional<Implant> imp = CyberwareItem.implantOf(stack);
        if (imp.isEmpty() || imp.get().slot != CyberSlot.ofPosition(pos)) return false;
        if (imp.get().slot == CyberSlot.SKELETON) {
            int other = pos == 6 ? 7 : 6;
            Optional<Implant> o = CyberwareItem.implantOf(implants.getItem(other));
            if (o.isPresent() && imp.get().conflictsWith(o.get())) return false;
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < CyberSlot.POSITIONS) {
            if (!moveItemStackTo(stack, CyberSlot.POSITIONS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < CyberSlot.POSITIONS && !moved; i++) {
                if (!slots.get(i).hasItem() && fits(stack, i)) moved = moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp)) return false;
        CyberState s = Cyberware.state(sp);
        if (id == CAPACITY_BUTTON) {
            if (s.capacityLevel >= 5) return false;
            if (!pay(sp, s.capacityLevel + 1, 3)) return false;
            s.capacityLevel++;
        } else if (id >= 0 && id < CyberSlot.POSITIONS) {
            ItemStack stack = implants.getItem(id);
            if (CyberwareItem.implantOf(stack).isEmpty()) return false;
            int lvl = CyberwareItem.upgrade(stack);
            if (lvl >= Implant.MAX_UPGRADE || !pay(sp, lvl + 1, 2)) return false;
            stack.set(io.github.constructchip.registry.ModDataComponents.CYBER_UPGRADE.get(), lvl + 1);
            implants.setChanged();
        } else {
            return false;
        }
        CyberEffects.play(sp, "er_postripperdoc", 0.6f);
        return true;
    }

    /** Takes {@code count} Internal Parts of {@code tier} from the player's inventory (creative: free). */
    private static boolean pay(ServerPlayer p, int tier, int count) {
        if (p.getAbilities().instabuild) return true;
        var item = CyberItems.PARTS.get(Part.internalParts(tier)).get();
        if (p.getInventory().countItem(item) < count) {
            p.displayClientMessage(Component.translatable("message.constructchip.surgery.cost", count,
                    Component.translatable(item.getDescriptionId())), true);
            return false;
        }
        int left = count;
        for (int i = 0; i < p.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player p) {
        return p.isAlive();
    }

    /** Server: write the body slots back into the player and apply. */
    @Override
    public void removed(Player p) {
        super.removed(p);
        if (!(p instanceof ServerPlayer sp)) return;
        CyberState s = Cyberware.state(sp);
        boolean changed = false;
        for (int i = 0; i < CyberSlot.POSITIONS; i++) {
            ItemStack now = implants.getItem(i);
            if (!ItemStack.isSameItemSameComponents(now, s.at(i))) changed = true;
            s.installed.set(i, now.copy());
        }
        if (changed) {
            s.rollIfNeeded(sp.getRandom());
            s.armsDeployed = false;
            CyberEffects.play(sp, "er_postripperdoc", 1f);
            Cyberware.sendGlitch(sp, 30, 0.5f, 0.6f);
        }
        Cyberware.refreshAttributes(sp);
        CyberSync.send(sp);
        ChromeVisuals.broadcast(sp);
    }

    public int load() { return data.get(0); }
    public int capacity() { return data.get(1); }
    public int pressure() { return data.get(2); }
    public int phase() { return data.get(3); }
    public int capacityLevel() { return data.get(4); }

    /** Server-side factory: a live view of the player's implants plus readouts. */
    public static SurgeryMenu open(int id, Inventory inv, ServerPlayer p) {
        CyberState s = Cyberware.state(p);
        s.rollIfNeeded(p.getRandom());
        SimpleContainer box = new SimpleContainer(CyberSlot.POSITIONS);
        for (int i = 0; i < CyberSlot.POSITIONS; i++) box.setItem(i, s.at(i).copy());
        ContainerData data = new ContainerData() {
            @Override public int get(int i) {
                return switch (i) {
                    case 0 -> previewLoad(box, p);
                    case 1 -> Cyberware.capacity(p);
                    case 2 -> Math.round(s.pressure);
                    case 3 -> s.phase;
                    case 4 -> s.capacityLevel;
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 5; }
        };
        return new SurgeryMenu(id, inv, box, data);
    }

    /** Load as it would be with the implants currently in the menu (plus a construct chip). */
    private static int previewLoad(Container box, ServerPlayer p) {
        int sum = Cyberware.load(p) - Cyberware.state(p).load();
        for (int i = 0; i < box.getContainerSize(); i++) sum += CyberwareItem.implantOf(box.getItem(i)).map(x -> x.load).orElse(0);
        return sum;
    }
}
