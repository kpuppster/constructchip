package io.github.constructchip.chip;

import io.github.constructchip.tracking.ChipTracker;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The personality construct chip. A blank chip has no {@link ChipIdentity}; a captured chip carries one.
 * While loose it heartbeats to the registry every tick (inventoryTick) and drops as a {@link ChipItemEntity}.
 */
public class PersonalityChipItem extends Item {
    public PersonalityChipItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide) ChipTracker.heartbeatCarried(stack, entity);
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return ChipStacks.isCaptured(stack);
    }

    @Override
    public @Nullable Entity createEntity(Level level, Entity location, ItemStack stack) {
        ChipItemEntity entity = new ChipItemEntity(level, location.getX(), location.getY(), location.getZ(), stack);
        entity.setDeltaMovement(location.getDeltaMovement());
        entity.setPickUpDelay(40);
        ChipTracker.onDropped(entity);
        return entity;
    }

    /** Keeps chips out of shulker boxes and bundles (vanilla's check takes no stack, so this covers blank chips too). */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    /** Right-click: install on yourself. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!ChipStacks.isCaptured(stack)) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp) {
            ChipTracker.InstallResult result = ChipTracker.install(sp, sp, hand, false);
            sp.displayClientMessage(Component.translatable(result.translationKey()), true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return ChipStacks.isCaptured(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ChipStacks.identity(stack).ifPresentOrElse(id -> {
            tooltip.add(Component.translatable("item.constructchip.personality_chip.construct", id.capturedName())
                    .withStyle(ChatFormatting.AQUA));
            ChipRam ram = ChipStacks.ram(stack);
            tooltip.add(Component.translatable("item.constructchip.personality_chip.ram",
                    ram.max(), String.format("%.1f", ram.regenPerSecond())).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.constructchip.personality_chip.tier",
                    ChipStacks.skills(stack).tier()).withStyle(ChatFormatting.GRAY));
            if (flag.isAdvanced()) {
                tooltip.add(Component.literal(id.chipId().toString()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }, () -> tooltip.add(Component.translatable("item.constructchip.personality_chip.blank")
                .withStyle(ChatFormatting.DARK_GRAY)));
    }
}
