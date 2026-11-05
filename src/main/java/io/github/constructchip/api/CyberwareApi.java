package io.github.constructchip.api;

import io.github.constructchip.attachment.CyberwareState;
import io.github.constructchip.attachment.ModAttachments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Public queries for other mods: is this entity's cyberware currently knocked out by a construct? */
public final class CyberwareApi {
    private CyberwareApi() {}

    private static final boolean CYBERNETICS = net.neoforged.fml.ModList.get().isLoaded("createcybernetics");

    public static boolean isDisabled(LivingEntity entity) {
        return entity.getData(ModAttachments.CYBERWARE_STATE).disabledUntil() > entity.level().getGameTime();
    }

    /** System Lock: disabled AND can't be turned back on until this passes. */
    public static boolean isLocked(LivingEntity entity) {
        return entity.getData(ModAttachments.CYBERWARE_STATE).lockedUntil() > entity.level().getGameTime();
    }

    /** Disables the entity's cyberware for {@code ticks} and short-circuits every {@link Hackable} it wears. */
    public static int disable(LivingEntity entity, int ticks, boolean lock) {
        long now = entity.level().getGameTime();
        CyberwareState old = entity.getData(ModAttachments.CYBERWARE_STATE);
        entity.setData(ModAttachments.CYBERWARE_STATE, new CyberwareState(
                Math.max(old.disabledUntil(), now + ticks), lock ? Math.max(old.lockedUntil(), now + ticks) : old.lockedUntil()));
        int hacked = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof Hackable h && h.canShortCircuit(stack, entity)) {
                h.shortCircuit(stack, entity, ticks);
                hacked++;
            }
        }
        // Cybernetics (optional): its own EMP knocks its implants out. Only touched when that mod is installed.
        if (CYBERNETICS && !entity.level().isClientSide) {
            hacked += io.github.constructchip.compat.cybernetics.CyberneticsCompat.disable(entity, ticks, lock);
        }
        return hacked;
    }
}
