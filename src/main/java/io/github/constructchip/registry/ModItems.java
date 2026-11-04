package io.github.constructchip.registry;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.chip.ChipUpgradeItem;
import io.github.constructchip.chip.PersonalityChipItem;
import io.github.constructchip.block.SoulKillerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(ConstructChip.MODID);

    public static final DeferredItem<PersonalityChipItem> PERSONALITY_CHIP = REGISTER.register("personality_chip",
            () -> new PersonalityChipItem(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.RARE)));

    public static final DeferredItem<ChipUpgradeItem> MK2_MODULE = REGISTER.register("mk2_module",
            () -> new ChipUpgradeItem(2, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ChipUpgradeItem> MK3_MODULE = REGISTER.register("mk3_module",
            () -> new ChipUpgradeItem(3, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<SoulKillerItem> SOUL_KILLER = REGISTER.register("soul_killer",
            () -> new SoulKillerItem(ModBlocks.SOUL_KILLER.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<io.github.constructchip.paint.PaintingStationItem> PAINTING_STATION = REGISTER.register("painting_station",
            () -> new io.github.constructchip.paint.PaintingStationItem(ModBlocks.PAINTING_STATION.get(), new Item.Properties()));

    private ModItems() {}
}
