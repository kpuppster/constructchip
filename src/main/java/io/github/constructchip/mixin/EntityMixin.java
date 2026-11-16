package io.github.constructchip.mixin;

import io.github.constructchip.ghost.GhostState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * On other players' clients a construct counts as invisible. Its body render is already cancelled; this also stops
 * the shadow (drawn separately by the entity renderer) from giving it away.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void constructchip$hiddenConstruct(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player && self.level().isClientSide && GhostState.clientHidden.test(self)) cir.setReturnValue(true);
    }
}
