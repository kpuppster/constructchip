package io.github.constructchip.client;

import io.github.constructchip.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The speech box the construct talks to its host through, styled after Capri's dialogue box (Ikeda's Capri mod,
 * used with permission): a dark cut-corner panel with a salmon frame, the speaker's name in a tab with a red/cyan
 * ghost and "// LINK", and text that types out with each new letter briefly flickering through scramble glyphs.
 * Here the name is the construct player's. Only the host sees it (the server sends it to nobody else).
 */
public final class ConstructTalk {
    private ConstructTalk() {}

    static final int SALMON = 0xFF5F55, CYAN = 0x5EF6FF, PANEL = 0x0B0B12, TEXT = 0xF2F2F2;
    static final float PANEL_ALPHA = 0.72f;
    static final int TAB_H = 9, LINE_H = 10, PAD_L = 5, PAD_R = 9, MAX_LINES = 6;
    /** How long a just-revealed letter keeps flickering through scramble glyphs. */
    private static final double DECODE = 1 / 12.0;
    private static final double PER_CHAR = 0.028, FADE_IN = 0.04, FADE_OUT = 0.4;
    private static final String SCRAMBLE = "#%&*+=<>/\\|?!$@0123456789ABCDEFHKMNRSTXZ";

    private record Line(String name, String text) {}

    private static final Deque<Line> QUEUE = new ArrayDeque<>();
    private static Line current;
    private static double start, revealEnd, end, lastBlip;

    static double sec() {
        return net.minecraft.Util.getNanos() / 1.0e9;
    }

    static void say(String name, String text) {
        text = text.strip();
        if (text.isEmpty()) return;
        if (QUEUE.size() >= 8) QUEUE.pollFirst();
        QUEUE.addLast(new Line(name, text));
        if (current == null) next();
    }

    private static void next() {
        current = QUEUE.pollFirst();
        if (current == null) return;
        start = sec();
        int n = current.text().codePointCount(0, current.text().length());
        revealEnd = start + n * PER_CHAR;
        end = revealEnd + 2.5 + n * 0.045; // hold long enough to read
        lastBlip = -1;
    }

    private static MutableComponent hud(String s) {
        return Component.literal(s).withStyle(st -> st.withFont(HudLayer.HUD_FONT));
    }

    private static int ca(int rgb, float alpha) {
        return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (current == null) return;
        double now = sec();
        if (now > end) {
            next();
            if (current == null) return;
            now = sec();
        }
        // a new line interrupts nothing: wait in the queue, but shorten the hold of the current one
        if (!QUEUE.isEmpty() && now > revealEnd + 1.2) end = Math.min(end, now + FADE_OUT);
        if (mc.options.hideGui || mc.player == null || mc.screen instanceof ChatScreen) return;
        double age = now - start;
        float alpha = (float) (Mth.clamp((end - now) / FADE_OUT, 0, 1) * Mth.clamp(age / FADE_IN, 0, 1));
        if (alpha <= 0.01f) return;

        // speaking blips while the text types out
        if (now < revealEnd && now - lastBlip > 0.1) {
            lastBlip = now;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BLIP.get(), 0.9f + mc.level.random.nextFloat() * 0.25f, 0.25f));
        }

        Font font = mc.font;
        int sw = g.guiWidth(), sh = g.guiHeight();
        int chatRight = (int) Math.ceil(ChatComponent.getWidth(mc.options.chatWidth().get()) * mc.options.chatScale().get()) + 14;
        boolean beside = sw - chatRight >= 200;
        int maxText = beside ? Math.min(320, sw - chatRight - 70) : Math.min(320, sw - 40);

        String name = current.name().toUpperCase(Locale.ROOT);
        List<String> lines = new ArrayList<>();
        for (FormattedText part : font.getSplitter().splitLines(current.text(), maxText, net.minecraft.network.chat.Style.EMPTY)) {
            lines.add(part.getString());
        }
        if (lines.isEmpty()) return;
        int widest = 0;
        for (String l : lines) widest = Math.max(widest, font.width(l));
        int nameW = font.width(hud(name)), linkW = font.width(hud(" // LINK"));
        int tabW = 4 + nameW + linkW + 7;
        int w = Math.max(PAD_L + widest + PAD_R, tabW + 10);

        // how much has been revealed (in characters of the full text)
        int shown = (int) Math.min(current.text().length(), Math.floor(age / PER_CHAR));
        // lines visible: the last MAX_LINES lines that have started typing
        int[] lineStart = new int[lines.size()];
        int from = 0, lastStarted = 0;
        for (int i = 0; i < lines.size(); i++) {
            int at = current.text().indexOf(lines.get(i), from);
            lineStart[i] = at < 0 ? from : at;
            from = lineStart[i] + lines.get(i).length();
            if (lineStart[i] <= shown) lastStarted = i;
        }
        int visible = Math.min(MAX_LINES, lines.size());
        int first = Mth.clamp(lastStarted - visible + 1, 0, lines.size() - visible);
        int h = TAB_H + visible * LINE_H + 8;

        int x = beside ? chatRight : (sw - w) / 2;
        int bottom = beside ? sh - 44 : sh - 72;
        int y = Math.max(2, bottom - h);

        // panel with cut corners (top right, bottom left), then the frame
        int panel = ca(PANEL, PANEL_ALPHA * alpha), frame = ca(SALMON, 0.95f * alpha);
        int cut = 5;
        cutFill(g, x, y, w, h, cut, panel);
        cutFrame(g, x, y, w, h, cut, frame);

        // name tab: red and cyan ghosts behind the name, then "// LINK"
        int tx = x + PAD_L, ty = y + 2;
        g.drawString(font, hud(name), tx + 1, ty, ca(SALMON, 0.8f * alpha), false);
        g.drawString(font, hud(name), tx - 1, ty, ca(CYAN, 0.8f * alpha), false);
        g.drawString(font, hud(name), tx, ty, ca(0xFFFFFF, alpha), false);
        g.drawString(font, hud(" // LINK"), tx + nameW, ty, ca(SALMON, 0.6f * alpha), false);
        int sepY = y + TAB_H + 2;
        g.fill(x + PAD_L, sepY, x + w - PAD_R, sepY + 1, ca(SALMON, 0.45f * alpha));
        diamond(g, x + w - PAD_R + 3, sepY, ca(SALMON, 0.95f * alpha));

        // text: revealed letters in white, the newest few flickering through scramble glyphs
        RandomSource glyphs = RandomSource.create((long) (now * 24) * 7919L + 104729L);
        for (int i = 0; i < visible; i++) {
            int li = first + i;
            String line = lines.get(li);
            int base = lineStart[li];
            int ly = sepY + 4 + i * LINE_H;
            int lx = x + PAD_L;
            for (int c = 0; c < line.length(); c++) {
                int idx = base + c;
                if (idx >= shown) break;
                double charAge = age - idx * PER_CHAR;
                String ch = String.valueOf(line.charAt(c));
                int color = ca(TEXT, alpha);
                if (charAge < DECODE && !Character.isWhitespace(line.charAt(c))) {
                    ch = String.valueOf(SCRAMBLE.charAt(glyphs.nextInt(SCRAMBLE.length())));
                    color = ca(SALMON, alpha);
                }
                g.drawString(font, ch, lx, ly, color, false);
                lx += font.width(String.valueOf(line.charAt(c)));
            }
        }
    }

    /** Filled rectangle with the top-right and bottom-left corners cut off at 45 degrees. */
    private static void cutFill(GuiGraphics g, int x, int y, int w, int h, int cut, int color) {
        for (int i = 0; i < cut; i++) {
            g.fill(x, y + i, x + w - cut + i, y + i + 1, color);              // top rows, shortened on the right
            g.fill(x + cut - i, y + h - 1 - i, x + w, y + h - i, color);       // bottom rows, shortened on the left
        }
        g.fill(x, y + cut, x + w, y + h - cut, color);
    }

    private static void cutFrame(GuiGraphics g, int x, int y, int w, int h, int cut, int color) {
        g.fill(x, y, x + w - cut, y + 1, color);                 // top
        g.fill(x + cut, y + h - 1, x + w, y + h, color);         // bottom
        g.fill(x, y, x + 1, y + h - cut, color);                 // left
        g.fill(x + w - 1, y + cut, x + w, y + h, color);         // right
        for (int i = 0; i < cut; i++) {
            g.fill(x + w - cut + i, y + i, x + w - cut + i + 1, y + i + 1, color);  // top-right diagonal
            g.fill(x + i, y + h - cut + i, x + i + 1, y + h - cut + i + 1, color);  // bottom-left diagonal
        }
    }

    private static void diamond(GuiGraphics g, int cx, int cy, int color) {
        g.fill(cx - 1, cy - 2, cx + 1, cy - 1, color);
        g.fill(cx - 2, cy - 1, cx + 2, cy + 1, color);
        g.fill(cx - 1, cy + 1, cx + 1, cy + 2, color);
    }

    static void reset() {
        QUEUE.clear();
        current = null;
    }
}
