package io.github.constructchip.client;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.registry.ModEntities;
import io.github.constructchip.registry.ModMenus;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
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
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.USE_HACK);
        event.register(Keybinds.NEXT_HACK);
        event.register(Keybinds.SCAN);
        event.register(Keybinds.HOST_INVENTORY);
        event.register(Keybinds.SKILL_TREE);
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
