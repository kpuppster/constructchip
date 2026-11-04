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

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CYBERWARE = REGISTER.register("cyberware", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.constructchip.cyberware"))
            .icon(() -> io.github.constructchip.cyberware.CyberItems.IMPLANTS.get(io.github.constructchip.cyberware.Implant.GORILLA_ARMS).get().getDefaultInstance())
            .withTabsBefore(MAIN.getKey())
            .displayItems((params, out) -> {
                out.accept(ModItems.PAINTING_STATION.get());
                out.accept(io.github.constructchip.cyberware.CyberItems.SOLDERING_IRON.get());
                out.accept(io.github.constructchip.cyberware.CyberItems.IMMUNOSUPPRESSANTS.get());
                io.github.constructchip.cyberware.CyberItems.IMPLANTS.values().forEach(i -> out.accept(i.get()));
                io.github.constructchip.cyberware.CyberItems.PARTS.values().forEach(i -> out.accept(i.get()));
            })
            .build());

    private ModCreativeTabs() {}
}
