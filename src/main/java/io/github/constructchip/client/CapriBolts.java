package io.github.constructchip.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.constructchip.ConstructChip;
import io.github.constructchip.fx.BoltFx;
import io.github.constructchip.network.BoltFxPayload;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Red lightning in the style of Ikeda's Capri mod (used with permission), re-implemented from how its bolts look
 * and behave: a jagged path that keeps bending back toward the target, widths measured in fractions of the screen
 * (so a bolt reads the same near or far), random thick and thin stretches, side branches, a fresh shape every two
 * frames (the flicker), and an optional grow-in from the start point. Flat solid colour, two-sided, no lighting.
 *
 * <p>The server sends {@link BoltFxPayload}s; {@link #handle} turns each preset into bolts.
 */
@EventBusSubscriber(modid = ConstructChip.MODID, value = Dist.CLIENT)
public final class CapriBolts {
    private CapriBolts() {}

    static final int RED = 0xFFFA1942;
    static final int PALE = 0xFFFED0D2;
    private static final int MAX_BOLTS = 160;

    enum Style { BODY, NET, HAND, PALE }
    /** AIR bolts face the camera; GROUND bolts lie flat (horizontal). */
    enum Kind { AIR, GROUND }

    private static final class Bolt {
        Vec3 a, b;
        float width;
        Style style;
        Kind kind;
        double born, life, grow;
        long seed;
        Supplier<Vec3[]> follow;
        boolean clearCentre;
    }

    private record Pending(double at, Runnable action) {}

    private static final List<Bolt> BOLTS = new ArrayList<>();
    private static final List<Pending> PENDING = new ArrayList<>();
    private static final RandomSource R = RandomSource.create();
    private static boolean centreFree;
    private static long frame;
    private static Vec3 camF = Vec3.ZERO, camR = Vec3.ZERO, camU = Vec3.ZERO;
    private static double camTanX = 1, camTanY = 1;
    private static BufferBuilder buf;
    private static Matrix4f mat;
    private static Vec3 camPos = Vec3.ZERO;

    static double sec() {
        return Util.getNanos() / 1.0e9;
    }

    // ------------------------------------------------------------------ spawning

    static void strike(Vec3 a, Vec3 b, float width, double life, Style style, Kind kind, double grow) {
        if (BOLTS.size() >= MAX_BOLTS) return;
        Bolt bolt = new Bolt();
        bolt.a = a;
        bolt.b = b;
        bolt.width = width;
        bolt.style = style;
        bolt.kind = kind;
        bolt.born = sec();
        bolt.life = life;
        bolt.grow = grow;
        bolt.seed = R.nextLong();
        bolt.clearCentre = centreFree;
        BOLTS.add(bolt);
    }

    /** A bolt whose ends are re-read every frame (sparks crawling on a moving body). */
    static void follow(Supplier<Vec3[]> ends, float width, double life, Style style) {
        Vec3[] e = ends.get();
        if (e == null) return;
        int before = BOLTS.size();
        strike(e[0], e[1], width, life, style, Kind.AIR, 0);
        if (BOLTS.size() > before) BOLTS.get(BOLTS.size() - 1).follow = ends;
    }

    /** Straight down out of the sky onto {@code at}. */
    static void skyBolt(Vec3 at, float width, double life, double grow) {
        Vec3 top = at.add((R.nextDouble() - 0.5) * 2, 9 + R.nextDouble() * 4, (R.nextDouble() - 0.5) * 2);
        strike(top, at, width, life, Style.NET, Kind.AIR, grow);
    }

    static void skyStrike(Vec3 at, float scale) {
        skyBolt(at, 0.016f, 0.3, 0.12);
        groundBurst(at, scale, 4);
    }

    /** Flat bolts running out along the ground in a fan that opens toward the camera. */
    static void groundBurst(Vec3 at, float scale, int count) {
        Vec3 p = at.add(0, 0.03, 0);
        Vec3 rel = p.subtract(camPosNow());
        double base = Math.atan2(rel.z, rel.x);
        double spread = Math.toRadians(150);
        for (int i = 0; i < count; i++) {
            double ang = base + (count == 1 ? 0 : (i / (double) (count - 1) - 0.5) * spread) + (R.nextDouble() - 0.5) * 0.4;
            double len = (1.2 + R.nextDouble() * 2.2) * scale * 0.6;
            strike(p, p.add(Math.cos(ang) * len, 0, Math.sin(ang) * len), 0.011f, 0.25 + R.nextDouble() * 0.2, Style.NET, Kind.GROUND, 0.08);
        }
    }

    static void handTo(Vec3 hand, Vec3 to) {
        strike(hand, to, 0.004f, 0.12, Style.HAND, Kind.AIR, 0);
        strike(hand, to, 0.011f, 0.45, Style.HAND, Kind.AIR, 0.15);
    }

    /** Short sparks jumping between points on an entity's body, following it as it moves. */
    static void crawl(Entity e, int count, double spread) {
        for (int i = 0; i < count; i++) {
            double[] o = {R.nextDouble(), R.nextDouble(), R.nextDouble(), R.nextDouble(), R.nextDouble(), R.nextDouble()};
            double delay = i * spread;
            later(delay, () -> follow(() -> {
                if (e.isRemoved()) return null;
                AABB bb = e.getBoundingBox().inflate(0.08);
                return new Vec3[]{point(bb, o[0], o[1], o[2]), point(bb, o[3], o[4], o[5])};
            }, 0.006f, 0.2 + R.nextDouble() * 0.15, R.nextBoolean() ? Style.NET : Style.PALE));
        }
    }

    private static Vec3 point(AABB bb, double x, double y, double z) {
        return new Vec3(Mth.lerp(x, bb.minX, bb.maxX), Mth.lerp(y, bb.minY, bb.maxY), Mth.lerp(z, bb.minZ, bb.maxZ));
    }

    private static void later(double seconds, Runnable action) {
        if (seconds <= 0) action.run();
        else PENDING.add(new Pending(sec() + seconds, action));
    }

    private static Vec3 camPosNow() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameRenderer == null ? Vec3.ZERO : mc.gameRenderer.getMainCamera().getPosition();
    }

    // ------------------------------------------------------------------ presets

    static void handle(BoltFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || p.preset() < 0 || p.preset() >= BoltFx.Preset.values().length) return;
        Entity source = p.source() >= 0 ? mc.level.getEntity(p.source()) : null;
        boolean ownView = source != null && source == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        Vec3 from = p.from();
        List<Vec3> to = p.to();
        centreFree = ownView;
        try {
            switch (BoltFx.Preset.values()[p.preset()]) {
                case HACK -> {
                    if (to.isEmpty()) return;
                    strike(from, to.get(0), 0.010f, 0.18, Style.NET, Kind.AIR, 0.04);
                    strike(from, to.get(0), 0.004f, 0.18, Style.PALE, Kind.AIR, 0.04);
                }
                case SCAN -> {
                    if (!to.isEmpty()) strike(from, to.get(0), 0.003f, 0.14, Style.PALE, Kind.AIR, 0.06);
                }
                case AURA -> {
                    if (source != null) crawl(source, 6, 0.04);
                }
                case FRY -> { // Capri's own hack bolt, plus a burst where it lands
                    if (to.isEmpty()) return;
                    Vec3 hit = to.get(0);
                    strike(from, hit, 0.014f, 0.2, Style.NET, Kind.AIR, 0.04);
                    strike(from, hit, 0.014f, 0.2, Style.NET, Kind.AIR, 0.04);
                    strike(from, hit, 0.0048f, 0.2, Style.NET, Kind.AIR, 0.04);
                    strike(from, hit, 0.0048f, 0.2, Style.NET, Kind.AIR, 0.04);
                    strike(from, hit, 0.02f, 0.3, Style.BODY, Kind.AIR, 0.04);
                    groundBurst(to.size() > 1 ? to.get(1) : hit, 0.9f, 3);
                }
                case HIGH_VOLTAGE -> { // to = chest, feet, chest, feet, ...
                    for (int i = 0; i + 1 < to.size(); i += 2) {
                        Vec3 chest = to.get(i), feet = to.get(i + 1);
                        later(i / 2 * 0.05, () -> {
                            skyBolt(chest, 0.016f, 0.3, 0.12);
                            groundBurst(feet, 1.1f, 4);
                            handTo(from, chest);
                        });
                    }
                }
                case CAPTURE -> { // from = sitter's eyes, to[0] = the cushion under them
                    for (int i = 0; i < 2; i++) {
                        Vec3 top = from.add((R.nextDouble() - 0.5) * 1.6, 1.6 + R.nextDouble(), (R.nextDouble() - 0.5) * 1.6);
                        strike(top, from, 0.010f, 0.25, Style.NET, Kind.AIR, 0.06);
                    }
                    if (source != null) crawl(source, 2, 0.05);
                    if (!to.isEmpty()) groundBurst(to.get(0), 0.45f, 3);
                }
                case UPGRADE -> { // from = hand, to[0] = eyes
                    if (to.isEmpty()) return;
                    strike(from, to.get(0), 0.012f, 0.3, Style.NET, Kind.AIR, 0.05);
                    strike(from, to.get(0), 0.005f, 0.3, Style.PALE, Kind.AIR, 0.05);
                    later(0.12, () -> strike(from, to.get(0), 0.010f, 0.25, Style.NET, Kind.AIR, 0.04));
                    if (source != null) crawl(source, 6, 0.05);
                }
                case CAPTURE_END -> {
                    Vec3 top = from.add((R.nextDouble() - 0.5), 11, (R.nextDouble() - 0.5));
                    strike(top, from, 0.03f, 0.5, Style.BODY, Kind.AIR, 0.08);
                    skyBolt(from, 0.016f, 0.35, 0.12);
                    skyBolt(from, 0.016f, 0.35, 0.12);
                    if (source != null) crawl(source, 6, 0.03);
                    if (!to.isEmpty()) groundBurst(to.get(0), 1.4f, 6);
                }
            }
        } finally {
            centreFree = false;
        }
    }

    // ------------------------------------------------------------------ rendering

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        BOLTS.clear();
        PENDING.clear();
        ConstructTalk.reset();
        ClientPossession.clear();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (BOLTS.isEmpty() && PENDING.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            BOLTS.clear();
            PENDING.clear();
            return;
        }
        double now = sec();
        if (!PENDING.isEmpty()) {
            List<Pending> due = new ArrayList<>();
            for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
                Pending p = it.next();
                if (p.at() <= now) {
                    due.add(p);
                    it.remove();
                }
            }
            for (Pending p : due) p.action().run();
        }
        if (BOLTS.isEmpty()) return;

        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        double tan = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
        camF = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());
        camR = Vec3.directionFromRotation(0, camera.getYRot()).cross(new Vec3(0, 1, 0)).normalize();
        camU = camR.cross(camF).normalize();
        camTanY = tan;
        camTanX = tan * mc.getWindow().getWidth() / (double) Math.max(1, mc.getWindow().getHeight());
        long f = ++frame;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        mat = event.getPoseStack().last().pose();
        camPos = cam;
        for (Iterator<Bolt> it = BOLTS.iterator(); it.hasNext(); ) {
            Bolt bolt = it.next();
            double age = now - bolt.born;
            if (age > bolt.life) {
                it.remove();
                continue;
            }
            if (bolt.follow != null) {
                Vec3[] ends = bolt.follow.get();
                if (ends == null) {
                    it.remove();
                    continue;
                }
                bolt.a = ends[0];
                bolt.b = ends[1];
            }
            long shape = (f + (bolt.seed & 7)) / 2; // a new shape every second frame
            RandomSource rand = RandomSource.create(bolt.seed + shape * 7919L);
            double dist = Math.max(0.4, bolt.a.add(bolt.b).scale(0.5).distanceTo(cam));
            double scale = 2.0 * dist * tan; // world height of the view at that distance
            double grow = bolt.grow <= 0 ? 1 : Mth.clamp(age / bolt.grow, 0, 1);
            int color = bolt.style == Style.PALE ? PALE : RED;
            draw(bolt.a, bolt.b, bolt.width, bolt.style, bolt.kind, rand, cam, scale, grow, color, true, bolt.clearCentre);
        }
        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        buf = null;
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Is this point in the middle third of the screen? */
    private static boolean inCentre(Vec3 p, Vec3 cam) {
        Vec3 d = p.subtract(cam);
        double fwd = d.dot(camF);
        if (fwd <= 0.05) return false;
        return Math.abs(d.dot(camR) / fwd) < 0.33 * camTanX && Math.abs(d.dot(camU) / fwd) < 0.33 * camTanY;
    }

    private static void draw(Vec3 a, Vec3 b, float width, Style style, Kind kind, RandomSource r, Vec3 cam,
                             double scale, double grow, int color, boolean branches, boolean clearCentre) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 0.001) return;
        Vec3 dir = d.scale(1 / len);
        Vec3 side = kind == Kind.GROUND ? dir.cross(new Vec3(0, 1, 0)) : dir.cross(a.add(b).scale(0.5).subtract(cam));
        if (side.lengthSqr() < 1e-8) side = new Vec3(1, 0, 0);
        side = side.normalize();

        // step lengths (fractions of the view), chance of a long step, turn range in degrees, branch budget
        double shortStep, longStep, longChance, turnMin, turnMax;
        int maxBranches;
        switch (style) {
            case BODY -> { shortStep = 0.08; longStep = 0.25; longChance = 0.6; turnMin = 8; turnMax = 28; maxBranches = branches ? 1 : 0; }
            case HAND -> { shortStep = 0.05; longStep = 0.14; longChance = 0.35; turnMin = 15; turnMax = 45; maxBranches = branches ? 3 : 0; }
            case PALE -> { shortStep = 0.04; longStep = 0.1; longChance = 0.3; turnMin = 20; turnMax = 55; maxBranches = branches ? 2 : 0; }
            default -> { shortStep = 0.035; longStep = 0.095; longChance = 0.28; turnMin = 20; turnMax = 65; maxBranches = branches ? 8 : 0; }
        }

        // 2D path: x along the bolt, y sideways. Each step turns, but never more than 70 degrees off the target.
        List<double[]> pts = new ArrayList<>();
        double x = 0, y = 0, ang = 0;
        pts.add(new double[]{0, 0});
        int sign = r.nextBoolean() ? 1 : -1;
        for (int i = 0; i < 200; i++) {
            double remaining = Math.hypot(len - x, y);
            double step = (r.nextDouble() < longChance ? longStep * (0.8 + r.nextDouble() * 0.5)
                    : shortStep * (0.7 + r.nextDouble() * 0.6)) * scale;
            if (remaining < step * 1.3) {
                pts.add(new double[]{len, 0});
                break;
            }
            double target = Math.toDegrees(Math.atan2(-y, len - x));
            double turn = turnMin + r.nextDouble() * (turnMax - turnMin);
            if (r.nextFloat() < 0.75f) sign = -sign;
            double next = ang + sign * turn;
            if (Math.abs(next - target) > 55) next = ang + Math.signum(target - ang) * turn;
            ang = Mth.clamp(next, target - 70, target + 70);
            double rad = Math.toRadians(ang);
            x += Math.cos(rad) * step;
            y += Math.sin(rad) * step;
            pts.add(new double[]{x, y});
        }

        int n = pts.size();
        float[] w = new float[n];
        for (int i = 0; i < n; i++) {
            float roll = r.nextFloat();
            float m = roll < 0.22f ? 0.4f : roll < 0.8f ? 1f : 3f;
            w[i] = (float) (width * scale * m);
        }
        w[0] *= 0.35f;
        w[n - 1] = 0;

        double total = 0;
        for (int i = 1; i < n; i++) total += Math.hypot(pts.get(i)[0] - pts.get(i - 1)[0], pts.get(i)[1] - pts.get(i - 1)[1]);
        double shown = total * grow, walked = 0;
        int budget = maxBranches == 0 ? 0
                : Math.min(maxBranches, (int) Math.round(len / scale * (style == Style.NET ? 4.0 : 1.5) * r.nextDouble()));

        for (int i = 0; i + 1 < n; i++) {
            double[] p0 = pts.get(i), p1 = pts.get(i + 1);
            double seg = Math.hypot(p1[0] - p0[0], p1[1] - p0[1]);
            if (walked >= shown) break;
            double t = Math.min(1, (shown - walked) / Math.max(1e-6, seg));
            walked += seg;
            Vec3 s0 = a.add(dir.scale(p0[0])).add(side.scale(p0[1]));
            Vec3 s1 = a.add(dir.scale(p1[0])).add(side.scale(p1[1]));
            Vec3 end = s0.lerp(s1, t);
            float w0 = w[i];
            float w1 = t < 1 ? w[i] * 0.2f : w[i + 1];
            if (!clearCentre || !(inCentre(s0, cam) || inCentre(end, cam) || inCentre(s0.lerp(end, 0.5), cam))) {
                quad(s0, end, w0, w1, kind, cam, color);
            }
            if (budget > 0 && i > 0 && r.nextFloat() < 0.35f) {
                budget--;
                double bend = (r.nextBoolean() ? 1 : -1) * Math.toRadians(25 + r.nextDouble() * 40);
                Vec3 along = s1.subtract(s0).normalize();
                Vec3 bdir = along.scale(Math.cos(bend)).add(side.scale(Math.sin(bend)));
                Vec3 tip = s0.add(bdir.scale(scale * (0.09 + r.nextDouble() * 0.12)));
                draw(s0, tip, width * 0.55f, style, kind, r, cam, scale, 1, color, false, clearCentre);
            }
        }
    }

    /** One stretch of bolt: a quad (or a taper triangle when one end has no width), slightly overlapping its neighbours. */
    private static void quad(Vec3 a, Vec3 b, float wa, float wb, Kind kind, Vec3 cam, int color) {
        Vec3 d = b.subtract(a);
        if (d.lengthSqr() < 1e-12) return;
        Vec3 side = kind == Kind.GROUND ? d.cross(new Vec3(0, 1, 0)) : d.cross(a.add(b).scale(0.5).subtract(cam));
        if (side.lengthSqr() < 1e-14) return;
        side = side.normalize();
        Vec3 ext = d.normalize().scale(Math.max(wa, wb) * 0.3);
        Vec3 p = a.subtract(ext), q = b.add(ext);
        if (wa > 1e-4 && wb > 1e-4) {
            Vec3 p1 = p.add(side.scale(wa / 2)), p2 = p.subtract(side.scale(wa / 2));
            Vec3 q1 = q.add(side.scale(wb / 2)), q2 = q.subtract(side.scale(wb / 2));
            tri(p1, p2, q2, color);
            tri(p1, q2, q1, color);
        } else {
            float wide = Math.max(wa, wb);
            Vec3 base = wa >= wb ? p : q, tip = wa >= wb ? q : p;
            tri(base.add(side.scale(wide / 2)), base.subtract(side.scale(wide / 2)), tip, color);
        }
    }

    private static void tri(Vec3 a, Vec3 b, Vec3 c, int color) {
        if (buf == null) return;
        vertex(a, color);
        vertex(b, color);
        vertex(c, color);
    }

    private static void vertex(Vec3 v, int color) {
        buf.addVertex(mat, (float) (v.x - camPos.x), (float) (v.y - camPos.y), (float) (v.z - camPos.z)).setColor(color);
    }
}
