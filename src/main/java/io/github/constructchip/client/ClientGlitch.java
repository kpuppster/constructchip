package io.github.constructchip.client;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.ghost.GhostState;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

/**
 * Red "signal" look. A short scanline glitch over the screen when a hack lands or a chip connects, and a faint red
 * shimmer around your construct (only the host's client draws it; everyone else can't see the construct at all).
 */
@EventBusSubscriber(modid = ConstructChip.MODID, value = Dist.CLIENT)
public final class ClientGlitch {
    private static final DustParticleOptions SHIMMER = new DustParticleOptions(new Vector3f(1f, 0.1f, 0.15f), 0.6f);
    private static int flashTicks, flashTotal;
    private static boolean hadChip;

    private ClientGlitch() {}

    static void flash(int ticks) {
        if (ticks > flashTicks) {
            flashTicks = ticks;
            flashTotal = ticks;
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (flashTicks > 0) flashTicks--;
        boolean hasChip = ClientChipState.chip().present();
        if (hasChip && !hadChip) flash(20); // chip just connected (installed, or you became its construct)
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

    /** Drawn by the HUD layer. Horizontal red bands that jitter and fade out. */
    static void render(GuiGraphics g) {
        if (flashTicks <= 0 || flashTotal <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth(), h = g.guiHeight();
        float strength = flashTicks / (float) flashTotal;
        RandomSource r = RandomSource.create(mc.level == null ? 0 : mc.level.getGameTime() * 31L + flashTicks);
        int bands = 4 + (int) (strength * 8);
        for (int i = 0; i < bands; i++) {
            int y = r.nextInt(Math.max(1, h));
            int bh = 1 + r.nextInt(4);
            int x = r.nextInt(Math.max(1, w / 3)) - w / 6;
            int alpha = (int) (strength * (60 + r.nextInt(80)));
            g.fill(x, y, x + w, y + bh, (alpha << 24) | 0xFF1830);
        }
        g.fill(0, 0, w, h, ((int) (strength * 28) << 24) | 0xFF0020);
    }
}
