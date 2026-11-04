package io.github.constructchip.client;

import io.github.constructchip.network.ConsentRequestPayload;
import io.github.constructchip.network.ServerboundPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/** "Do you agree?" Accept or Decline. Closing (Esc) or the timeout counts as Decline. */
public class ConsentScreen extends Screen {
    private final ConsentRequestPayload request;
    private final long openedAt;
    private boolean answered;

    public ConsentScreen(ConsentRequestPayload request) {
        super(request.title());
        this.request = request;
        this.openedAt = ClientChipState.now();
    }

    @Override
    protected void init() {
        int y = height / 2 + 30;
        addRenderableWidget(Button.builder(Component.translatable("gui.constructchip.consent.accept"), b -> answer(true))
                .bounds(width / 2 - 104, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.constructchip.consent.decline"), b -> answer(false))
                .bounds(width / 2 + 4, y, 100, 20).build());
    }

    private void answer(boolean accept) {
        if (answered) return;
        answered = true;
        PacketDistributor.sendToServer(new ServerboundPayloads.ConsentReply(request.requestId(), accept));
        onClose();
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
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int pw = Math.min(360, width - 20);
        g.blitSprite(ChipArt.PANEL, width / 2 - pw / 2, height / 2 - 112, pw, 196);
        g.blit(ChipArt.ART, width / 2 - 32, height / 2 - 106, 0, 0, 48, 48, 48, 48);
        g.drawCenteredString(font, title, width / 2, height / 2 - 50, 0xFFF90759);
        int y = height / 2 - 30;
        for (FormattedCharSequence line : font.split(request.body(), Math.min(320, width - 40))) {
            g.drawString(font, line, width / 2 - font.width(line) / 2, y, 0xFF303030, false);
            y += 10;
        }
        long left = Math.max(0, request.timeoutTicks() - (ClientChipState.now() - openedAt)) / 20;
        g.drawCenteredString(font, Component.translatable("gui.constructchip.consent.timeout", left), width / 2, height / 2 + 56, 0xFF7A1020);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
