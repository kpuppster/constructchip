package io.github.constructchip.mixin;

import io.github.constructchip.possession.Possession;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A construct in its host's body (Takeover) goes by the host's name: name tag, chat, death messages. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void constructchip$bodyName(CallbackInfoReturnable<Component> cir) {
        Player self = (Player) (Object) this;
        if (!Possession.isPossessing(self)) return;
        Component name = Possession.bodyName(self);
        if (name != null) cir.setReturnValue(name);
    }
}
