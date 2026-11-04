package io.github.constructchip.hack;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.api.CyberwareApi;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.network.HighlightPayload;
import io.github.constructchip.skill.ChipStats;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tier 1 Scanner, server half: reads a block or creature and tells the construct about it. Free, 0.5 s cooldown.
 * The client half (step 7) does the raycast, the highlight, and a proper overlay instead of chat lines.
 */
public final class ScannerHack implements Quickhack {
    @Override public float baseRamCost() { return 0f; }
    @Override public int cooldownTicks() { return 10; }
    @Override public TargetType targetType() { return TargetType.ENTITY_OR_BLOCK; }
    @Override public double baseRange() { return 16.0; }

    @Override
    public double range(ChipStats stats) {
        return baseRange() + stats.scanRangeBonus();
    }

    public static final ResourceLocation DEEP_SCAN = ConstructChip.id("deep_scan");

    private static boolean deep(HackContext ctx) {
        return ChipStacks.skills(ctx.chip()).has(DEEP_SCAN);
    }

    @Override
    public HackResult execute(HackContext ctx) {
        if (ctx.entity().isPresent()) {
            PacketDistributor.sendToPlayer(ctx.construct(), new HighlightPayload(List.of(ctx.entity().get().getId()), 60));
            return HackResult.success(describeEntity(ctx, ctx.entity().get()));
        }
        if (ctx.block().isPresent()) return HackResult.success(describeBlock(ctx, ctx.block().get()));
        return HackResult.fail(Component.translatable("message.constructchip.hack.no_target"));
    }

    private static MutableComponent header(Component name) {
        return Component.translatable("message.constructchip.scan.header", name).withStyle(ChatFormatting.AQUA);
    }

    private static MutableComponent line(String key, Object... args) {
        return Component.literal("\n ").append(Component.translatable(key, args).withStyle(ChatFormatting.GRAY));
    }

    private static Component describeBlock(HackContext ctx, BlockPos pos) {
        ServerLevel level = ctx.level();
        BlockState state = level.getBlockState(pos);
        MutableComponent out = header(state.getBlock().getName());
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) {
            out.append(line("message.constructchip.scan.unbreakable"));
            return out;
        }
        out.append(line("message.constructchip.scan.hardness", String.format(Locale.ROOT, "%.1f", hardness)));
        if (deep(ctx)) {
            out.append(line("message.constructchip.scan.blast", String.format(Locale.ROOT, "%.1f", state.getBlock().getExplosionResistance())));
            if (level.getBlockEntity(pos) != null) out.append(line("message.constructchip.scan.has_data"));
        }
        out.append(line("message.constructchip.scan.tool", Component.translatable(toolKey(state))));
        if (state.requiresCorrectToolForDrops()) {
            out.append(line("message.constructchip.scan.tier", Component.translatable(tierKey(state))));
        }
        return out;
    }

    private static String toolKey(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return "message.constructchip.scan.tool.pickaxe";
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return "message.constructchip.scan.tool.axe";
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return "message.constructchip.scan.tool.shovel";
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) return "message.constructchip.scan.tool.hoe";
        if (state.is(BlockTags.SWORD_EFFICIENT)) return "message.constructchip.scan.tool.sword";
        return "message.constructchip.scan.tool.any";
    }

    private static String tierKey(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return "message.constructchip.scan.tier.diamond";
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return "message.constructchip.scan.tier.iron";
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return "message.constructchip.scan.tier.stone";
        return "message.constructchip.scan.tier.wood";
    }

    private static Component describeEntity(HackContext ctx, Entity target) {
        MutableComponent out = header(target.getDisplayName());
        if (!(target instanceof LivingEntity living)) return out;
        ServerPlayer host = ctx.host();
        out.append(line("message.constructchip.scan.health",
                String.format(Locale.ROOT, "%.1f", living.getHealth()), String.format(Locale.ROOT, "%.1f", living.getMaxHealth())));
        float armor = living.getArmorValue();
        float toughness = (float) living.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        out.append(line("message.constructchip.scan.armor", Math.round(armor), String.format(Locale.ROOT, "%.1f", toughness)));

        // What one full-strength main-hand hit from the host would do: attribute damage + enchantments, after armor.
        DamageSource source = host.damageSources().playerAttack(host);
        float base = (float) host.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float withEnchants = EnchantmentHelper.modifyDamage(ctx.level(), host.getMainHandItem(), living, source, base);
        float dealt = CombatRules.getDamageAfterAbsorb(living, withEnchants, source, armor, toughness);
        out.append(line("message.constructchip.scan.damage", host.getGameProfile().getName(),
                String.format(Locale.ROOT, "%.1f", dealt)));
        if (deep(ctx)) {
            for (MobEffectInstance effect : living.getActiveEffects()) {
                out.append(line("message.constructchip.scan.effect", Component.translatable(effect.getDescriptionId()),
                        effect.getAmplifier() + 1, effect.getDuration() / 20));
            }
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = living.getItemBySlot(slot);
                if (!stack.isEmpty()) out.append(line("message.constructchip.scan.gear", stack.getHoverName()));
            }
            if (CyberwareApi.isDisabled(living)) out.append(line("message.constructchip.scan.cyberware_down"));
        }
        return out;
    }
}
