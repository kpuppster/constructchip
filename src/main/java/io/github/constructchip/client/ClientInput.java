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
                ? HackTarget.NONE : aim(player, hack.targetType(), hack.baseRange(), hack.aimAssist());
        PacketDistributor.sendToServer(new UseQuickhackPayload(hackId, target));
    }

    /** Aim assist cone for creature-only hacks, in degrees (on top of the creature's own size). */
    private static final double ASSIST_DEGREES = 6.0;

    static HackTarget aim(LocalPlayer player, Quickhack.TargetType type) {
        return aim(player, type, AIM_RANGE, type == Quickhack.TargetType.ENTITY);
    }

    /**
     * Nearest creature (not the host, not constructs) or block along the view. For creature-only hacks a near miss
     * still counts: the creature closest to the crosshair inside a small cone is picked, preferring ones the host can
     * actually reach (the server checks range and line of sight from the host's eyes).
     */
    static HackTarget aim(LocalPlayer player, Quickhack.TargetType type, double hostRange, boolean assistAim) {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getViewVector(1f);
        Vec3 end = eye.add(dir.scale(AIM_RANGE));
        BlockHitResult blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
        double maxSq = hitBlock ? eye.distanceToSqr(blockHit.getLocation()) : AIM_RANGE * AIM_RANGE;
        UUID host = GhostState.clientHostOf(player.getUUID());
        if (type != Quickhack.TargetType.BLOCK) {
            AABB box = player.getBoundingBox().expandTowards(dir.scale(AIM_RANGE)).inflate(1.0);
            boolean livingOnly = type == Quickhack.TargetType.ENTITY;
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                    e -> e != player && !e.isSpectator() && e.isPickable() && !e.getUUID().equals(host)
                            && (!livingOnly || e instanceof net.minecraft.world.entity.LivingEntity), maxSq);
            if (hit != null) return HackTarget.entity(hit.getEntity().getId());
            if (assistAim) {
                net.minecraft.world.entity.Entity assisted = assist(player, eye, dir, host, hostRange);
                if (assisted != null) return HackTarget.entity(assisted.getId());
            }
        }
        if (hitBlock && type != Quickhack.TargetType.ENTITY) return HackTarget.block(blockHit.getBlockPos(), blockHit.getDirection());
        return HackTarget.NONE;
    }

    private static net.minecraft.world.entity.Entity assist(LocalPlayer player, Vec3 eye, Vec3 dir, UUID hostId, double hostRange) {
        net.minecraft.world.entity.player.Player hostPlayer = hostId == null ? null : player.level().getPlayerByUUID(hostId);
        net.minecraft.world.entity.Entity best = null;
        double bestScore = Double.MAX_VALUE;
        for (net.minecraft.world.entity.LivingEntity e : player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                player.getBoundingBox().inflate(AIM_RANGE))) {
            if (e == player || !e.isAlive() || e.isSpectator() || !e.isPickable() || e.getUUID().equals(hostId)
                    || io.github.constructchip.ghost.GhostState.isConstruct(e)) continue;
            Vec3 center = e.getBoundingBox().getCenter();
            Vec3 to = center.subtract(eye);
            double dist = to.length();
            if (dist < 0.5 || dist > AIM_RANGE) continue;
            double angle = Math.toDegrees(Math.acos(net.minecraft.util.Mth.clamp(dir.dot(to.scale(1 / dist)), -1, 1)));
            double size = Math.toDegrees(Math.atan2(Math.max(e.getBbWidth(), e.getBbHeight()) * 0.5, dist));
            if (angle > ASSIST_DEGREES + size) continue;
            if (!clear(player, eye, center)) continue; // the construct has to see it
            double score = angle;
            if (hostPlayer != null) {
                Vec3 hostEye = hostPlayer.getEyePosition();
                if (hostEye.distanceTo(center) > hostRange || !clear(player, hostEye, center)) score += 100; // host can't reach it
            }
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private static boolean clear(LocalPlayer player, Vec3 from, Vec3 to) {
        BlockHitResult r = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return r.getType() == HitResult.Type.MISS;
    }
}
