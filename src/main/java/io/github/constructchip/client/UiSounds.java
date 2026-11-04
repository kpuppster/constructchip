package io.github.constructchip.client;

import io.github.constructchip.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

/** Local UI sounds (only this player hears them). */
final class UiSounds {
    private UiSounds() {}

    static void blip() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BLIP.get(), 1f, 0.6f));
    }

    static void hover() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.UI_HOVER.get(), 1f, 0.35f));
    }
}
