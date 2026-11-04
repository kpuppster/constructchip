package io.github.constructchip.cyberware;

import io.github.constructchip.registry.ModDataComponents;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** An implant item. Installed at the Ripperdoc (Soul Killer station: sneak + right-click). */
public class CyberwareItem extends Item {
    public final Implant implant;

    public CyberwareItem(Implant implant, Properties properties) {
        super(properties);
        this.implant = implant;
    }

    public static Optional<Implant> implantOf(ItemStack stack) {
        return stack.getItem() instanceof CyberwareItem c ? Optional.of(c.implant) : Optional.empty();
    }

    public static int upgrade(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CYBER_UPGRADE.get(), 0);
    }

    public static String element(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ARMS_ELEMENT.get(), "");
    }

    /** Gorilla Arms: hold a catalyst in the other hand and use the arms to set their element (one at a time). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (implant != Implant.GORILLA_ARMS) return InteractionResultHolder.pass(stack);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        String element = other.is(Items.BLAZE_ROD) ? "thermal" : other.is(Items.LIGHTNING_ROD) ? "electric"
                : other.is(Items.FERMENTED_SPIDER_EYE) ? "toxic" : other.is(Items.BLUE_ICE) ? "frozen" : null;
        if (element == null) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            stack.set(ModDataComponents.ARMS_ELEMENT.get(), element);
            if (!player.getAbilities().instabuild) other.shrink(1);
            player.displayClientMessage(Component.translatable("message.constructchip.arms.element",
                    Component.translatable("element.constructchip." + element)), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.constructchip.cyber.slot", Component.translatable(implant.slot.key()),
                implant.load, implant.tier).withStyle(ChatFormatting.DARK_AQUA));
        int up = upgrade(stack);
        if (up > 0) tooltip.add(Component.translatable("tooltip.constructchip.cyber.upgrade", up, Implant.MAX_UPGRADE).withStyle(ChatFormatting.AQUA));
        if (!element(stack).isEmpty()) {
            tooltip.add(Component.translatable("element.constructchip." + element(stack)).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("implant.constructchip." + implant.id() + ".desc").withStyle(ChatFormatting.GRAY));
        if (implant.cooldownTicks > 0 && implant != Implant.GORILLA_ARMS) {
            tooltip.add(Component.translatable("tooltip.constructchip.cyber.cooldown", implant.cooldownTicks / 20).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static String keyOf(Implant implant) {
        return implant.slot.name().toLowerCase(Locale.ROOT);
    }
}
