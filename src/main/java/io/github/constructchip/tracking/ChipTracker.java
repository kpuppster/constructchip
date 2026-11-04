package io.github.constructchip.tracking;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.ModGameRules;
import io.github.constructchip.attachment.ConstructState;
import io.github.constructchip.attachment.ModAttachments;
import io.github.constructchip.capture.CaptureService;
import io.github.constructchip.chip.ChipIdentity;
import io.github.constructchip.chip.ChipItemEntity;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.registry.ModDataComponents;
import io.github.constructchip.sync.ChipSync;
import io.github.constructchip.tracking.ChipLocation.State;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * All chip hand-overs go through here, so the {@link ChipRegistry} always knows where the one valid stack is.
 *
 * <p>Dupe guard. A stack is "current" when its chipId is registered and its serial is not behind the registry's.
 * The serial is bumped on every install and eject, so copies left behind become stale. Every loose stack heartbeats
 * every tick (inventoryTick or the chip ItemEntity's tick). If two stacks with the same chipId heartbeat in the same tick,
 * the second one is a duplicate. Stale and duplicate stacks are wiped to blank chips.
 *
 * <p>A stack whose serial is AHEAD of the registry is accepted, and the registry catches up. That only happens when
 * player data saved but the registry didn't (crash between saves), and in that case the stack is the newest one.
 */
public final class ChipTracker {
    /** A loose chip that stops heartbeating for this long becomes STORED. */
    public static final long STORED_TIMEOUT_TICKS = 100L;

    /** chipId -> overworld tick of its last heartbeat. Transient: only for same-tick duplicate detection. */
    private static final Map<UUID, Long> HEARTBEAT_TICK = new HashMap<>();

    private ChipTracker() {}

    public static void clearTransientState() {
        HEARTBEAT_TICK.clear();
    }

    // ---------------------------------------------------------------- validity

    public static int serialOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CHIP_SERIAL.get(), 0);
    }

    private static void setSerial(ItemStack stack, int serial) {
        stack.set(ModDataComponents.CHIP_SERIAL.get(), serial);
    }

    /**
     * The registry entry this stack is the valid copy of, or empty when the stack is blank, unregistered, stale,
     * or belongs to someone else. If the stack is ahead of the registry, the registry's serial is caught up.
     */
    public static Optional<ChipEntry> currentEntry(MinecraftServer server, ItemStack stack) {
        Optional<ChipIdentity> id = ChipStacks.identity(stack);
        if (id.isEmpty()) return Optional.empty();
        ChipRegistry registry = ChipRegistry.get(server);
        Optional<ChipEntry> entry = registry.get(id.get().chipId());
        if (entry.isEmpty() || !entry.get().capturedUuid().equals(id.get().capturedUuid())) return Optional.empty();
        int serial = serialOf(stack);
        if (serial < entry.get().serial()) return Optional.empty();
        if (serial > entry.get().serial()) {
            return registry.update(id.get().chipId(),
                    e -> new ChipEntry(e.capturedUuid(), e.capturedName(), serial, e.location()));
        }
        return entry;
    }

    /** Strips a stack down to a blank chip. Used on stale copies, duplicates, and chips of released constructs. */
    public static void wipe(ItemStack stack, String reason) {
        ChipStacks.identity(stack).ifPresent(id -> ConstructChip.LOGGER.info(
                "Wiping chip {} ({}): {}", id.chipId(), id.capturedName(), reason));
        ChipStacks.clearAll(stack);
    }

    // ---------------------------------------------------------------- heartbeats

    /** From {@code Item#inventoryTick}: the chip is loose in an entity's inventory. */
    public static void heartbeatCarried(ItemStack stack, Entity holder) {
        if (!(holder.level() instanceof ServerLevel level)) return;
        heartbeat(level, stack, State.CARRIED, holder.getUUID(), GlobalPos.of(level.dimension(), holder.blockPosition()));
    }

    /** From {@link ChipItemEntity#tick()}. */
    public static void heartbeatDropped(ChipItemEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        heartbeat(level, entity.getItem(), State.DROPPED, entity.getUUID(),
                GlobalPos.of(level.dimension(), entity.blockPosition()));
    }

    private static void heartbeat(ServerLevel level, ItemStack stack, State state, UUID holder, GlobalPos pos) {
        Optional<ChipIdentity> id = ChipStacks.identity(stack);
        if (id.isEmpty()) return;
        MinecraftServer server = level.getServer();
        Optional<ChipEntry> entry = currentEntry(server, stack);
        if (entry.isEmpty()) {
            wipe(stack, "stale or unregistered");
            return;
        }
        if (entry.get().location().state() == State.INSTALLED) {
            // The real stack is inside a host. Anything with a current serial outside is a copy.
            wipe(stack, "copy of an installed chip");
            return;
        }
        UUID chipId = id.get().chipId();
        long now = ChipRegistry.now(server);
        Long previous = HEARTBEAT_TICK.put(chipId, now);
        if (previous != null && previous == now) {
            wipe(stack, "duplicate (two stacks heartbeat in the same tick)");
            return;
        }
        ChipRegistry.get(server).update(chipId, e -> e.withLocation(ChipLocation.of(state, holder, pos, now)));
    }

    // ---------------------------------------------------------------- hand-overs

    /** Called when a chip ItemEntity is created (Q, dispenser, eject overflow). Host death uses {@link #onHostDeath}. */
    public static void onDropped(ChipItemEntity entity) {
        markDropped(entity, false);
    }

    private static void markDropped(ChipItemEntity entity, boolean fromInstalled) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        currentEntry(level.getServer(), entity.getItem()).ifPresent(entry -> {
            // A loose stack can't override INSTALLED. Only the host-death path moves an installed chip to the ground.
            if (entry.location().state() == State.INSTALLED && !fromInstalled) return;
            UUID chipId = ChipStacks.identity(entity.getItem()).orElseThrow().chipId();
            long now = ChipRegistry.now(level.getServer());
            HEARTBEAT_TICK.remove(chipId); // hand-over: the new holder may heartbeat later this same tick
            ChipRegistry.get(level.getServer()).update(chipId, e -> e.withLocation(ChipLocation.of(
                    State.DROPPED, entity.getUUID(), GlobalPos.of(level.dimension(), entity.blockPosition()), now)));
        });
    }

    /** Called after a player successfully picked the chip ItemEntity up. */
    public static void onPickedUp(ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Optional<ChipIdentity> id = ChipStacks.identity(stack);
        if (id.isEmpty() || currentEntry(level.getServer(), stack).isEmpty()) return;
        long now = ChipRegistry.now(level.getServer());
        // The ItemEntity already heartbeat this tick; without this the picker's first heartbeat looks like a duplicate.
        HEARTBEAT_TICK.remove(id.get().chipId());
        ChipRegistry.get(level.getServer()).update(id.get().chipId(), e -> e.withLocation(ChipLocation.of(
                State.CARRIED, player.getUUID(), GlobalPos.of(level.dimension(), player.blockPosition()), now)));
    }

    public static ItemStack installedChip(Player host) {
        return host.getData(ModAttachments.INSTALLED_CHIP);
    }

    public static boolean isConstruct(Player player) {
        return player.getData(ModAttachments.CONSTRUCT_STATE).isConstruct();
    }

    public enum InstallResult {
        OK, NOT_A_CAPTURED_CHIP, STALE_CHIP, OWN_CHIP, HOST_IS_CONSTRUCT, HOST_HAS_CHIP,
        CAPTURE_DISABLED, HOST_OPTED_OUT, NEEDS_CONSENT;

        public String translationKey() {
            return "message.constructchip.install." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /**
     * Moves the chip in {@code installer}'s hand into {@code host}'s head.
     *
     * @param adminOverride skip the game rule, the host's opt-out, and the consent screen
     */
    public static InstallResult install(ServerPlayer installer, ServerPlayer host, InteractionHand hand, boolean adminOverride) {
        ItemStack stack = installer.getItemInHand(hand);
        Optional<ChipIdentity> id = ChipStacks.identity(stack);
        if (id.isEmpty()) return InstallResult.NOT_A_CAPTURED_CHIP;
        MinecraftServer server = installer.server;
        Optional<ChipEntry> entry = currentEntry(server, stack);
        if (entry.isEmpty() || entry.get().location().state() == State.INSTALLED) {
            wipe(stack, "stale chip used for install");
            return InstallResult.STALE_CHIP;
        }
        if (host.getUUID().equals(id.get().capturedUuid())) return InstallResult.OWN_CHIP;
        if (isConstruct(host)) return InstallResult.HOST_IS_CONSTRUCT;
        if (!installedChip(host).isEmpty()) return InstallResult.HOST_HAS_CHIP;
        if (installer != host && !adminOverride) {
            if (!server.getGameRules().getBoolean(ModGameRules.ALLOW_CONSTRUCT_CAPTURE)) return InstallResult.CAPTURE_DISABLED;
            if (host.getData(ModAttachments.OPTED_OUT)) return InstallResult.HOST_OPTED_OUT;
            // TODO(step 11): open the host's consent screen; on accept, re-validate and call install(..., true).
            return InstallResult.NEEDS_CONSENT;
        }

        ItemStack chip = stack.copyAndClear();
        int serial = entry.get().serial() + 1;
        setSerial(chip, serial);
        host.setData(ModAttachments.INSTALLED_CHIP, chip);
        long now = ChipRegistry.now(server);
        ChipRegistry.get(server).update(id.get().chipId(), e -> new ChipEntry(e.capturedUuid(), e.capturedName(), serial,
                ChipLocation.of(State.INSTALLED, host.getUUID(), GlobalPos.of(host.level().dimension(), host.blockPosition()), now)));
        ChipSync.syncChip(server, id.get().chipId());
        ConstructChip.LOGGER.info("{} installed chip {} ({}) on {}", installer.getGameProfile().getName(),
                id.get().chipId(), id.get().capturedName(), host.getGameProfile().getName());
        return InstallResult.OK;
    }

    /** Takes the installed chip out of {@code host}'s head into their inventory (or drops it). */
    public static boolean eject(ServerPlayer host) {
        ItemStack chip = installedChip(host);
        if (chip.isEmpty()) return false;
        host.setData(ModAttachments.INSTALLED_CHIP, ItemStack.EMPTY);
        ChipStacks.identity(chip).ifPresent(id -> {
            Optional<ChipEntry> entry = currentEntry(host.server, chip);
            if (entry.isEmpty()) {
                wipe(chip, "stale chip ejected");
                return;
            }
            int serial = entry.get().serial() + 1;
            setSerial(chip, serial);
            long now = ChipRegistry.now(host.server);
            ChipRegistry.get(host.server).update(id.chipId(), e -> new ChipEntry(e.capturedUuid(), e.capturedName(), serial,
                    ChipLocation.of(State.CARRIED, host.getUUID(), GlobalPos.of(host.level().dimension(), host.blockPosition()), now)));
        });
        if (!host.addItem(chip) && !chip.isEmpty()) {
            host.drop(chip, false); // becomes a ChipItemEntity via createEntity, which marks it DROPPED
        }
        ChipSync.sendChipStatus(host);
        ChipStacks.identity(chip).ifPresent(id -> ChipSync.syncChip(host.server, id.chipId()));
        return true;
    }

    /** Host died: the installed chip always drops, keepInventory or not. */
    public static void onHostDeath(ServerPlayer host, Collection<ItemEntity> drops) {
        ItemStack chip = installedChip(host);
        if (chip.isEmpty()) return;
        host.setData(ModAttachments.INSTALLED_CHIP, ItemStack.EMPTY);
        ChipSync.sendChipStatus(host);
        ChipItemEntity entity = new ChipItemEntity(host.level(), host.getX(), host.getY(), host.getZ(), chip);
        entity.setDefaultPickUpDelay();
        drops.add(entity);
        markDropped(entity, true);
    }

    /** The chip ItemEntity was /kill-ed: the chip is confirmed gone, so the construct goes free. */
    public static void onDestroyed(MinecraftServer server, ItemStack stack) {
        if (currentEntry(server, stack).isEmpty()) return;
        ChipStacks.identity(stack).ifPresent(id -> release(server, id.chipId(), "chip destroyed"));
    }

    // ---------------------------------------------------------------- release

    /**
     * Frees the construct on {@code chipId}: removes the registry entry, wipes every copy we can reach, and un-anchors
     * the construct if online (offline constructs are cleaned up on login). Copies we can't reach (unloaded chunks,
     * containers, offline players) are wiped by their next heartbeat, because they are no longer registered.
     */
    public static boolean release(MinecraftServer server, UUID chipId, String reason) {
        ChipRegistry registry = ChipRegistry.get(server);
        Optional<ChipEntry> entryOpt = registry.get(chipId);
        if (entryOpt.isEmpty()) return false;
        ChipEntry entry = entryOpt.get();
        ChipLocation loc = entry.location();

        if (loc.state() == State.INSTALLED) {
            loc.holder().map(server.getPlayerList()::getPlayer).ifPresent(host -> {
                ItemStack chip = installedChip(host);
                if (ChipStacks.identity(chip).map(i -> i.chipId().equals(chipId)).orElse(false)) {
                    host.setData(ModAttachments.INSTALLED_CHIP, ItemStack.EMPTY);
                    wipe(chip, reason);
                    if (!host.addItem(chip) && !chip.isEmpty()) host.drop(chip, false); // hand back the blank chip
                }
            });
        }
        registry.remove(chipId); // from here on, every remaining copy reads as unregistered
        HEARTBEAT_TICK.remove(chipId);

        ServerPlayer construct = server.getPlayerList().getPlayer(entry.capturedUuid());
        if (construct != null) {
            ConstructAnchor.clearConstruct(construct);
            ChipSync.sendChipStatus(construct);
        }
        loc.holder().map(server.getPlayerList()::getPlayer).ifPresent(ChipSync::sendChipStatus);
        ConstructChip.LOGGER.info("Released construct {} from chip {}: {}", entry.capturedName(), chipId, reason);
        return true;
    }

    // ---------------------------------------------------------------- periodic + login

    /** Every second: loose chips that stopped heartbeating become STORED (anchor at last known position). */
    public static void sweep(MinecraftServer server) {
        long now = ChipRegistry.now(server);
        ChipRegistry registry = ChipRegistry.get(server);
        for (UUID chipId : java.util.List.copyOf(registry.all().keySet())) {
            registry.update(chipId, e -> {
                State s = e.location().state();
                boolean loose = s == State.CARRIED || s == State.DROPPED;
                return loose && now - e.location().lastSeenTick() > STORED_TIMEOUT_TICKS
                        ? e.withLocation(e.location().asStored())
                        : e;
            });
        }
        HEARTBEAT_TICK.values().removeIf(t -> now - t > 20L);
    }

    /** Self-heal on login: stale installed chips and constructs whose chip no longer exists. */
    public static void onLogin(ServerPlayer player) {
        MinecraftServer server = player.server;
        ItemStack chip = installedChip(player);
        if (!chip.isEmpty()) {
            Optional<ChipEntry> entry = currentEntry(server, chip);
            boolean ok = entry.isPresent()
                    && (entry.get().location().state() != State.INSTALLED || entry.get().location().isHeldBy(player.getUUID()));
            if (ok) {
                UUID chipId = ChipStacks.identity(chip).orElseThrow().chipId();
                long now = ChipRegistry.now(server);
                ChipRegistry.get(server).update(chipId, e -> e.withLocation(ChipLocation.of(State.INSTALLED,
                        player.getUUID(), GlobalPos.of(player.level().dimension(), player.blockPosition()), now)));
            } else {
                player.setData(ModAttachments.INSTALLED_CHIP, ItemStack.EMPTY);
                wipe(chip, "stale installed chip on login");
                if (!player.addItem(chip) && !chip.isEmpty()) player.drop(chip, false);
            }
        }

        ConstructState state = player.getData(ModAttachments.CONSTRUCT_STATE);
        state.chipId().ifPresent(chipId -> {
            Optional<ChipEntry> entry = ChipRegistry.get(server).get(chipId);
            if (entry.isEmpty() || !entry.get().capturedUuid().equals(player.getUUID())) {
                ConstructChip.LOGGER.info("{} logged in as a construct of a chip that no longer exists; releasing",
                        player.getGameProfile().getName());
                ConstructAnchor.clearConstruct(player);
            }
        });

        // Player data and the registry are saved at different times; heal either half of a crash.
        if (!player.getData(ModAttachments.CONSTRUCT_STATE).isConstruct()) {
            // Registry still has a chip for them but their data says they're free: drop the stale chip.
            ChipRegistry.get(server).chipOf(player.getUUID()).ifPresent(chipId ->
                    release(server, chipId, "player data says not captured (login self-heal)"));
            // A release that cleared the state but never restored them.
            if (CaptureService.restore(player)) {
                ConstructChip.LOGGER.info("{} logged in with an unrestored capture snapshot; restored",
                        player.getGameProfile().getName());
            }
        }
    }
}
