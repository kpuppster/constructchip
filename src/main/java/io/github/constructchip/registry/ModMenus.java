package io.github.constructchip.registry;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.menu.HostInventoryMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, ConstructChip.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<HostInventoryMenu>> HOST_INVENTORY = REGISTER.register(
            "host_inventory", () -> new MenuType<>(HostInventoryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<io.github.constructchip.cyberware.SurgeryMenu>> SURGERY = REGISTER.register(
            "surgery", () -> new MenuType<>(io.github.constructchip.cyberware.SurgeryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}
}
