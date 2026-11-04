package io.github.constructchip.client;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.ghost.GhostState;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

/**
 * Red "signal" look. Ikeda's glitch post effect (Capri mod, used with permission): band shifts, color split, red wash
 * and a white-red flash when a hack lands or a chip connects, plus faint red noise at the screen edges the whole time
 * you are an installed construct (the voice in the host's head). A red shimmer around your construct is drawn on the
 * host's client only. If the shader can't run (another post effect is active), a simple scanline overlay stands in.
 */
@EventBusSubscriber(modid = ConstructChip.MODID, value = Dist.CLIENT)
public final class ClientGlitch {
    private static final DustParticleOptions SHIMMER = new DustParticleOptions(new Vector3f(1f, 0.1f, 0.15f), 0.6f);
    public static final ResourceLocation POST = ResourceLocation.fromNamespaceAndPath("constructchip", "shaders/post/capri_glitch.json");
    private static int flashTicks, flashTotal;
    private static float flashRed, flashWhite;
    private static boolean hadChip;

    private ClientGlitch() {}

    static void flash(int ticks, float red, float white) {
        if (ticks >= flashTicks) {
            flashTicks = ticks;
            flashTotal = ticks;
            flashRed = red;
            flashWhite = white;
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (flashTicks > 0) flashTicks--;
        boolean hasChip = ClientChipState.chip().present();
        if (hasChip && !hadChip) flash(24, 0.5f, 0.3f); // chip just connected (installed, or you became its construct)
        hadChip = hasChip;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        RandomSource r = mc.level.random;
        for (Map.Entry<UUID, UUID> link : GhostState.clientLinks().entrySet()) {
            if (!link.getValue().equals(mc.player.getUUID())) continue; // only my construct
            Player construct = mc.level.getPlayerByUUID(link.getKey());
            if (construct == null || r.nextInt(3) != 0) continue;
            mc.level.addParticle(SHIMMER, construct.getRandomX(0.6), construct.getRandomY(), construct.getRandomZ(0.6), 0, 0.02, 0);
        }
    }

    /** Called every frame from the HUD layer: drives the post effect, or draws the fallback overlay. */
    static void render(GuiGraphics g, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        GameRenderer renderer = mc.gameRenderer;
        float strength = flashTicks <= 0 || flashTotal <= 0 ? 0f : Math.max(0f, (flashTicks - partialTick) / flashTotal);
        float glitch = strength;
        float red = strength * flashRed;
        float white = flashTotal - flashTicks < 2 ? flashWhite * strength : 0f;
        float edge = ClientChipState.isConstruct() ? 0.35f : 0f;
        boolean wanted = glitch > 0.01f || red > 0.01f || white > 0.01f || edge > 0f;

        PostChain current = renderer.currentEffect();
        boolean ours = current != null && POST.toString().equals(current.getName());
        if (wanted && current == null) {
            renderer.loadEffect(POST);
            current = renderer.currentEffect();
            ours = current != null && POST.toString().equals(current.getName());
        } else if (!wanted && ours) {
            renderer.shutdownEffect();
            return;
        }
        if (ours) {
            float frame = mc.level == null ? 0f : (mc.level.getGameTime() + partialTick) * 1.2f;
            for (PostPass pass : current.passes) {
                EffectInstance fx = pass.getEffect();
                fx.safeGetUniform("Glitch").set(glitch);
                fx.safeGetUniform("Red").set(red);
                fx.safeGetUniform("Edge").set(edge);
                fx.safeGetUniform("Flash").set(white);
                fx.safeGetUniform("Impact").set(0f);
                fx.safeGetUniform("Shake").set(strength * flashRed * 0.6f);
                fx.safeGetUniform("Frame").set((float) Math.floor(frame));
            }
        } else if (strength > 0f) {
            drawFallback(g, strength);
        }
    }

    private static void drawFallback(GuiGraphics g, float strength) {
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth(), h = g.guiHeight();
        RandomSource r = RandomSource.create(mc.level == null ? 0 : mc.level.getGameTime() * 31L + flashTicks);
        int bands = 4 + (int) (strength * 8);
        for (int i = 0; i < bands; i++) {
            int y = r.nextInt(Math.max(1, h));
            int bh = 1 + r.nextInt(4);
            int x = r.nextInt(Math.max(1, w / 3)) - w / 6;
            int alpha = (int) (strength * (60 + r.nextInt(80)));
            g.fill(x, y, x + w, y + bh, (alpha << 24) | 0xFF1830);
        }
    }
}
