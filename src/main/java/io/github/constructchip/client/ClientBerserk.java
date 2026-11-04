package io.github.constructchip.client;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.network.CyberStatusPayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.joml.Vector3f;

/**
 * Cyberpsychosis on your own client. Berserk episodes: forced third person, your inputs ignored, the chrome aims,
 * chases and swings. Watcher eyes in the dark (Phase II+). IFF corruption (Phase III+): other players' name tags
 * read HOSTILE.
 */
@EventBusSubscriber(modid = ConstructChip.MODID, value = Dist.CLIENT)
public final class ClientBerserk {
    private static final DustParticleOptions EYE = new DustParticleOptions(new Vector3f(1f, 0.05f, 0.1f), 0.7f);
    private static int targetId = -1, ticksLeft;
    private static CameraType savedCamera;

    private ClientBerserk() {}

    static void start(int target, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (ticks <= 0) {
            stop();
            return;
        }
        if (savedCamera == null) savedCamera = mc.options.getCameraType();
        targetId = target;
        ticksLeft = ticks;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    private static void stop() {
        Minecraft mc = Minecraft.getInstance();
        targetId = -1;
        ticksLeft = 0;
        if (savedCamera != null) mc.options.setCameraType(savedCamera);
        savedCamera = null;
    }

    static boolean active() {
        return ticksLeft > 0;
    }

    private static Entity target() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null || targetId < 0 ? null : mc.level.getEntity(targetId);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        CyberStatusPayload s = ClientCyber.own();
        int phase = s == null ? 0 : s.phase();

        // watcher eyes: a pair of red points somewhere in the dark around you
        RandomSource r = p.getRandom();
        if (phase >= 2 && !mc.isPaused() && r.nextInt(phase >= 4 ? 40 : 120) == 0) {
            double a = r.nextDouble() * Math.PI * 2, d = 8 + r.nextDouble() * 6;
            Vec3 at = p.position().add(Math.cos(a) * d, 1.5 + r.nextDouble(), Math.sin(a) * d);
            Vec3 side = new Vec3(-Math.sin(a), 0, Math.cos(a)).scale(0.18);
            for (int i = 0; i < 6; i++) {
                mc.level.addParticle(EYE, at.x + side.x, at.y, at.z + side.z, 0, 0, 0);
                mc.level.addParticle(EYE, at.x - side.x, at.y, at.z - side.z, 0, 0, 0);
            }
        }

        if (!active()) return;
        if (--ticksLeft <= 0 || !(target() instanceof LivingEntity t) || !t.isAlive()) {
            stop();
            return;
        }
        // auto-aim
        Vec3 to = t.getEyePosition().subtract(p.getEyePosition());
        float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90f;
        float pitch = (float) -(Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * Mth.RAD_TO_DEG);
        p.setYRot(Mth.rotLerp(0.5f, p.getYRot(), yaw));
        p.setXRot(Mth.lerp(0.5f, p.getXRot(), pitch));
        p.yHeadRot = p.getYRot();
        // swing on full charge when in reach
        if (p.getAttackStrengthScale(0f) >= 1f && p.distanceToSqr(t) <= 3.2 * 3.2 && mc.gameMode != null) {
            mc.gameMode.attack(p, t);
            p.swing(InteractionHand.MAIN_HAND);
        }
    }

    /** Input lock + auto-chase: the chrome walks you at the target, jumping over steps. */
    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        if (!active()) return;
        var input = event.getInput();
        Entity t = target();
        boolean far = t != null && event.getEntity().distanceToSqr(t) > 2.2 * 2.2;
        input.forwardImpulse = far ? 1f : 0f;
        input.leftImpulse = 0f;
        input.up = far;
        input.down = input.left = input.right = false;
        input.shiftKeyDown = false;
        input.jumping = far && event.getEntity().horizontalCollision;
    }

    /** IFF corruption: in Phase III+ everyone else reads as hostile. */
    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        CyberStatusPayload s = ClientCyber.own();
        if (s == null || s.phase() < 3 || !(event.getEntity() instanceof Player other) || other == Minecraft.getInstance().player) return;
        event.setContent(Component.literal("HOSTILE").withStyle(st -> st.withColor(0xFF2040).withFont(HudLayer.HUD_FONT)));
    }
}
