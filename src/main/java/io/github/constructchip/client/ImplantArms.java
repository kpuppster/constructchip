package io.github.constructchip.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

/** Called from PlayerRendererMixin after vanilla sets part visibility. */
public final class ImplantArms {
    public static boolean firstPerson;

    private ImplantArms() {}

    public static void apply(PlayerRenderer renderer, AbstractClientPlayer player) {
        if (firstPerson || !ImplantLayer.gorillaArmsShown(player)) return;
        PlayerModel<AbstractClientPlayer> m = renderer.getModel();
        m.leftArm.visible = false;
        m.leftSleeve.visible = false;
        m.rightArm.visible = false;
        m.rightSleeve.visible = false;
    }
}
