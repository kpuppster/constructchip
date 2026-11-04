package io.github.constructchip.client;

import net.minecraft.client.Minecraft;

/** Client-only actions called from common code (only ever invoked on the client side). */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openGuide() {
        Minecraft.getInstance().setScreen(new GuideScreen());
    }
}
