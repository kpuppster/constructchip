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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

/** An implant item. Installed at the Ripperdoc (Soul Killer station: sneak + right-click). */
public class CyberwareItem extends Item implements GeoItem {
    private static final RawAnimation ARMS_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ARMS_ACTIVE = RawAnimation.begin().thenPlay("extender").thenLoop("active");
    private static final RawAnimation PUMP_IDLE = RawAnimation.begin().thenLoop("animation.unknown.idle");
    private static final RawAnimation ANKLES_IDLE = RawAnimation.begin().thenLoop("animation.fortified_ankles.idle");
    private static final RawAnimation TENDONS_IDLE = RawAnimation.begin().thenLoop("animation.reinforced_tendons.idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** Animations for the 3D implant models (art + animations by A24-N22, used with permission). */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            switch (implant) {
                case GORILLA_ARMS -> {
                    Entity e = state.getData(DataTickets.ENTITY);
                    ChromeVisuals.Visible v = e == null ? null : ChromeVisuals.get(e);
                    return state.setAndContinue(v != null && v.armsDeployed() ? ARMS_ACTIVE : ARMS_IDLE);
                }
                case BLOOD_PUMP -> { return state.setAndContinue(PUMP_IDLE); }
                case FORTIFIED_ANKLES -> { return state.setAndContinue(ANKLES_IDLE); }
                case REINFORCED_TENDONS -> { return state.setAndContinue(TENDONS_IDLE); }
                default -> { return PlayState.STOP; }
            }
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

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
