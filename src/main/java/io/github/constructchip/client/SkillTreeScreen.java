package io.github.constructchip.client;

import io.github.constructchip.chip.ChipSkills;
import io.github.constructchip.network.ChipStatusPayload;
import io.github.constructchip.network.ServerboundPayloads;
import io.github.constructchip.skill.ModSkills;
import io.github.constructchip.skill.Skill;
import io.github.constructchip.skill.SkillTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The chip's skill tree: tiers are rows, columns come from each skill's {@code column}. Green = unlocked,
 * cyan = can unlock now (click), grey = locked. Both construct and host can spend points; only the host can buy
 * them (with their XP). Reads live client state, so it updates as the server syncs.
 */
public class SkillTreeScreen extends Screen {
    private static final int NODE = 22, STEP_X = 32, STEP_Y = 52, COLUMNS = 10;
    private Button buyButton;

    public SkillTreeScreen() {
        super(Component.translatable("gui.constructchip.skill_tree"));
    }

    private int originX() { return (width - (COLUMNS - 1) * STEP_X - NODE) / 2; }
    private int originY() { return 52; }
    private int nodeX(Skill s) { return originX() + s.column() * STEP_X; }
    private int nodeY(Skill s) { return originY() + (s.tier() - 1) * STEP_Y; }

    @Override
    protected void init() {
        buyButton = addRenderableWidget(Button.builder(Component.empty(),
                b -> PacketDistributor.sendToServer(new ServerboundPayloads.BuySkillPoint(1)))
                .bounds(width / 2 + 60, 20, 140, 20).build());
    }

    private Registry<Skill> registry() {
        return minecraft == null || minecraft.level == null ? null : ModSkills.get(minecraft.level.registryAccess());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        ChipStatusPayload chip = ClientChipState.chip();
        Registry<Skill> registry = registry();
        if (!chip.present() || registry == null) {
            onClose();
            return;
        }
        ChipSkills skills = chip.skills();
        buyButton.visible = chip.isHost();
        buyButton.setMessage(Component.translatable("gui.constructchip.buy_point", chip.nextPointCost()));

        g.drawCenteredString(font, Component.translatable("gui.constructchip.skill_tree.header", chip.constructName(),
                skills.tier(), skills.points()), width / 2 - 70, 26, 0xFF55FFFF);
        if (!chip.isHost()) {
            g.drawCenteredString(font, Component.translatable("gui.constructchip.host_buys"), width / 2, 38, 0xFF888888);
        }
        for (int tier = 1; tier <= 3; tier++) {
            g.drawString(font, "T" + tier, originX() - 22, originY() + (tier - 1) * STEP_Y + 7, 0xFF666666);
        }

        // prerequisite lines first, so nodes draw over them
        for (Map.Entry<ResourceKey<Skill>, Skill> e : registry.entrySet()) {
            Skill s = e.getValue();
            for (ResourceLocation parentId : s.requires()) {
                Skill parent = registry.get(parentId);
                if (parent == null) continue;
                int color = skills.has(parentId) ? 0xFF3FA060 : 0xFF404850;
                int px = nodeX(parent) + NODE / 2, py = nodeY(parent) + NODE;
                int cx = nodeX(s) + NODE / 2, cy = nodeY(s);
                int midY = (py + cy) / 2;
                g.fill(px, py, px + 1, midY + 1, color);
                g.fill(Math.min(px, cx), midY, Math.max(px, cx) + 1, midY + 1, color);
                g.fill(cx, midY, cx + 1, cy, color);
            }
        }

        Skill hovered = null;
        ResourceLocation hoveredId = null;
        for (Map.Entry<ResourceKey<Skill>, Skill> e : registry.entrySet()) {
            Skill s = e.getValue();
            ResourceLocation id = e.getKey().location();
            int x = nodeX(s), y = nodeY(s);
            SkillTree.UnlockResult status = SkillTree.check(skills, id, registry);
            int border = status == SkillTree.UnlockResult.ALREADY_UNLOCKED ? 0xFF3FD060
                    : status == SkillTree.UnlockResult.OK ? 0xFF3FD0E0 : 0xFF505860;
            g.fill(x - 1, y - 1, x + NODE + 1, y + NODE + 1, border);
            g.fill(x, y, x + NODE, y + NODE, 0xFF151A20);
            g.renderItem(new ItemStack(s.icon()), x + 3, y + 3);
            if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) {
                hovered = s;
                hoveredId = id;
            }
        }
        if (hovered != null) g.renderTooltip(font, tooltip(hovered, hoveredId, skills, registry), mouseX, mouseY);
    }

    private List<FormattedCharSequence> tooltip(Skill s, ResourceLocation id, ChipSkills skills, Registry<Skill> registry) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(s.name().copy().withStyle(ChatFormatting.AQUA).getVisualOrderText());
        lines.addAll(font.split(s.description().copy().withStyle(ChatFormatting.GRAY), 220));
        lines.add(Component.translatable("gui.constructchip.skill.cost", s.cost(), s.tier()).withStyle(ChatFormatting.DARK_AQUA).getVisualOrderText());
        if (!s.requires().isEmpty()) {
            Component req = Component.translatable("gui.constructchip.skill.requires");
            for (ResourceLocation r : s.requires()) {
                Skill parent = registry.get(r);
                req = req.copy().append(" ").append(parent == null ? Component.literal(r.toString()) : parent.name());
            }
            lines.add(req.copy().withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        }
        SkillTree.UnlockResult status = SkillTree.check(skills, id, registry);
        ChatFormatting color = status == SkillTree.UnlockResult.OK ? ChatFormatting.GREEN
                : status == SkillTree.UnlockResult.ALREADY_UNLOCKED ? ChatFormatting.DARK_GREEN : ChatFormatting.RED;
        lines.add(Component.translatable("gui.constructchip.skill.status." + status.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(color).getVisualOrderText());
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        Registry<Skill> registry = registry();
        ChipStatusPayload chip = ClientChipState.chip();
        if (registry == null || !chip.present() || button != 0) return false;
        for (Map.Entry<ResourceKey<Skill>, Skill> e : registry.entrySet()) {
            Skill s = e.getValue();
            int x = nodeX(s), y = nodeY(s);
            if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) {
                if (SkillTree.check(chip.skills(), e.getKey().location(), registry) == SkillTree.UnlockResult.OK) {
                    PacketDistributor.sendToServer(new ServerboundPayloads.UnlockSkill(e.getKey().location()));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
