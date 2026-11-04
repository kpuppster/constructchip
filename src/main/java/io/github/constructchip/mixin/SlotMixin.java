package io.github.constructchip.mixin;

import io.github.constructchip.chip.ChipStacks;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A captured chip may only sit in a player's own inventory (including armor/offhand), never in a container slot.
 * Covers clicks, shift-clicks, drags and number keys, since they all ask mayPlace. Slot subclasses that override mayPlace
 * without calling super (some mod slots) are not covered; those chips fall back to the STORED state.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow @Final public Container container;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void constructchip$keepChipsOutOfContainers(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!(container instanceof Inventory) && ChipStacks.isCaptured(stack)) {
            cir.setReturnValue(false);
        }
    }
}
