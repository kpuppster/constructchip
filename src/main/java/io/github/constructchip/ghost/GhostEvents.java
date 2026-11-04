package io.github.constructchip.ghost;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.hack.QuickhackService;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Constructs can't touch the world: no interacting, attacking, breaking, placing, dropping or picking up; no damage;
 * mobs ignore them. The only exception is a quickhack in progress (the service flags the construct and host), so
 * hacks that act "as the host" still fire normal events that other mods can veto.
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class GhostEvents {
    private GhostEvents() {}

    private static boolean blocked(Entity entity) {
        return GhostState.isConstruct(entity) && !QuickhackService.isHackInProgress(entity);
    }

    private static <E extends PlayerInteractEvent & ICancellableEvent> void cancel(E event) {
        if (blocked(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) { cancel(event); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) { cancel(event); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) { cancel(event); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) { cancel(event); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (blocked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        if (blocked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (blocked(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() != null && blocked(event.getEntity())) event.setCanceled(true);
    }

    /** Cancelling a toss would delete the item (it's already out of the inventory), so hand it back. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        if (!blocked(player)) return;
        event.setCanceled(true);
        if (!player.level().isClientSide) player.getInventory().add(event.getEntity().getItem().copy());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (GhostState.isConstruct(event.getPlayer())) event.setCanPickup(TriState.FALSE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onXp(PlayerXpEvent.PickupXp event) {
        if (GhostState.isConstruct(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (GhostState.isConstruct(event.getEntity())) event.setCanceled(true);
    }

    /** Mobs never notice constructs. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && GhostState.isConstruct(event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
        }
    }
}
