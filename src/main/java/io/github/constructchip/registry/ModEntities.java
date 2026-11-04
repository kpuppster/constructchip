package io.github.constructchip.registry;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.block.ChairSeatEntity;
import io.github.constructchip.chip.ChipItemEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(Registries.ENTITY_TYPE, ConstructChip.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ChipItemEntity>> CHIP_ITEM = REGISTER.register("chip_item",
            () -> EntityType.Builder.<ChipItemEntity>of(ChipItemEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .eyeHeight(0.2125f)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .build("chip_item"));

    public static final DeferredHolder<EntityType<?>, EntityType<ChairSeatEntity>> CHAIR_SEAT = REGISTER.register("chair_seat",
            () -> EntityType.Builder.<ChairSeatEntity>of(ChairSeatEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.35f)
                    .noSummon()
                    .clientTrackingRange(10)
                    .build("chair_seat"));

    private ModEntities() {}
}
