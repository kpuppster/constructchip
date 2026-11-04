package io.github.constructchip.tracking;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.chip.ChipStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = ConstructChip.MODID)
public final class ChipEvents {
    private ChipEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) ConstructAnchor.tick(player);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 0) ChipTracker.sweep(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ChipTracker.clearTransientState();
    }

    /** HIGHEST so grave/corpse mods that collect drops later also collect the chip. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer host) ChipTracker.onHostDeath(host, event.getDrops());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ChipTracker.onLogin(player);
    }

    /** Right-click a player with a captured chip: install on them (consent required). Other entities: blocked. */
    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (!ChipStacks.isCaptured(event.getItemStack())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer installer && event.getTarget() instanceof ServerPlayer host) {
            ChipTracker.InstallResult result = ChipTracker.install(installer, host, event.getHand(), false);
            installer.displayClientMessage(Component.translatable(result.translationKey()), true);
        }
    }

    /** Item frames, armor stands (interactAt), allays: never hand them a chip. */
    @SubscribeEvent
    public static void onInteractEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (ChipStacks.isCaptured(event.getItemStack()) && !(event.getTarget() instanceof Player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    /**
     * Blocks that take items directly on right-click without a GUI (decorated pots, jukeboxes, mod displays). GUI
     * containers are handled by the Slot#mayPlace mixin, so chests still open normally.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!ChipStacks.isCaptured(event.getItemStack())) return;
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof Container && !(be instanceof MenuProvider)) {
            event.setUseBlock(TriState.FALSE);
        }
    }
}
