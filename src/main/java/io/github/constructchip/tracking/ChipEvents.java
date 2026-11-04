package io.github.constructchip.tracking;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.ModGameRules;
import io.github.constructchip.attachment.ModAttachments;
import io.github.constructchip.capture.ConsentManager;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
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
            InteractionHand hand = event.getHand();
            ChipTracker.InstallResult result = ChipTracker.install(installer, host, hand, false);
            installer.displayClientMessage(Component.translatable(result.translationKey()), true);
            if (result == ChipTracker.InstallResult.NEEDS_CONSENT) askHostToAccept(installer, host, hand);
        }
    }

    /**
     * The host must agree before someone else's chip goes into their head. On accept, everything is re-checked:
     * the installer still holds the same chip, is within 6 blocks, and the rules still allow it.
     */
    private static void askHostToAccept(ServerPlayer installer, ServerPlayer host, InteractionHand hand) {
        ItemStack held = installer.getItemInHand(hand);
        UUID chipId = ChipStacks.identity(held).orElseThrow().chipId();
        String construct = ChipStacks.identity(held).get().capturedName();
        UUID installerId = installer.getUUID();
        UUID hostId = host.getUUID();
        ConsentManager.request(host,
                Component.translatable("gui.constructchip.consent.install.title"),
                Component.translatable("gui.constructchip.consent.install.body", installer.getGameProfile().getName(), construct),
                ConsentManager.DEFAULT_TIMEOUT_TICKS,
                new ConsentManager.Handler() {
                    @Override
                    public Component revalidate(MinecraftServer server) {
                        ServerPlayer i = server.getPlayerList().getPlayer(installerId);
                        ServerPlayer h = server.getPlayerList().getPlayer(hostId);
                        if (i == null || h == null) return Component.translatable("message.constructchip.consent.left");
                        boolean sameChip = ChipStacks.identity(i.getItemInHand(hand)).map(id -> id.chipId().equals(chipId)).orElse(false);
                        if (!sameChip) return Component.translatable("message.constructchip.consent.chip_moved");
                        if (i.level() != h.level() || i.distanceToSqr(h) > 36) return Component.translatable("message.constructchip.consent.too_far");
                        if (!server.getGameRules().getBoolean(ModGameRules.ALLOW_CONSTRUCT_CAPTURE)) {
                            return Component.translatable(ChipTracker.InstallResult.CAPTURE_DISABLED.translationKey());
                        }
                        if (h.getData(ModAttachments.OPTED_OUT)) return Component.translatable(ChipTracker.InstallResult.HOST_OPTED_OUT.translationKey());
                        return null;
                    }

                    @Override
                    public void accepted(MinecraftServer server) {
                        ServerPlayer i = server.getPlayerList().getPlayer(installerId);
                        ServerPlayer h = server.getPlayerList().getPlayer(hostId);
                        if (i == null || h == null) return;
                        ChipTracker.InstallResult result = ChipTracker.install(i, h, hand, true);
                        i.displayClientMessage(Component.translatable(result.translationKey()), true);
                        h.displayClientMessage(Component.translatable(result.translationKey()), true);
                    }

                    @Override
                    public void declined(MinecraftServer server, Component reason) {
                        ServerPlayer i = server.getPlayerList().getPlayer(installerId);
                        if (i != null) i.displayClientMessage(reason, true);
                    }
                });
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
