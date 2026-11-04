package io.github.constructchip.mixin;

import io.github.constructchip.ghost.GhostState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Constructs don't push or get pushed (both sides, since player movement is client-driven). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void constructchip$ghostNotPushable(CallbackInfoReturnable<Boolean> cir) {
        if (GhostState.isConstruct((LivingEntity) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void constructchip$ghostPushesNothing(CallbackInfo ci) {
        if (GhostState.isConstruct((LivingEntity) (Object) this)) ci.cancel();
    }

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void constructchip$ghostNotPickable(CallbackInfoReturnable<Boolean> cir) {
        if (GhostState.isConstruct((LivingEntity) (Object) this)) cir.setReturnValue(false); // projectiles pass through
    }
}
