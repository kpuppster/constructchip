package io.github.constructchip;

import com.mojang.logging.LogUtils;
import io.github.constructchip.attachment.ModAttachments;
import io.github.constructchip.hack.ModQuickhacks;
import io.github.constructchip.network.ModNetwork;
import io.github.constructchip.registry.ModDataComponents;
import io.github.constructchip.registry.ModBlockEntities;
import io.github.constructchip.registry.ModBlocks;
import io.github.constructchip.registry.ModCreativeTabs;
import io.github.constructchip.registry.ModEntities;
import io.github.constructchip.registry.ModMenus;
import io.github.constructchip.registry.ModSounds;
import io.github.constructchip.skill.ModSkills;
import io.github.constructchip.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(ConstructChip.MODID)
public final class ConstructChip {
    public static final String MODID = "constructchip";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ConstructChip(IEventBus modBus, ModContainer container) {
        ModGameRules.init();
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        ModDataComponents.REGISTER.register(modBus);
        ModSounds.REGISTER.register(modBus);
        ModBlocks.REGISTER.register(modBus);
        io.github.constructchip.cyberware.CyberItems.register();
        ModItems.REGISTER.register(modBus);
        io.github.constructchip.cyberware.CyberEffectsRegistry.REGISTER.register(modBus);
        io.github.constructchip.villager.Ripperdoc.POI.register(modBus);
        io.github.constructchip.villager.Ripperdoc.PROFESSIONS.register(modBus);
        ModBlockEntities.REGISTER.register(modBus);
        ModCreativeTabs.REGISTER.register(modBus);
        ModEntities.REGISTER.register(modBus);
        ModAttachments.REGISTER.register(modBus);
        ModMenus.REGISTER.register(modBus);
        modBus.addListener(ModSkills::onNewDatapackRegistry);
        if (ModList.get().isLoaded("vista")) {
            // Only touch the compat class when Vista is present, so its imports never load otherwise.
            io.github.constructchip.compat.vista.VistaCompat.init(modBus);
        }
        ModQuickhacks.REGISTER.register(modBus);
        modBus.addListener(ModQuickhacks::onNewRegistry);
        modBus.addListener(ModNetwork::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
