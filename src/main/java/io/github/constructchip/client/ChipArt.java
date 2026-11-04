package io.github.constructchip.client;

import net.minecraft.resources.ResourceLocation;

/** Ikeda's chip art and panel (Capri mod), used with permission. */
final class ChipArt {
    /** 256x256 chip illustration. */
    static final ResourceLocation ART = ResourceLocation.fromNamespaceAndPath("constructchip", "textures/gui/cyber/chip_art.png");
    /** Nine-slice GUI sprite (textures/gui/sprites/chip_panel.png). */
    static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath("constructchip", "chip_panel");

    private ChipArt() {}
}
