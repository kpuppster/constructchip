package io.github.constructchip.registry;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.block.SignalBlock;
import io.github.constructchip.block.SoulKillerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(ConstructChip.MODID);

    public static final DeferredBlock<SignalBlock> SIGNAL = REGISTER.register("signal", () -> new SignalBlock(
            BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().noLootTable().replaceable()
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<SoulKillerBlock> SOUL_KILLER = REGISTER.register("soul_killer", () -> new SoulKillerBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5f, 9f).requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK).noOcclusion() // the 3D model doesn't cover the block below
                    .lightLevel(s -> s.getValue(SoulKillerBlock.ACTIVE) ? 10 : 2)));

    private ModBlocks() {}
}
