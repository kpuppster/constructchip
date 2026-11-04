package io.github.constructchip.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.constructchip.ConstructChip;
import io.github.constructchip.cyberware.ChromeVisuals;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * Sandevistan visuals, after Project: Edgerunner's (A24-N22): a six-layer after-image trail (red-orange near the
 * body, fading to blue further back), green lightning crackling around the body, exhaust puffs from arms and
 * legs. Seen by everyone near the user. The user also gets a teal-edged screen and an FOV punch on activation.
 */
@EventBusSubscriber(modid = ConstructChip.MODID, value = Dist.CLIENT)
public final class SandevistanFx {
    /** Their trail layers: colour, spacing (blocks moved between images), lifetime (ticks), opacity. */
    private record Layer(int rgb, double spacing, int lifetime, float opacity) {}

    private static final Layer[] LAYERS = {
            new Layer(0xFF1B1B, 0.5, 10, 1.0f), new Layer(0xFF3C1B, 0.8, 12, 0.9f), new Layer(0xFF6A3C, 1.1, 14, 0.75f),
            new Layer(0x5F7DD6, 1.6, 22, 0.55f), new Layer(0x3D8BE9, 2.0, 32, 0.45f), new Layer(0x2C94EE, 2.4, 42, 0.35f)};
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final DustParticleOptions SPARK = new DustParticleOptions(new Vector3f(0.16f, 0.52f, 0.05f), 0.5f);
    private static final DustParticleOptions SPARK_CORE = new DustParticleOptions(new Vector3f(0.02f, 0.49f, 0f), 0.35f);

    /** One frozen pose of a player. */
    private record Image(Vec3 pos, float bodyYaw, float headYaw, float pitch, float limbPos, float limbSpeed, float age,
                         boolean crouching, int rgb, float opacity, int lifetime, long born) {}

    private static final Map<UUID, List<Image>> IMAGES = new HashMap<>();
    private static final Map<UUID, Vec3[]> LAST_SPAWN = new HashMap<>();
    private static long activatedAt = -100;
    private static boolean wasActive;

    private SandevistanFx() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        long now = mc.level.getGameTime();
        RandomSource r = mc.level.random;
        for (AbstractClientPlayer p : mc.level.players()) {
            boolean active = ChromeVisuals.sandevistanActive(p, now) && !ClientEvents.hiddenFromMe(p);
            if (p == mc.player) {
                if (active && !wasActive) activatedAt = now;
                wasActive = active;
            }
            if (!active) {
                LAST_SPAWN.remove(p.getUUID());
                continue;
            }
            // after-images: each layer drops a new image every `spacing` blocks moved
            Vec3[] last = LAST_SPAWN.computeIfAbsent(p.getUUID(), k -> new Vec3[LAYERS.length]);
            List<Image> list = IMAGES.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
            for (int i = 0; i < LAYERS.length; i++) {
                Layer l = LAYERS[i];
                if (last[i] == null || last[i].distanceTo(p.position()) >= l.spacing()) {
                    last[i] = p.position();
                    list.add(new Image(p.position(), p.yBodyRot, p.yHeadRot, p.getXRot(), p.walkAnimation.position(),
                            p.walkAnimation.speed(), p.tickCount, p.isCrouching(), l.rgb(), l.opacity(), l.lifetime(), now));
                }
            }
            // green lightning around the body
            for (int i = 0; i < 3; i++) {
                Vec3 a = new Vec3(p.getRandomX(0.8), p.getRandomY(), p.getRandomZ(0.8));
                Vec3 b = a.add((r.nextDouble() - 0.5) * 0.7, (r.nextDouble() - 0.5) * 0.7, (r.nextDouble() - 0.5) * 0.7);
                for (int k = 0; k <= 5; k++) {
                    Vec3 q = a.lerp(b, k / 5.0).add((r.nextDouble() - 0.5) * 0.08, (r.nextDouble() - 0.5) * 0.08, (r.nextDouble() - 0.5) * 0.08);
                    mc.level.addParticle(k % 2 == 0 ? SPARK_CORE : SPARK, q.x, q.y, q.z, 0, 0, 0);
                }
            }
            // exhaust: arms and legs vent backwards-up
            if (r.nextFloat() < 0.45f && (p != mc.player || !mc.options.getCameraType().isFirstPerson())) {
                float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
                Vec3 back = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw));
                Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
                double s = r.nextBoolean() ? 1 : -1;
                double h = r.nextBoolean() ? 1.2 : 0.55;
                Vec3 at = p.position().add(side.scale(0.38 * s)).add(back.scale(0.15)).add(0, h, 0);
                Vec3 m = side.scale(0.04 * s).add(back.scale(0.03)).add(0, 0.05, 0);
                mc.level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, m.x, m.y, m.z);
            }
        }
        // age out
        for (Iterator<Map.Entry<UUID, List<Image>>> it = IMAGES.entrySet().iterator(); it.hasNext(); ) {
            List<Image> list = it.next().getValue();
            list.removeIf(im -> now - im.born() > im.lifetime());
            if (list.isEmpty()) it.remove();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || IMAGES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucentEmissive(WHITE);
        long now = mc.level.getGameTime();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (Map.Entry<UUID, List<Image>> e : IMAGES.entrySet()) {
            if (!(mc.level.getPlayerByUUID(e.getKey()) instanceof AbstractClientPlayer owner)) continue;
            if (!(mc.getEntityRenderDispatcher().getRenderer(owner) instanceof PlayerRenderer renderer)) continue;
            PlayerModel<AbstractClientPlayer> model = renderer.getModel();
            VertexConsumer vc = buffers.getBuffer(type);
            for (Image im : e.getValue()) {
                float life = 1f - (now - im.born() + partial) / im.lifetime();
                if (life <= 0) continue;
                // don't draw the freshest images right inside the player's own body
                if (owner == mc.player && mc.options.getCameraType().isFirstPerson() && im.pos().distanceToSqr(owner.position()) < 0.5) continue;
                int alpha = (int) (255 * im.opacity() * life * 0.8f);
                int color = FastColor.ARGB32.color(alpha, (im.rgb() >> 16) & 0xFF, (im.rgb() >> 8) & 0xFF, im.rgb() & 0xFF);
                ps.pushPose();
                ps.translate(im.pos().x - cam.x, im.pos().y - cam.y, im.pos().z - cam.z);
                ps.mulPose(Axis.YP.rotationDegrees(180f - im.bodyYaw()));
                ps.scale(-0.9375f, -0.9375f, 0.9375f);
                ps.translate(0, -1.501, 0);
                model.attackTime = 0;
                model.riding = false;
                model.young = false;
                model.crouching = im.crouching();
                model.setAllVisible(true);
                model.setupAnim(owner, im.limbPos(), im.limbSpeed(), im.age(), Mth.wrapDegrees(im.headYaw() - im.bodyYaw()), im.pitch());
                model.renderToBuffer(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
                ps.popPose();
            }
        }
        buffers.endBatch(type);
    }

    /** FOV punch on activation, then a slight stretch while it runs. */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !wasActive) return;
        long since = mc.level.getGameTime() - activatedAt;
        float punch = since < 8 ? 1f + 0.25f * (1f - since / 8f) : 1.06f;
        event.setNewFovModifier(event.getFovModifier() * punch);
    }

    /** Teal edges + faint scanlines on the user's screen while it runs (drawn by the HUD layer). */
    static void renderOverlay(GuiGraphics g) {
        if (!wasActive) return;
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth(), h = g.guiHeight();
        long since = mc.level == null ? 99 : mc.level.getGameTime() - activatedAt;
        int edge = since < 6 ? 0x70 : 0x38;
        int band = Math.max(24, w / 10);
        g.fillGradient(0, 0, w, band, (edge << 24) | 0x1BE0A0, 0x001BE0A0);
        g.fillGradient(0, h - band, w, h, 0x001BE0A0, (edge << 24) | 0x1BE0A0);
        for (int y = 0; y < h; y += 4) g.fill(0, y, w, y + 1, 0x0C1BE0A0);
        if (since < 4) g.fill(0, 0, w, h, (int) ((1f - since / 4f) * 0x60) << 24 | 0xD0FFF0);
    }
}
