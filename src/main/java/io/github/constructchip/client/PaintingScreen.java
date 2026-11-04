package io.github.constructchip.client;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.constructchip.ConstructChip;
import io.github.constructchip.network.PaintPayload;
import io.github.constructchip.paint.Paint;
import io.github.constructchip.paint.PaintingMenu;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Painting station editor. Put an implant in the slot, pick one of its model textures, and paint the texture with
 * brush (sizes 1-3), bucket fill, eraser (back to the original) or eyedropper. 16-colour palette, undo, Save writes to
 * the item, Revert reloads it. The result shows on the 3D implant on your body.
 */
public class PaintingScreen extends AbstractContainerScreen<PaintingMenu> {
    private enum Tool { BRUSH, BUCKET, ERASER, EYEDROPPER }

    private static final int CANVAS = 152, CX = 48, CY = 18;
    private static final ResourceLocation CANVAS_ID = ConstructChip.id("painting_canvas");

    private ItemStack loadedFor = ItemStack.EMPTY;
    private List<String> textures = List.of();
    private int texIndex;
    private int[] original, pixels; // ARGB
    private int w, h;
    private DynamicTexture canvas;
    private Tool tool = Tool.BRUSH;
    private int brush = 1;
    private int color = 0xFFF90759;
    private final Deque<int[]> undo = new ArrayDeque<>();

    public PaintingScreen(PaintingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 216;
        imageHeight = 260;
        inventoryLabelY = 167;
        inventoryLabelX = 48;
    }

    @Override
    protected void init() {
        super.init();
        int bx = leftPos + 4, by = topPos + 40;
        String[] names = {"brush", "bucket", "eraser", "eyedropper"};
        for (int i = 0; i < 4; i++) {
            Tool t = Tool.values()[i];
            addRenderableWidget(Button.builder(Component.translatable("gui.constructchip.paint." + names[i]), b -> tool = t)
                    .bounds(bx, by + i * 16, 40, 14).build());
        }
        addRenderableWidget(Button.builder(Component.literal("1/2/3"), b -> brush = brush % 3 + 1).bounds(bx, by + 66, 40, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.constructchip.paint.undo"), b -> undo()).bounds(bx, by + 82, 40, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.constructchip.paint.save"), b -> save()).bounds(bx, by + 98, 40, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.constructchip.paint.revert"), b -> { loadedFor = ItemStack.EMPTY; reload(); })
                .bounds(bx, by + 114, 40, 14).build());
        addRenderableWidget(Button.builder(Component.literal("<>"), b -> { if (!textures.isEmpty()) { texIndex = (texIndex + 1) % textures.size(); loadedFor = ItemStack.EMPTY; } })
                .bounds(bx, by + 130, 40, 14).build());
    }

    private void reload() {
        ItemStack stack = menu.implant();
        if (ItemStack.isSameItemSameComponents(stack, loadedFor) && pixels != null) return;
        loadedFor = stack.copy();
        undo.clear();
        textures = Paint.textures(stack);
        if (textures.isEmpty()) {
            pixels = null;
            return;
        }
        texIndex = Math.min(texIndex, textures.size() - 1);
        String name = textures.get(texIndex);
        NativeImage base = PaintTextures.load(ConstructChip.id("textures/models/implant/" + name + ".png"));
        if (base == null) {
            pixels = null;
            return;
        }
        w = base.getWidth();
        h = base.getHeight();
        original = new int[w * h];
        for (int i = 0; i < original.length; i++) original[i] = PaintTextures.abgrToArgb(base.getPixelRGBA(i % w, i / w));
        base.close();
        pixels = original.clone();
        int[] diff = Paint.decode(Paint.of(stack).getOrDefault(name, ""), w * h);
        for (int i = 0; i < diff.length; i++) if ((diff[i] >>> 24) != 0) pixels[i] = diff[i];
        upload();
    }

    private void upload() {
        if (canvas == null || canvas.getPixels() == null || canvas.getPixels().getWidth() != w || canvas.getPixels().getHeight() != h) {
            if (canvas != null) canvas.close();
            canvas = new DynamicTexture(w, h, false);
            minecraft.getTextureManager().register(CANVAS_ID, canvas);
        }
        NativeImage img = canvas.getPixels();
        for (int i = 0; i < pixels.length; i++) img.setPixelRGBA(i % w, i / w, PaintTextures.argbToAbgr(pixels[i]));
        canvas.upload();
    }

    private void pushUndo() {
        undo.push(pixels.clone());
        while (undo.size() > 30) undo.removeLast();
    }

    private void undo() {
        if (!undo.isEmpty() && pixels != null) {
            pixels = undo.pop();
            upload();
        }
    }

    private void save() {
        if (pixels == null || textures.isEmpty()) return;
        int[] diff = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) if (pixels[i] != original[i]) diff[i] = pixels[i] | 0xFF000000;
        String data = Paint.encode(diff);
        if (data.length() > Paint.MAX_LENGTH) {
            minecraft.player.displayClientMessage(Component.translatable("gui.constructchip.paint.too_big"), true);
            return;
        }
        PacketDistributor.sendToServer(new PaintPayload(textures.get(texIndex), data));
        UiSounds.blip();
    }

    private int scale() { return Math.max(1, CANVAS / Math.max(w, h)); }

    private boolean paintAt(double mx, double my, boolean start) {
        if (pixels == null) return false;
        int s = scale();
        int px = (int) ((mx - leftPos - CX) / s), py = (int) ((my - topPos - CY) / s);
        if (px < 0 || py < 0 || px >= w || py >= h) return false;
        if (start && tool != Tool.EYEDROPPER) pushUndo();
        int idx = py * w + px;
        switch (tool) {
            case EYEDROPPER -> color = pixels[idx] | 0xFF000000;
            case BUCKET -> { if (start) fill(px, py, pixels[idx], color); }
            case BRUSH, ERASER -> {
                for (int dy = -(brush - 1); dy < brush; dy++)
                    for (int dx = -(brush - 1); dx < brush; dx++) {
                        int x = px + dx, y = py + dy;
                        if (x < 0 || y < 0 || x >= w || y >= h) continue;
                        int i = y * w + x;
                        if ((original[i] >>> 24) == 0) continue; // don't paint outside the UV islands
                        pixels[i] = tool == Tool.ERASER ? original[i] : color;
                    }
            }
        }
        upload();
        return true;
    }

    private void fill(int x0, int y0, int from, int to) {
        if (from == to) return;
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{x0, y0});
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int x = p[0], y = p[1];
            if (x < 0 || y < 0 || x >= w || y >= h) continue;
            int i = y * w + x;
            if (pixels[i] != from || (original[i] >>> 24) == 0) continue;
            pixels[i] = to;
            q.add(new int[]{x + 1, y}); q.add(new int[]{x - 1, y}); q.add(new int[]{x, y + 1}); q.add(new int[]{x, y - 1});
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int sw = 10, px = leftPos + CX, py = topPos + CY + CANVAS + 3;
        for (int i = 0; i < 16; i++) {
            int x = px + i * sw;
            if (mx >= x && mx < x + sw && my >= py && my < py + sw) {
                color = DyeColor.byId(i).getTextColor() | 0xFF000000;
                return true;
            }
        }
        if (button == 0 && paintAt(mx, my, true)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && (tool == Tool.BRUSH || tool == Tool.ERASER) && paintAt(mx, my, false)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    protected void containerTick() {
        reload();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF00A0608);
        g.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xAA9B1C28);
        g.fill(leftPos + 7, topPos + 17, leftPos + 25, topPos + 35, 0xFF5EF6FF);
        g.fill(leftPos + 8, topPos + 18, leftPos + 24, topPos + 34, 0xFF120A0C);
        for (int i = 9; i < menu.slots.size(); i++) {
            var s = menu.slots.get(i);
            g.fill(leftPos + s.x - 1, topPos + s.y - 1, leftPos + s.x + 17, topPos + s.y + 17, 0xFF3A2228);
            g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0xFF120A0C);
        }
        for (int i = 1; i < 9; i++) {
            var s = menu.slots.get(i);
            g.fill(leftPos + s.x - 1, topPos + s.y - 1, leftPos + s.x + 17, topPos + s.y + 17, 0xFF3A2228);
            g.fill(leftPos + s.x, topPos + s.y, leftPos + s.x + 16, topPos + s.y + 16, 0xFF120A0C);
        }
        int cx = leftPos + CX, cy = topPos + CY;
        // checkerboard behind transparent pixels
        for (int y = 0; y < CANVAS; y += 8)
            for (int x = 0; x < CANVAS; x += 8) g.fill(cx + x, cy + y, cx + x + 8, cy + y + 8, ((x + y) / 8 % 2 == 0) ? 0xFF2A2A2A : 0xFF1E1E1E);
        if (pixels != null) {
            int s = scale();
            g.blit(CANVAS_ID, cx, cy, 0, 0, w * s, h * s, w * s, h * s);
        } else {
            g.drawCenteredString(font, Component.translatable("gui.constructchip.paint.insert"), cx + CANVAS / 2, cy + CANVAS / 2, 0xFFE8B4B4);
        }
        g.renderOutline(cx - 1, cy - 1, CANVAS + 2, CANVAS + 2, 0xFF5EF6FF);
        int py = cy + CANVAS + 3;
        for (int i = 0; i < 16; i++) g.fill(cx + i * 10, py, cx + i * 10 + 9, py + 9, DyeColor.byId(i).getTextColor() | 0xFF000000);
        g.fill(leftPos + 8, topPos + 210, leftPos + 40, topPos + 226, color);
        g.renderOutline(leftPos + 8, topPos + 210, 32, 16, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 48, 6, 0xFFF3E600, false);
        if (!textures.isEmpty()) {
            g.drawString(font, Component.literal(textures.get(texIndex).replace("_texture", "").replace('_', ' ') + "  (" + tool.name().toLowerCase() + ", " + brush + ")"),
                    100, 6, 0xFFE8B4B4, false);
        }
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE8B4B4, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public void removed() {
        super.removed();
        if (canvas != null) {
            minecraft.getTextureManager().release(CANVAS_ID);
            canvas = null;
        }
    }
}
