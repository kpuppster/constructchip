package io.github.constructchip.villager;

import com.google.common.collect.ImmutableSet;
import io.github.constructchip.ConstructChip;
import io.github.constructchip.cyberware.CyberItems;
import io.github.constructchip.cyberware.Part;
import io.github.constructchip.registry.ModBlocks;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Ripperdoc villager: an unemployed villager next to a Soul Killer station takes the job (up to 4 per station).
 * Only source of the Soldering Iron, Immunosuppressants and Internal Parts (Project: Edgerunner's economy).
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class Ripperdoc {
    public static final DeferredRegister<PoiType> POI = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, ConstructChip.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, ConstructChip.MODID);

    public static final DeferredHolder<PoiType, PoiType> STATION = POI.register("ripperdoc",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.SOUL_KILLER.get().getStateDefinition().getPossibleStates()), 4, 1));
    public static final DeferredHolder<VillagerProfession, VillagerProfession> PROFESSION = PROFESSIONS.register("ripperdoc",
            () -> new VillagerProfession("ripperdoc", h -> h.is(STATION.getKey()), h -> h.is(STATION.getKey()),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_TOOLSMITH));

    private Ripperdoc() {}

    private static VillagerTrades.ItemListing sell(ItemCost a, ItemCost b, java.util.function.Supplier<ItemStack> result, int uses, int xp) {
        return (trader, random) -> new MerchantOffer(a, b == null ? Optional.empty() : Optional.of(b), result.get(), uses, xp, 0.05f);
    }

    private static ItemStack part(int tier, int count) {
        return new ItemStack(CyberItems.PARTS.get(Part.internalParts(tier)).get(), count);
    }

    @SubscribeEvent
    public static void onTrades(VillagerTradesEvent event) {
        if (event.getType() != PROFESSION.get()) return;
        var t = event.getTrades();
        List<VillagerTrades.ItemListing> l1 = t.get(1), l2 = t.get(2), l3 = t.get(3), l4 = t.get(4), l5 = t.get(5);
        l1.add(sell(new ItemCost(Items.EMERALD, 18), new ItemCost(Items.IRON_INGOT, 12), () -> new ItemStack(CyberItems.SOLDERING_IRON.get()), 3, 10));
        l1.add(sell(new ItemCost(Items.EMERALD, 8), null, () -> new ItemStack(CyberItems.IMMUNOSUPPRESSANTS.get()), 12, 5));
        l2.add(sell(new ItemCost(Items.EMERALD, 6), null, () -> part(1, 2), 16, 10));
        l2.add(sell(new ItemCost(Items.EMERALD, 7), new ItemCost(Items.GLASS_BOTTLE, 1), () -> new ItemStack(CyberItems.IMMUNOSUPPRESSANTS.get(), 2), 12, 10));
        l3.add(sell(new ItemCost(Items.EMERALD, 12), null, () -> part(2, 2), 12, 15));
        l3.add(sell(new ItemCost(Items.EMERALD, 20), new ItemCost(Items.DIAMOND, 1), () -> part(3, 2), 10, 20));
        l4.add(sell(new ItemCost(Items.EMERALD, 32), new ItemCost(Items.DIAMOND, 2), () -> part(4, 2), 8, 25));
        l5.add(sell(new ItemCost(Items.EMERALD, 48), new ItemCost(Items.NETHERITE_SCRAP, 1), () -> part(5, 2), 6, 30));
    }
}
