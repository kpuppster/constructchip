package io.github.constructchip.client;

import io.github.constructchip.chip.ChipRam;
import io.github.constructchip.chip.ChipTimers;
import io.github.constructchip.hack.ModQuickhacks;
import io.github.constructchip.hack.Quickhack;
import io.github.constructchip.network.ChipStatusPayload;
import io.github.constructchip.network.HostStatusPayload;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

/**
 * Top-left HUD. Construct: host vitals, RAM bar, selected quickhack with cost and cooldown, Overclock and lockout
 * timers, plus (Vitals Link) host effects and gear about to break. Host: a one-line chip summary.
 */
public final class HudLayer {
    private HudLayer() {}

    public static Component hackName(ResourceLocation hack) {
        return Component.translatable("quickhack." + hack.getNamespace() + "." + hack.getPath());
    }

    /** Rajdhani (SIL Open Font License, see licenses/OFL-Rajdhani.txt in the jar), with vanilla as fallback. */
    public static final ResourceLocation HUD_FONT = ResourceLocation.fromNamespaceAndPath("constructchip", "hud");

    private static Component hud(Component c) {
        return c.copy().withStyle(s -> s.withFont(HUD_FONT));
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        ClientGlitch.render(g, delta.getGameTimeDeltaPartialTick(false));
        if (mc.options.hideGui || mc.player == null) return;
        ChipStatusPayload chip = ClientChipState.chip();
        if (!chip.present()) return;
        Font font = mc.font;
        long now = ClientChipState.now();
        ChipRam ram = chip.ram();
        float current = ram.current(now);
        int x = 6, y = 6;

        if (chip.isHost()) {
            g.drawString(font, hud(Component.translatable("hud.constructchip.host_line", chip.constructName(),
                    String.format(Locale.ROOT, "%.1f", current), ram.max(), chip.skills().tier(), chip.skills().points())), x, y, 0xFF55FFFF);
            return;
        }

        g.fill(x - 3, y - 3, x + 150, y + 60, 0x88000000);
        g.fill(x - 3, y - 3, x - 2, y + 60, 0xFFE0283C);
        g.drawString(font, hud(Component.translatable("hud.constructchip.host", chip.hostName())), x, y, 0xFF55FFFF);
        y += 11;
        HostStatusPayload host = ClientChipState.host();
        if (host != null) {
            g.drawString(font, hud(Component.translatable("hud.constructchip.vitals",
                    String.format(Locale.ROOT, "%.0f", host.health()), String.format(Locale.ROOT, "%.0f", host.maxHealth()),
                    host.food(), host.armor())), x, y, host.health() <= host.maxHealth() * 0.3f ? 0xFFFF5555 : 0xFFFFFFFF);
        }
        y += 11;

        // RAM bar
        int barW = 100;
        g.fill(x, y, x + barW, y + 7, 0xFF202830);
        int filled = ram.max() <= 0 ? 0 : Math.round(barW * current / ram.max());
        g.fill(x, y, x + filled, y + 7, 0xFFE0283C); // red RAM bar
        g.drawString(font, String.format(Locale.ROOT, "%.1f/%d", current, ram.max()), x + barW + 4, y, 0xFFFFFFFF);
        y += 11;

        // Selected hack
        ResourceLocation selected = ClientChipState.selectedHack();
        ChipTimers timers = chip.timers();
        if (selected != null) {
            Quickhack hack = ModQuickhacks.REGISTRY.get(selected);
            float cost = hack == null ? 0f : hack.baseRamCost() * chip.ramCostMultiplier() * (timers.isOverclocked(now) ? 0.5f : 1f);
            long cd = timers.cooldownRemaining(selected, now);
            Component line = Component.translatable("hud.constructchip.hack", hackName(selected), String.format(Locale.ROOT, "%.1f", cost));
            if (cd > 0) line = line.copy().append(Component.literal(String.format(Locale.ROOT, " (%.1fs)", cd / 20f)).withStyle(ChatFormatting.GRAY));
            g.drawString(font, hud(line), x, y, current >= cost && cd <= 0 ? 0xFFFFFFFF : 0xFFAAAAAA);
        } else {
            g.drawString(font, hud(Component.translatable("message.constructchip.no_hacks")), x, y, 0xFFAAAAAA);
        }
        y += 11;
        if (timers.isOverclocked(now)) {
            g.drawString(font, hud(Component.translatable("hud.constructchip.overclock", (timers.overclockUntil() - now) / 20)), x, y, 0xFFFFAA00);
        } else if (timers.isLockedOut(now)) {
            g.drawString(font, hud(Component.translatable("hud.constructchip.lockout", (timers.lockoutUntil() - now) / 20)), x, y, 0xFFFF5555);
        }
        y += 14;

        // Vitals Link extras
        if (host != null) {
            for (MobEffectInstance effect : host.effects()) {
                g.drawString(font, hud(Component.translatable(effect.getDescriptionId()).copy()
                        .append(" " + effect.getDuration() / 20 + "s")), x, y, 0xFFCCCCFF);
                y += 10;
            }
            for (ItemStack worn : host.wornOut()) {
                g.drawString(font, hud(Component.translatable("hud.constructchip.worn", worn.getHoverName())), x, y, 0xFFFF7755);
                y += 10;
            }
        }
    }
}
