package io.github.constructchip.client;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.ghost.GhostState;
import io.github.constructchip.hack.HackTarget;
import io.github.constructchip.hack.ModQuickhacks;
import io.github.constructchip.hack.Quickhack;
import io.github.constructchip.network.ServerboundPayloads;
import io.github.constructchip.network.UseQuickhackPayload;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Keys → requests. Aiming is a client raycast from the construct's own eyes, reaching further than any hack.
 * The server then checks range and line of sight from the HOST's eyes, so aiming further than allowed simply fails.
 */
@EventBusSubscriber(modid = ConstructChip.MODID, value = Dist.CLIENT)
public final class ClientInput {
    public static final double AIM_RANGE = 32.0;
    public static final ResourceLocation SCANNER = ConstructChip.id("scanner");

    private ClientInput() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        while (Keybinds.NEXT_HACK.consumeClick()) {
            if (!ClientChipState.isConstruct()) continue;
            ClientChipState.cycleHack(player.isShiftKeyDown() ? -1 : 1);
            UiSounds.blip();
            ResourceLocation hack = ClientChipState.selectedHack();
            if (hack != null) player.displayClientMessage(Component.translatable("message.constructchip.selected", HudLayer.hackName(hack)), true);
        }
        while (Keybinds.USE_HACK.consumeClick()) {
            if (!ClientChipState.isConstruct()) continue;
            ResourceLocation hack = ClientChipState.selectedHack();
            if (hack == null) player.displayClientMessage(Component.translatable("message.constructchip.no_hacks"), true);
            else send(player, hack);
        }
        while (Keybinds.SCAN.consumeClick()) {
            if (ClientChipState.isConstruct()) send(player, SCANNER);
        }
        while (Keybinds.HOST_INVENTORY.consumeClick()) {
            if (ClientChipState.isConstruct()) PacketDistributor.sendToServer(new ServerboundPayloads.OpenHostInventory());
        }
        // Power wheel: opens while held (it closes itself when the key is released)
        if (Keybinds.WHEEL.isDown() && mc.screen == null && ClientChipState.isConstruct()) {
            if (ClientChipState.unlockedHacks().isEmpty()) {
                if (Keybinds.WHEEL.consumeClick()) player.displayClientMessage(Component.translatable("message.constructchip.no_hacks"), true);
            } else {
                mc.setScreen(new PowerWheelScreen());
            }
        }
        while (Keybinds.WHEEL.consumeClick()) { /* handled above */ }
        while (Keybinds.SKILL_TREE.consumeClick()) {
            if (ClientChipState.chip().present()) mc.setScreen(new SkillTreeScreen());
            else player.displayClientMessage(Component.translatable("message.constructchip.no_chip"), true);
        }
    }

    static void send(LocalPlayer player, ResourceLocation hackId) {
        Quickhack hack = ModQuickhacks.REGISTRY.get(hackId);
        HackTarget target = hack == null || hack.targetType() == Quickhack.TargetType.NONE
                ? HackTarget.NONE : aim(player, hack.targetType());
        PacketDistributor.sendToServer(new UseQuickhackPayload(hackId, target));
    }

    /** Nearest creature (not the host, not constructs) or block along the view. */
    static HackTarget aim(LocalPlayer player, Quickhack.TargetType type) {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getViewVector(1f);
        Vec3 end = eye.add(dir.scale(AIM_RANGE));
        BlockHitResult blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
        double maxSq = hitBlock ? eye.distanceToSqr(blockHit.getLocation()) : AIM_RANGE * AIM_RANGE;
        UUID host = GhostState.clientHostOf(player.getUUID());
        if (type != Quickhack.TargetType.BLOCK) {
            AABB box = player.getBoundingBox().expandTowards(dir.scale(AIM_RANGE)).inflate(1.0);
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                    e -> e != player && !e.isSpectator() && e.isPickable() && !e.getUUID().equals(host), maxSq);
            if (hit != null) return HackTarget.entity(hit.getEntity().getId());
        }
        if (hitBlock && type != Quickhack.TargetType.ENTITY) return HackTarget.block(blockHit.getBlockPos(), blockHit.getDirection());
        return HackTarget.NONE;
    }

}
