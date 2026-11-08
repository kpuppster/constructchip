package io.github.constructchip.mixin;

import io.github.constructchip.client.ClientPossession;
import io.github.constructchip.possession.Possession;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A construct in its host's body (Takeover) is drawn with the host's skin. */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void constructchip$bodySkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        if (!Possession.clientPairs.containsKey(self.getUUID())) return;
        PlayerSkin skin = ClientPossession.skinFor(self.getUUID());
        if (skin != null) cir.setReturnValue(skin);
    }
}
