package io.github.constructchip.client;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.block.SoulKillerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Renders the Soul Killer as A24-N22's ripperdoc station (Project: Edgerunner), used with the author's permission. */
public class SoulKillerRenderer extends GeoBlockRenderer<SoulKillerBlockEntity> {
    public SoulKillerRenderer() {
        super(new Model());
    }

    static final class Model extends GeoModel<SoulKillerBlockEntity> {
        private static final ResourceLocation GEO = ConstructChip.id("geo/soul_killer.geo.json");
        private static final ResourceLocation TEXTURE = ConstructChip.id("textures/block/soul_killer_station.png");
        private static final ResourceLocation ANIMATION = ConstructChip.id("animations/soul_killer.animation.json");

        @Override public ResourceLocation getModelResource(SoulKillerBlockEntity animatable) { return GEO; }
        @Override public ResourceLocation getTextureResource(SoulKillerBlockEntity animatable) { return TEXTURE; }
        @Override public ResourceLocation getAnimationResource(SoulKillerBlockEntity animatable) { return ANIMATION; }
    }
}
