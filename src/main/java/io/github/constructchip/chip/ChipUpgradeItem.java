package io.github.constructchip.chip;

import io.github.constructchip.sync.ChipSync;
import io.github.constructchip.tracking.ChipTracker;
import io.github.constructchip.attachment.ModAttachments;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Raises a chip's hardware tier (Mk.N-1 → Mk.N), which unlocks the next row of the skill tree. Right-click with
 * the captured chip in your other hand, or while a chip is installed in you.
 */
public class ChipUpgradeItem extends Item {
    private final int toTier;

    public ChipUpgradeItem(int toTier, Properties properties) {
        super(properties);
        this.toTier = toTier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack module = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(module);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        boolean installed = false;
        ItemStack chip;
        if (ChipStacks.isCaptured(other)) {
            chip = other;
        } else {
            chip = ChipTracker.installedChip(sp);
            installed = true;
        }
        if (!ChipStacks.isCaptured(chip) || ChipTracker.currentEntry(sp.server, chip).isEmpty()) {
            sp.displayClientMessage(Component.translatable("message.constructchip.upgrade.no_chip"), true);
            return InteractionResultHolder.fail(module);
        }
        ChipSkills skills = ChipStacks.skills(chip);
        if (skills.tier() != toTier - 1) {
            sp.displayClientMessage(Component.translatable("message.constructchip.upgrade.wrong_tier", toTier - 1, skills.tier()), true);
            return InteractionResultHolder.fail(module);
        }
        ChipStacks.setSkills(chip, skills.withTier(toTier));
        if (installed) sp.setData(ModAttachments.INSTALLED_CHIP, chip);
        if (!sp.getAbilities().instabuild) module.shrink(1);
        upgradeSparks(sp);
        sp.displayClientMessage(Component.translatable("message.constructchip.upgrade.done", toTier), true);
        ChipSync.syncHost(sp);
        return InteractionResultHolder.consume(module);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.constructchip.chip_upgrade.tooltip", toTier - 1, toTier).withStyle(ChatFormatting.GRAY));
    }

    /** Electric sparks bursting from the host's head and hand as the construct moves onto the new chip. */
    private static void upgradeSparks(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        Vec3 head = sp.getEyePosition();
        Vec3 hand = sp.position().add(sp.getLookAngle().scale(0.5)).add(0, sp.getBbHeight() * 0.55, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, head.x, head.y, head.z, 24, 0.25, 0.2, 0.25, 0.12);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 14, 0.15, 0.15, 0.15, 0.08);
        level.sendParticles(ParticleTypes.WAX_OFF, head.x, head.y, head.z, 8, 0.3, 0.25, 0.3, 0.6);
    }
}
