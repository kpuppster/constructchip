package io.github.constructchip.client;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.github.constructchip.ConstructChip;
import io.github.constructchip.block.ChairSeatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Players sitting in the Soul Killer play A24-N22's ripperdoc sitting animation (Project: Edgerunner, used with
 * permission) through PlayerAnimator. Every client animates every sitter it can see. Our copy drops the original's
 * whole-body offset (we place the rider on the cushion ourselves) and its fixed head angle, so the head follows the
 * player's view while {@code ChairSeatEntity} keeps the body locked to the chair.
 */
@EventBusSubscriber(modid = ConstructChip.MODID, value = Dist.CLIENT)
public final class SitAnimation {
    /** Layer id on each player's animation stack. */
    private static final ResourceLocation LAYER = ConstructChip.id("chair");
    /** PlayerAnimator names animations "<namespace>:<animation name inside the file>". */
    private static final ResourceLocation SIT = ResourceLocation.fromNamespaceAndPath(ConstructChip.MODID, "ripperdoc_sentarse");

    private SitAnimation() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000, player -> new ModifierLayer<>());
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        for (AbstractClientPlayer player : mc.level.players()) {
            Object data = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER);
            if (!(data instanceof ModifierLayer<?> raw)) continue;
            @SuppressWarnings("unchecked")
            ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) raw;
            boolean sitting = player.getVehicle() instanceof ChairSeatEntity;
            if (sitting && layer.getAnimation() == null) {
                PlayerAnimationRegistry.getAnimationOptional(SIT).ifPresent(anim -> layer.setAnimation(anim.playAnimation()));
            } else if (!sitting && layer.getAnimation() != null) {
                layer.setAnimation(null);
            }
        }
    }
}
