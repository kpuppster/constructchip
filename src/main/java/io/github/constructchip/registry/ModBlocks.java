package io.github.constructchip.registry;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.block.SignalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(ConstructChip.MODID);

    public static final DeferredBlock<SignalBlock> SIGNAL = REGISTER.register("signal", () -> new SignalBlock(
            BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().noLootTable().replaceable()
                    .pushReaction(PushReaction.DESTROY)));

    private ModBlocks() {}
}
