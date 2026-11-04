package io.github.constructchip.client;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.cyberware.CyberEffects;
import io.github.constructchip.cyberware.CyberSlot;
import io.github.constructchip.cyberware.Implant;
import io.github.constructchip.network.CyberPayloads;
import io.github.constructchip.network.CyberStatusPayload;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client side of cyberware: your own chrome (keys, jumps, zoom) and, for a construct, the host's chrome readout. */
@EventBusSubscriber(modid = ConstructChip.MODID, value = Dist.CLIENT)
public final class ClientCyber {
    private static CyberStatusPayload own, host;
    private static boolean usedDoubleJump, jumpWasDown;
    private static int ankleCharge, ankleHold;

    private ClientCyber() {}

    static void set(CyberStatusPayload p) {
        if (p.ofHost()) host = p;
        else own = p;
    }

    public static CyberStatusPayload own() { return own; }
    public static CyberStatusPayload host() { return host; }

    static boolean has(Implant implant) {
        return own != null && !own.suppressed() && own.implants().contains(implant.id());
    }

    static Implant at(CyberSlot slot) {
        if (own == null) return null;
        String id = own.implants().get(slot.index);
        if (id.isEmpty()) return null;
        try {
            return Implant.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static void activate(CyberEffects.Action action) {
        PacketDistributor.sendToServer(new CyberPayloads.Activate(action.ordinal()));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        boolean construct = ClientChipState.isConstruct();
        while (Keybinds.CYBER_OS.consumeClick()) if (!construct) activate(CyberEffects.Action.OS);
        while (Keybinds.CYBER_ARMS.consumeClick()) if (!construct) activate(CyberEffects.Action.ARMS);
        while (Keybinds.CYBER_PUMP.consumeClick()) if (!construct) activate(CyberEffects.Action.CIRCULATORY);
        // V doubles as the optics scan for non-constructs (constructs use it for the Scanner quickhack)
        if (!construct && own != null && own.any()) {
            while (Keybinds.SCAN.consumeClick()) activate(CyberEffects.Action.FACE);
        }
        if (construct || own == null || own.suppressed()) return;

        // Reinforced Tendons: jump again in mid-air
        boolean jumpDown = mc.options.keyJump.isDown();
        if (p.onGround()) usedDoubleJump = false;
        if (has(Implant.REINFORCED_TENDONS) && jumpDown && !jumpWasDown && !p.onGround() && !usedDoubleJump
                && !p.getAbilities().flying && !p.isInWater() && p.getDeltaMovement().y < 0.25) {
            usedDoubleJump = true;
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, 0.6 + 0.04 * Math.max(0, own.levels().get(CyberSlot.LEGS.index)), v.z);
            PacketDistributor.sendToServer(new CyberPayloads.Jump(false));
        }
        jumpWasDown = jumpDown;

        // Fortified Ankles: crouch on the ground to charge; the charge lingers briefly after you stand
        if (has(Implant.FORTIFIED_ANKLES)) {
            if (p.isCrouching() && p.onGround()) {
                ankleCharge = Math.min(30, ankleCharge + 1);
                ankleHold = 15;
            } else if (ankleHold > 0) {
                ankleHold--;
            } else {
                ankleCharge = Math.max(0, ankleCharge - 2);
            }
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer p) || ankleCharge <= 0 || !has(Implant.FORTIFIED_ANKLES)) return;
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x, v.y + 0.8 * (ankleCharge / 30.0), v.z);
        ankleCharge = 0;
        ankleHold = 0;
        PacketDistributor.sendToServer(new CyberPayloads.Jump(true));
    }

    /** Gorilla Arms: right-click with an empty hand while deployed = Slam. */
    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (own != null && own.armsDeployed() && event.getItemStack().isEmpty()) activate(CyberEffects.Action.SLAM);
    }

    /** Optics zoom (hold C). */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        Implant face = at(CyberSlot.FACE);
        if (face == null || !Keybinds.ZOOM.isDown() || ClientChipState.isConstruct()) return;
        event.setNewFovModifier(event.getFovModifier() / Math.max(1.5f, face.param * 0.6f));
    }

    static int ankleCharge() { return ankleCharge; }

}
