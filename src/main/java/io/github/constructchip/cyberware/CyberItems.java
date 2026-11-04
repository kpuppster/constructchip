package io.github.constructchip.cyberware;

import io.github.constructchip.registry.ModItems;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;

/** All cyberware items, registered into ModItems.REGISTER (art by A24-N22, Project: Edgerunner, used with permission). */
public final class CyberItems {
    public static final Map<Implant, DeferredItem<CyberwareItem>> IMPLANTS = new EnumMap<>(Implant.class);
    public static final Map<Part, DeferredItem<Item>> PARTS = new EnumMap<>(Part.class);
    public static DeferredItem<Item> SOLDERING_IRON;
    public static DeferredItem<Item> IMMUNOSUPPRESSANTS;
    public static DeferredItem<Item> GUIDE;

    private CyberItems() {}

    public static void register() {
        for (Implant implant : Implant.values()) {
            Rarity rarity = implant.tier >= 5 ? Rarity.EPIC : implant.tier >= 3 ? Rarity.RARE : Rarity.UNCOMMON;
            IMPLANTS.put(implant, ModItems.REGISTER.register(implant.id(),
                    () -> new CyberwareItem(implant, new Item.Properties().stacksTo(1).rarity(rarity))));
        }
        for (Part part : Part.values()) {
            PARTS.put(part, ModItems.REGISTER.register(part.id(), () -> new Item(new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack s, TooltipContext c, List<Component> t, TooltipFlag f) {
                    t.add(Component.translatable("tooltip.constructchip.part.tier", part.tier).withStyle(ChatFormatting.DARK_GRAY));
                }
            }));
        }
        SOLDERING_IRON = ModItems.REGISTER.register("soldering_iron", () -> new Item(new Item.Properties().stacksTo(1)) {
            // A tool for assembly recipes: it comes back out of the crafting grid.
            @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }
            @Override public ItemStack getCraftingRemainingItem(ItemStack stack) { return stack.copy(); }
        });
        GUIDE = ModItems.REGISTER.register("guide", () -> new Item(new Item.Properties().stacksTo(1)) {
            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                if (level.isClientSide) io.github.constructchip.client.ClientHooks.openGuide();
                return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
            }
        });
        IMMUNOSUPPRESSANTS = ModItems.REGISTER.register("immunosuppressants", () -> new Item(new Item.Properties().stacksTo(16)) {
            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    Psychosis.dose(sp);
                    if (!sp.getAbilities().instabuild) stack.shrink(1);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }

            @Override
            public void appendHoverText(ItemStack s, TooltipContext c, List<Component> t, TooltipFlag f) {
                t.add(Component.translatable("tooltip.constructchip.immuno").withStyle(ChatFormatting.GRAY));
            }
        });
    }
}
