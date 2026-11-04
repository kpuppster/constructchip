package io.github.constructchip.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Painted implant textures (filled in by the painting station step). */
public final class PaintTextures {
    private PaintTextures() {}

    /** The painted texture for {@code stack} on top of {@code base}, or null if unpainted. */
    public static ResourceLocation textureFor(ItemStack stack, ResourceLocation base) {
        return null;
    }
}
