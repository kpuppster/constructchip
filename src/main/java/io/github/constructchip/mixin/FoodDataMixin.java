package io.github.constructchip.mixin;

import io.github.constructchip.cyberware.Cyberware;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** "The price of chrome": with any implant installed, food still fills hunger but no longer heals. */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void constructchip$noFoodHealingWithChrome(Player player, float amount) {
        if (player.level().isClientSide || !Cyberware.state(player).any() || Cyberware.suppressed(player)) player.heal(amount);
    }
}
