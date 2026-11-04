package io.github.constructchip.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.constructchip.ClientConfig;
import io.github.constructchip.cyberware.ChromeVisuals;
import io.github.constructchip.cyberware.CyberwareItem;
import io.github.constructchip.cyberware.Implant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Draws installed implants on players (each model can be switched off in the client config). */
public class ImplantLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final Map<String, ImplantRenderer> RENDERERS = new HashMap<>();

    public ImplantLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static ImplantRenderer renderer(String geo, String texture, String animation) {
        return RENDERERS.computeIfAbsent(geo + "|" + texture, k -> new ImplantRenderer(geo, texture, animation));
    }

    /** Which models an installed implant shows (empty if switched off in the config). */
    static List<ImplantRenderer> renderersFor(Implant implant, ItemStack stack) {
        List<ImplantRenderer> out = new ArrayList<>();
        switch (implant) {
            case WARP_DANCER, ZETATECH, DYNALAR, FALCON, APOGEE -> {
                if (ClientConfig.SANDEVISTAN.get()) out.add(renderer("sandevistan_model", "sandevistan_" + (implant == Implant.WARP_DANCER ? "warp_dancer" : implant.id()) + "_texture", "none"));
            }
            case GORILLA_ARMS -> {
                if (ClientConfig.GORILLA_ARMS.get()) {
                    String suffix = "electric".equals(CyberwareItem.element(stack)) ? "_electric" : "";
                    out.add(renderer("gorilla_arm_left", "gorilla_arm_texture" + suffix, "gorilla_arms"));
                    out.add(renderer("gorilla_arm_right", "gorilla_arm_right_texture" + suffix, "gorilla_arms"));
                }
            }
            case BLOOD_PUMP -> {
                if (ClientConfig.BLOOD_PUMP.get()) {
                    int lvl = CyberwareItem.upgrade(stack);
                    out.add(renderer(lvl <= 0 ? "blood_pump_model" : "blood_pump_" + Math.min(3, lvl) + "_model", "blood_pump", "blood_pump_model"));
                }
            }
            case FORTIFIED_ANKLES -> {
                if (ClientConfig.FORTIFIED_ANKLES.get()) out.add(renderer("fortified_ankles_model", "fortified_ankles_texture", "fortified_ankles_model"));
            }
            case REINFORCED_TENDONS -> {
                if (ClientConfig.REINFORCED_TENDONS.get()) out.add(renderer("reinforced_tendons_model", "reinforced_tendons_texture", "reinforced_tendons_model"));
            }
            default -> { }
        }
        return out;
    }

    static boolean gorillaArmsShown(AbstractClientPlayer player) {
        if (!ClientConfig.GORILLA_ARMS.get() || !ClientConfig.HIDE_ARMS_UNDER_GORILLA.get()) return false;
        ChromeVisuals.Visible v = ChromeVisuals.get(player);
        if (v == null) return false;
        for (ItemStack s : v.installed()) if (CyberwareItem.implantOf(s).map(i -> i == Implant.GORILLA_ARMS).orElse(false)) return true;
        return false;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        ChromeVisuals.Visible v = ChromeVisuals.get(player);
        if (v == null) return;
        for (ItemStack stack : v.installed()) {
            if (!(stack.getItem() instanceof CyberwareItem item)) continue;
            for (ImplantRenderer r : renderersFor(item.implant, stack)) {
                r.prepForRender(player, stack, EquipmentSlot.CHEST, getParentModel(), buffer, partialTick, limbSwing, limbSwingAmount, netHeadYaw, headPitch);
                r.renderToBuffer(poseStack, buffer.getBuffer(RenderType.armorCutoutNoCull(r.getTextureLocation(item))), light,
                        OverlayTexture.NO_OVERLAY, -1);
            }
        }
    }
}
