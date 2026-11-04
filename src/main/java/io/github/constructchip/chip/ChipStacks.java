package io.github.constructchip.chip;

import io.github.constructchip.registry.ModDataComponents;
import io.github.constructchip.registry.ModItems;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Helpers for reading and writing chip components. All writes replace the (immutable) component value. */
public final class ChipStacks {
    /** Starting RAM for a freshly captured chip. Tune later (config / skills). */
    public static final int BASE_MAX_RAM = 8;
    public static final float BASE_REGEN_PER_SECOND = 0.5f;

    private ChipStacks() {}

    public static boolean isChip(ItemStack stack) {
        return stack.is(ModItems.PERSONALITY_CHIP.get());
    }

    public static ItemStack createBlank() {
        return new ItemStack(ModItems.PERSONALITY_CHIP.get());
    }

    /**
     * A chip holding {@code captured}, with a brand-new chipId, full RAM, tier 1 and no timers.
     * Does NOT register the chip or touch the player; capture/registry code is responsible for that.
     */
    public static ItemStack createCaptured(ServerPlayer captured, long now) {
        ItemStack stack = createBlank();
        stack.set(ModDataComponents.CHIP_IDENTITY.get(),
                ChipIdentity.fresh(captured.getUUID(), captured.getGameProfile().getName()));
        stack.set(ModDataComponents.CHIP_RAM.get(), ChipRam.full(BASE_MAX_RAM, BASE_REGEN_PER_SECOND, now));
        stack.set(ModDataComponents.CHIP_SKILLS.get(), ChipSkills.DEFAULT);
        stack.set(ModDataComponents.CHIP_TIMERS.get(), ChipTimers.NONE);
        return stack;
    }

    public static Optional<ChipIdentity> identity(ItemStack stack) {
        return isChip(stack) ? Optional.ofNullable(stack.get(ModDataComponents.CHIP_IDENTITY.get())) : Optional.empty();
    }

    public static boolean isCaptured(ItemStack stack) {
        return identity(stack).isPresent();
    }

    public static ChipRam ram(ItemStack stack) {
        // stampTick 0 with stored == max is always full, so a missing component reads as a full base pool
        return stack.getOrDefault(ModDataComponents.CHIP_RAM.get(),
                ChipRam.full(BASE_MAX_RAM, BASE_REGEN_PER_SECOND, 0L));
    }

    public static ChipSkills skills(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CHIP_SKILLS.get(), ChipSkills.DEFAULT);
    }

    public static ChipTimers timers(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CHIP_TIMERS.get(), ChipTimers.NONE);
    }

    /** Removes every chip component, leaving a blank chip. */
    public static void clearAll(ItemStack stack) {
        stack.remove(ModDataComponents.CHIP_IDENTITY.get());
        stack.remove(ModDataComponents.CHIP_RAM.get());
        stack.remove(ModDataComponents.CHIP_SKILLS.get());
        stack.remove(ModDataComponents.CHIP_TIMERS.get());
        stack.remove(ModDataComponents.CHIP_SERIAL.get());
    }

    public static void setRam(ItemStack stack, ChipRam ram) {
        stack.set(ModDataComponents.CHIP_RAM.get(), ram);
    }

    public static void setSkills(ItemStack stack, ChipSkills skills) {
        stack.set(ModDataComponents.CHIP_SKILLS.get(), skills);
    }

    public static void setTimers(ItemStack stack, ChipTimers timers) {
        stack.set(ModDataComponents.CHIP_TIMERS.get(), timers);
    }

    /**
     * Spends RAM if affordable. Returns false (and changes nothing) otherwise.
     * Overclock halves the cost. If the stack lives in a data attachment, the caller must re-call setData afterwards.
     */
    public static boolean trySpendRam(ItemStack stack, float baseCost, long now) {
        float cost = timers(stack).isOverclocked(now) ? baseCost * 0.5f : baseCost;
        ChipRam ram = ram(stack);
        if (!ram.canAfford(cost, now)) return false;
        setRam(stack, ram.spend(cost, now));
        return true;
    }
}
