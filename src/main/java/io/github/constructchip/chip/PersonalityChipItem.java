package io.github.constructchip.chip;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The personality construct chip. A blank chip has no {@link ChipIdentity}; a captured chip carries one.
 * Later steps add: install/eject, a custom invulnerable non-despawning ItemEntity, inventoryTick heartbeat,
 * and container blocking (overrideStackedOnOther / overrideOtherStackedOnMe).
 */
public class PersonalityChipItem extends Item {
    public PersonalityChipItem(Properties properties) {
        super(properties);
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
