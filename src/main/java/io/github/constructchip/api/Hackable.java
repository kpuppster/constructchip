package io.github.constructchip.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Implement on an Item that is cyberware (implants, gear with active effects) so constructs can hack it.
 * Short Circuit calls {@link #shortCircuit} for 10 s; System Lock calls it for 30 s. While disabled, your item
 * should stop doing its thing; check {@link CyberwareApi#isDisabled}.
 */
public interface Hackable {
    boolean canShortCircuit(ItemStack stack, LivingEntity wearer);

    void shortCircuit(ItemStack stack, LivingEntity wearer, int ticks);
}
