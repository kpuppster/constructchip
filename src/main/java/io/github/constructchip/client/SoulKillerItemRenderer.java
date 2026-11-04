package io.github.constructchip.client;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.block.SoulKillerItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Item form of the ripperdoc station (A24-N22, used with permission). Sizing comes from models/item/soul_killer.json. */
public class SoulKillerItemRenderer extends GeoItemRenderer<SoulKillerItem> {
    public SoulKillerItemRenderer() {
        super(new GeoModel<>() {
            @Override public ResourceLocation getModelResource(SoulKillerItem animatable) { return ConstructChip.id("geo/soul_killer.geo.json"); }
            @Override public ResourceLocation getTextureResource(SoulKillerItem animatable) { return ConstructChip.id("textures/block/soul_killer_station.png"); }
            @Override public ResourceLocation getAnimationResource(SoulKillerItem animatable) { return ConstructChip.id("animations/soul_killer.animation.json"); }
        });
    }
}
