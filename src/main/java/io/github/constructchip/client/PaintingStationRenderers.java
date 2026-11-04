package io.github.constructchip.client;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.paint.PaintingStationBlockEntity;
import io.github.constructchip.paint.PaintingStationItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Painting station model + glowmask (A24-N22, used with permission), for the block and the item. */
public final class PaintingStationRenderers {
    private PaintingStationRenderers() {}

    static <T extends GeoAnimatable> GeoModel<T> model() {
        return new GeoModel<>() {
            @Override public ResourceLocation getModelResource(T a) { return ConstructChip.id("geo/painting_station.geo.json"); }
            @Override public ResourceLocation getTextureResource(T a) { return ConstructChip.id("textures/block/painting_station.png"); }
            @Override public ResourceLocation getAnimationResource(T a) { return ConstructChip.id("animations/none.animation.json"); }
        };
    }

    public static class BlockRenderer extends GeoBlockRenderer<PaintingStationBlockEntity> {
        public BlockRenderer() {
            super(PaintingStationRenderers.<PaintingStationBlockEntity>model());
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
    }

    public static class ItemRenderer extends GeoItemRenderer<PaintingStationItem> {
        public ItemRenderer() {
            super(PaintingStationRenderers.<PaintingStationItem>model());
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
    }
}
