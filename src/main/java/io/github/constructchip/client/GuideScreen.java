package io.github.constructchip.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.constructchip.ConstructChip;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/**
 * Edgerunner's Guide: chapters → entries → pages (text by A24-N22, used with permission; adjusted for this mod).
 * Pages use **bold** markup. Same red/cyan menu look as the consent screen.
 */
public class GuideScreen extends Screen {
    private record Entry(String category, String id, int pages) {}

    private final List<String> categories = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private String category;
    private Entry open;
    private int page;

    public GuideScreen() {
        super(Component.translatable("item.constructchip.guide"));
        try (var in = minecraftResource()) {
            JsonArray index = JsonParser.parseReader(in).getAsJsonArray();
            for (JsonElement c : index) {
                JsonObject cat = c.getAsJsonObject();
                String id = cat.get("id").getAsString();
                categories.add(id);
                for (JsonElement e : cat.getAsJsonArray("entries")) {
                    JsonObject o = e.getAsJsonObject();
                    entries.add(new Entry(id, o.get("id").getAsString(), o.get("pages").getAsInt()));
                }
            }
        } catch (Exception e) {
            ConstructChip.LOGGER.warn("Guide index missing", e);
        }
        if (!categories.isEmpty()) category = categories.get(0);
    }

    private static InputStreamReader minecraftResource() throws java.io.IOException {
        var res = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResourceOrThrow(ConstructChip.id("guide/index.json"));
        return new InputStreamReader(res.open(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private int panelX() { return Math.max(10, width / 2 - 190); }

    @Override
    protected void init() {
        clearWidgets();
        int x = panelX(), y = 34;
        for (String c : categories) {
            addRenderableWidget(Button.builder(Component.translatable("guide.constructchip." + c), b -> { category = c; open = null; init(); })
                    .bounds(x, y, 110, 16).build());
            y += 18;
        }
        if (open == null) {
            int ex = x + 120, ey = 34;
            for (Entry e : entries) {
                if (!e.category().equals(category)) continue;
                if (ey > height - 30) break;
                addRenderableWidget(Button.builder(Component.translatable("guide.constructchip." + e.category() + "." + e.id()), b -> { open = e; page = 0; init(); })
                        .bounds(ex, ey, 240, 14).build());
                ey += 15;
            }
        } else {
            int bx = x + 120, by = height - 28;
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { page = Math.max(0, page - 1); }).bounds(bx, by, 20, 16).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { page = Math.min(open.pages() - 1, page + 1); }).bounds(bx + 24, by, 20, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.constructchip.guide.back"), b -> { open = null; init(); }).bounds(bx + 50, by, 50, 16).build());
        }
    }

    /** "**bold**" markup → styled component; newlines kept for wrapping. */
    static MutableComponent markup(String text) {
        MutableComponent out = Component.empty();
        String[] parts = text.split("\\*\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            MutableComponent c = Component.literal(parts[i]);
            if (i % 2 == 1) c = c.withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);
            out.append(c);
        }
        return out;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xF00A0608);
        int x = panelX();
        g.fillGradient(x - 6, 0, x + 114, height, 0xC03A0C12, 0xC0240810);
        g.fill(x + 114, 0, x + 115, height, 0xAA9B1C28);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = panelX();
        g.drawString(font, Component.literal(title.getString().toUpperCase()).withStyle(s -> s.withFont(HudLayer.HUD_FONT)), x, 12, 0xFFF3E600, false);
        if (open == null) return;
        String base = "guide.constructchip." + open.category() + "." + open.id() + ".p" + page;
        int tx = x + 120, ty = 34, tw = Math.min(260, width - tx - 10);
        g.drawString(font, Component.translatable(base + ".title").withStyle(ChatFormatting.BOLD), tx, ty, 0xFF5EF6FF, false);
        ty += 14;
        String text = Component.translatable(base + ".text").getString();
        for (String para : text.split("\n")) {
            for (FormattedCharSequence line : font.split(markup(para), tw)) {
                g.drawString(font, line, tx, ty, 0xFFE8B4B4, false);
                ty += 10;
            }
            if (para.isEmpty()) ty += 2;
        }
        g.drawString(font, (page + 1) + " / " + open.pages(), tx + 110, height - 24, 0xFF888888, false);
    }

    @Override public boolean isPauseScreen() { return false; }
}
