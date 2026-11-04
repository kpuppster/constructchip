package io.github.constructchip.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.constructchip.network.ConsentRequestPayload;
import io.github.constructchip.network.ServerboundPayloads;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * "Do you consent?" in the style of a cyberpunk main menu: dark red side panel with thin frame lines, a big yellow
 * title with a cyan glitch ghost, red uppercase options, and the selected option boxed in cyan. Mouse or keyboard
 * (up/down + Enter). Esc, closing, or the timeout counts as Decline.
 */
public class ConsentScreen extends Screen {
    private static final ResourceLocation FONT = HudLayer.HUD_FONT;
    private static final int RED = 0xFFFF4A4A, RED_DIM = 0xFF7A1A22, RED_LINE = 0xAA9B1C28;
    private static final int CYAN = 0xFF5EF6FF, CYAN_FILL = 0x3326C6D0, YELLOW = 0xFFF3E600;
    private static final String[] OPTIONS = {"gui.constructchip.consent.accept", "gui.constructchip.consent.decline"};

    private final ConsentRequestPayload request;
    private final long openedAt;
    private boolean answered;
    private int selected = 1; // default to Decline: consent must be a deliberate choice
    private final String[] noise = new String[6];

    public ConsentScreen(ConsentRequestPayload request) {
        super(request.title());
        this.request = request;
        this.openedAt = ClientChipState.now();
        RandomSource r = RandomSource.create(request.requestId() * 7919L);
        for (int i = 0; i < noise.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 6; j++) sb.append(String.format(Locale.ROOT, "%04X", r.nextInt(0x10000))).append(j == 2 ? "  //  " : " ");
            noise[i] = sb.toString().trim();
        }
    }

    private static MutableComponent styled(Component c) {
        return c.copy().withStyle(s -> s.withFont(FONT));
    }

    private static MutableComponent upper(String key) {
        return styled(Component.literal(Component.translatable(key).getString().toUpperCase(Locale.ROOT)));
    }

    // ------------------------------------------------------------ layout

    private int panelRight() { return Math.max(220, (int) (width * 0.42f)); }
    private int left() { return Math.max(24, (int) (width * 0.09f)); }
    private int optionsTop() { return height / 2 + 14; }
    private int optionWidth() { return Math.min(200, panelRight() - left() - 20); }

    private int optionAt(double mx, double my) {
        int x = left() - 6;
        for (int i = 0; i < OPTIONS.length; i++) {
            int y = optionsTop() + i * 22 - 5;
            if (mx >= x && mx < x + optionWidth() && my >= y && my < y + 18) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------ input

    private void answer(boolean accept) {
        if (answered) return;
        answered = true;
        UiSounds.blip();
        PacketDistributor.sendToServer(new ServerboundPayloads.ConsentReply(request.requestId(), accept));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int i = optionAt(mx, my);
        if (button == 0 && i >= 0) {
            answer(i == 0);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        int i = optionAt(mx, my);
        if (i >= 0 && i != selected) {
            selected = i;
            UiSounds.hover();
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_S) {
            selected = 1 - selected;
            UiSounds.hover();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            answer(selected == 0);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void tick() {
        if (ClientChipState.now() - openedAt > request.timeoutTicks()) answer(false);
    }

    @Override
    public void removed() {
        if (!answered) {
            answered = true;
            PacketDistributor.sendToServer(new ServerboundPayloads.ConsentReply(request.requestId(), false));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------ drawing

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // full-screen dark, with a red-tinted side panel fading out to the right
        g.fill(0, 0, width, height, 0xF00A0608);
        int pr = panelRight();
        g.fillGradient(0, 0, pr, height, 0xC03A0C12, 0xC0240810);
        for (int x = pr; x < pr + 60; x += 4) {
            int a = (int) (0x60 * (1f - (x - pr) / 60f));
            g.fill(x, 0, x + 4, height, (a << 24) | 0x2A0A10);
        }
        // faint horizontal scanlines over everything
        for (int y = 0; y < height; y += 3) g.fill(0, y, width, y + 1, 0x14000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        long now = ClientChipState.now();
        int pr = panelRight(), l = left();

        // chip art as the backdrop on the right, faded and tinted
        int art = Math.min(height - 40, width - pr - 40);
        if (art > 64) {
            RenderSystem.enableBlend();
            g.setColor(0.55f, 0.2f, 0.35f, 0.35f);
            g.blit(ChipArt.ART, pr + (width - pr - art) / 2, (height - art) / 2, 0, 0, art, art, art, art);
            g.setColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }

        // frame lines and corner data, like a terminal readout
        g.fill(pr, 0, pr + 1, height, RED_LINE);
        g.fill(10, 0, 11, height, RED_LINE);
        g.fill(width - 11, 0, width - 10, height, 0x559B1C28);
        g.renderOutline(l - 8, 14, 150, 11, RED_LINE);
        g.drawString(font, styled(Component.literal(noise[0])), l - 5, 16, RED_DIM, false);
        g.drawString(font, styled(Component.literal(noise[1])), l - 5, 30, RED_DIM, false);
        int warnW = Math.min(240, width / 3);
        g.renderOutline(width - warnW - 20, 14, warnW, 14, RED_LINE);
        g.drawString(font, styled(Component.literal("⚠ " + noise[2])), width - warnW - 16, 17, RED_DIM, false);
        g.drawString(font, styled(Component.literal(noise[3])), pr + 12, 18, RED_DIM, false);

        // title: big yellow, with a jittering cyan ghost
        int titleY = height / 2 - 78;
        MutableComponent title = styled(Component.literal(this.title.getString().toUpperCase(Locale.ROOT)));
        float scale = Mth.clamp((pr - l - 20) / (float) Math.max(1, font.width(title)), 1.5f, 3f);
        RandomSource jitter = RandomSource.create(now / 3);
        g.pose().pushPose();
        g.pose().translate(l, titleY, 0);
        g.pose().scale(scale, scale, 1f);
        int gx = jitter.nextInt(4) == 0 ? jitter.nextInt(3) - 1 : 0;
        g.drawString(font, title, 1 + gx, 1, 0x8800E5FF, false);
        g.drawString(font, title, 0, 0, YELLOW, false);
        g.pose().popPose();
        int underY = titleY + (int) (font.lineHeight * scale) + 3;
        g.fill(l, underY, l + (int) (font.width(title) * scale * 0.6f), underY + 1, 0xFFB8AE00);

        // body text
        int y = underY + 10;
        List<FormattedCharSequence> lines = font.split(styled(request.body()), pr - l - 24);
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, l, y, 0xFFE8B4B4, false);
            y += 11;
        }

        // options: red uppercase; the selected one boxed in cyan with an icon square on its right
        for (int i = 0; i < OPTIONS.length; i++) {
            int oy = optionsTop() + i * 22;
            MutableComponent text = upper(OPTIONS[i]);
            if (i == selected) {
                int bx = l - 6, bw = optionWidth();
                g.fill(bx, oy - 5, bx + bw, oy + 13, CYAN_FILL);
                g.renderOutline(bx, oy - 5, bw, 18, CYAN);
                int ix = bx + Math.min(110, bw - 40);
                g.fill(ix, oy - 5, ix + 18, oy + 13, 0x6626C6D0);
                g.renderOutline(ix, oy - 5, 18, 18, CYAN);
                g.fill(ix + 5, oy + 3, ix + 13, oy + 5, CYAN);
                for (int k = 0; k < 3; k++) g.fill(bx + bw - 22, oy - 1 + k * 3, bx + bw - 8, oy + k * 3, 0x995EF6FF);
                g.drawString(font, text, l, oy, CYAN, false);
            } else {
                g.drawString(font, text, l, oy, RED, false);
            }
        }

        // footer: countdown like a version number, and a "Select" hint bottom-right
        int footY = height - 42;
        g.fill(l - 8, footY, pr - 10, footY + 1, RED_LINE);
        g.drawString(font, styled(Component.literal(noise[4])), l - 5, footY + 4, RED_DIM, false);
        long left = Math.max(0, request.timeoutTicks() - (now - openedAt)) / 20;
        g.drawString(font, styled(Component.translatable("gui.constructchip.consent.timeout", left)), l - 5, footY + 16, 0xFFD06060, false);
        MutableComponent hint = styled(Component.translatable("gui.constructchip.consent.hint"));
        int hx = width - 24 - font.width(hint);
        g.renderOutline(hx - 14, height - 26, 9, 12, CYAN);
        g.fill(hx - 10, height - 24, hx - 9, height - 20, CYAN);
        g.drawString(font, hint, hx, height - 24, CYAN, false);
    }
}
