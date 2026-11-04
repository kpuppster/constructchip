package io.github.constructchip.mixin;

import io.github.constructchip.client.ImplantArms;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the normal arms under the Gorilla Arms model in third person (first-person hands stay vanilla). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "setModelProperties", at = @At("TAIL"))
    private void constructchip$hideArms(AbstractClientPlayer player, CallbackInfo ci) {
        ImplantArms.apply((PlayerRenderer) (Object) this, player);
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"))
    private void constructchip$firstPersonStartR(CallbackInfo ci) { ImplantArms.firstPerson = true; }

    @Inject(method = "renderRightHand", at = @At("RETURN"))
    private void constructchip$firstPersonEndR(CallbackInfo ci) { ImplantArms.firstPerson = false; }

    @Inject(method = "renderLeftHand", at = @At("HEAD"))
    private void constructchip$firstPersonStartL(CallbackInfo ci) { ImplantArms.firstPerson = true; }

    @Inject(method = "renderLeftHand", at = @At("RETURN"))
    private void constructchip$firstPersonEndL(CallbackInfo ci) { ImplantArms.firstPerson = false; }
}
