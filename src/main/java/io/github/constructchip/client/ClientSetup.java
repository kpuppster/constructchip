package io.github.constructchip.client;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.registry.ModBlockEntities;
import io.github.constructchip.registry.ModEntities;
import io.github.constructchip.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import io.github.constructchip.registry.ModMenus;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ConstructChip.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.<ItemEntity>registerEntityRenderer(ModEntities.CHIP_ITEM.get(), ItemEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.CHAIR_SEAT.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SOUL_KILLER.get(), ctx -> new SoulKillerRenderer());
    }

    /** Captured chips show the full-color chip art (red Mk.1, cyan Mk.2, gold Mk.3); blank ones the greyed version. */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        io.github.constructchip.possession.Possession.clientBodyName = ClientPossession::bodyName;
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.PERSONALITY_CHIP.get(), ConstructChip.id("captured"),
                    (stack, level, entity, seed) -> ChipStacks.isCaptured(stack) ? 1f : 0f);
            // Mk.2 chips show cyan, Mk.3 gold (same art as the Mk.2/Mk.3 chip items)
            ItemProperties.register(ModItems.PERSONALITY_CHIP.get(), ConstructChip.id("tier"),
                    (stack, level, entity, seed) -> ChipStacks.isCaptured(stack) ? ChipStacks.skills(stack).tier() : 0f);
        });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.USE_HACK);
        event.register(Keybinds.NEXT_HACK);
        event.register(Keybinds.SCAN);
        event.register(Keybinds.HOST_INVENTORY);
        event.register(Keybinds.SKILL_TREE);
        event.register(Keybinds.WHEEL);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.HOST_INVENTORY.get(), HostInventoryScreen::new);
    }

    @SubscribeEvent
    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ConstructChip.id("chip_hud"), HudLayer::render);
    }
}
