package io.github.constructchip.registry;

import io.github.constructchip.ConstructChip;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ConstructChip.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = REGISTER.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.constructchip"))
            .icon(() -> ModItems.PERSONALITY_CHIP.get().getDefaultInstance())
            .displayItems((params, out) -> {
                out.accept(ModItems.PERSONALITY_CHIP.get()); // blank only: captured chips come from captures
                out.accept(ModItems.SOUL_KILLER.get());
                out.accept(ModItems.MK2_MODULE.get());
                out.accept(ModItems.MK3_MODULE.get());
            })
            .build());

    private ModCreativeTabs() {}
}
