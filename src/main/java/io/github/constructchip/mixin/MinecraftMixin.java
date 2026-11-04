package io.github.constructchip.mixin;

import io.github.constructchip.client.ClientHighlights;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client-only outline for entities a quickhack highlighted (Scanner target, Ping). Only this client sees it. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void constructchip$hackHighlight(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ClientHighlights.isHighlighted(entity)) cir.setReturnValue(true);
    }
}
