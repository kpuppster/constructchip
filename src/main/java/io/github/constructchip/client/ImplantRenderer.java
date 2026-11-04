package io.github.constructchip.client;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.cyberware.CyberwareItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * One implant model drawn on a player, using GeckoLib's armor mapping (bones armorBody, armorLeftArm, ... follow the
 * player's limbs). Models, textures and animations by A24-N22 (Project: Edgerunner), used with permission.
 */
public class ImplantRenderer extends GeoArmorRenderer<CyberwareItem> {
    private final ResourceLocation texture;

    public ImplantRenderer(String geo, String texture, String animation) {
        super(new GeoModel<>() {
            @Override public ResourceLocation getModelResource(CyberwareItem a) { return ConstructChip.id("geo/implant/" + geo + ".geo.json"); }
            @Override public ResourceLocation getTextureResource(CyberwareItem a) { return ConstructChip.id("textures/models/implant/" + texture + ".png"); }
            @Override public ResourceLocation getAnimationResource(CyberwareItem a) { return ConstructChip.id("animations/implant/" + animation + ".animation.json"); }
        });
        this.texture = ConstructChip.id("textures/models/implant/" + texture + ".png");
    }

    /** Painted implants use their painted texture (see PaintTextures). */
    @Override
    public ResourceLocation getTextureLocation(CyberwareItem animatable) {
        ResourceLocation painted = getCurrentStack() == null ? null : PaintTextures.textureFor(getCurrentStack(), texture);
        return painted != null ? painted : texture;
    }

    /** Implants aren't armour: every bone the model has is shown, whatever "slot" we render in. */
    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        for (GeoBone b : new GeoBone[]{head, body, rightArm, leftArm, rightLeg, leftLeg, rightBoot, leftBoot}) {
            if (b != null) b.setHidden(false);
        }
    }
}
