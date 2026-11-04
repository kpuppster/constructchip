package io.github.constructchip.registry;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.block.SoulKillerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ConstructChip.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoulKillerBlockEntity>> SOUL_KILLER =
            REGISTER.register("soul_killer", () -> BlockEntityType.Builder.of(SoulKillerBlockEntity::new,
                    ModBlocks.SOUL_KILLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.github.constructchip.paint.PaintingStationBlockEntity>> PAINTING_STATION =
            REGISTER.register("painting_station", () -> BlockEntityType.Builder.of(io.github.constructchip.paint.PaintingStationBlockEntity::new,
                    ModBlocks.PAINTING_STATION.get()).build(null));

    private ModBlockEntities() {}
}
