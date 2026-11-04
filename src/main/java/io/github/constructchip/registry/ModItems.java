package io.github.constructchip.registry;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.chip.PersonalityChipItem;
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

    private ModItems() {}
}
