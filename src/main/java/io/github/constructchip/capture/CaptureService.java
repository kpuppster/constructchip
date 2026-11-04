package io.github.constructchip.capture;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.attachment.ModAttachments;
import io.github.constructchip.attachment.ConstructState;
import io.github.constructchip.chip.ChipIdentity;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.tracking.ChipEntry;
import io.github.constructchip.tracking.ChipLocation;
import io.github.constructchip.tracking.ChipRegistry;
import io.github.constructchip.tracking.ChipTracker;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Turns a player into a blank construct body and back. Capture order: snapshot first, then strip. Restore order:
 * remove the snapshot first, then apply it. So no bug or repeat call can hand the items back twice.
 */
public final class CaptureService {
    /** Game mode while living in a host. Step 5's restrictions build on this. */
    public static final GameType CONSTRUCT_GAME_MODE = GameType.ADVENTURE;

    private CaptureService() {}

    public static Optional<CaptureSnapshot> snapshotOf(ServerPlayer player) {
        return player.getData(ModAttachments.CAPTURE_SNAPSHOT);
    }

    /**
     * Saves the player's state and strips them: empty inventory, no XP, no effects, full health, adventure mode.
     * Captured chips they carry (other constructs) are dropped at their feet instead of disappearing into the
     * snapshot, so those constructs aren't stranded. Callers must have validated the capture already.
     *
     * @return false if the player already has a snapshot (never overwrite one: it holds their real items)
     */
    public static boolean capture(ServerPlayer player) {
        if (snapshotOf(player).isPresent()) {
            ConstructChip.LOGGER.warn("Refusing to capture {}: they already have a capture snapshot",
                    player.getGameProfile().getName());
            return false;
        }
        Inventory inv = player.getInventory();
        List<CaptureSnapshot.SlotItem> items = new ArrayList<>();
        List<ItemStack> chipsToDrop = new ArrayList<>();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            if (ChipStacks.isCaptured(stack)) chipsToDrop.add(stack.copy());
            else items.add(new CaptureSnapshot.SlotItem(slot, stack.copy()));
        }
        List<MobEffectInstance> effects = player.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        CaptureSnapshot snapshot = new CaptureSnapshot(
                items,
                new CaptureSnapshot.Xp(player.experienceLevel, player.experienceProgress, player.totalExperience),
                player.gameMode.getGameModeForPlayer(),
                new CaptureSnapshot.Position(player.level().dimension(), player.position(), player.getYRot(), player.getXRot()),
                new CaptureSnapshot.Vitals(player.getHealth(), player.getFoodData().getFoodLevel(),
                        player.getFoodData().getSaturationLevel(), effects),
                player.level().getGameTime());
        player.setData(ModAttachments.CAPTURE_SNAPSHOT, Optional.of(snapshot)); // saved BEFORE anything is removed

        inv.clearContent();
        player.containerMenu.broadcastChanges();
        for (ItemStack chip : chipsToDrop) player.drop(chip, false); // becomes a tracked chip entity
        player.setExperienceLevels(0);
        player.experienceProgress = 0f;
        player.totalExperience = 0;
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5f);
        player.setGameMode(CONSTRUCT_GAME_MODE);
        ConstructChip.LOGGER.info("Captured {}: saved {} stacks, {} levels, {} at {}",
                player.getGameProfile().getName(), items.size(), snapshot.xp().level(), snapshot.gameMode(),
                snapshot.position().pos());
        return true;
    }

    /** Why {@code player} can't be captured right now, or null if they can (rules like opt-out are checked by callers). */
    public static Component whyNotCapturable(ServerPlayer player) {
        String name = player.getGameProfile().getName();
        if (ChipTracker.isConstruct(player) || ChipRegistry.get(player.server).chipOf(player.getUUID()).isPresent()) {
            return Component.translatable("commands.constructchip.givechip.already_captured", name);
        }
        if (!ChipTracker.installedChip(player).isEmpty()) {
            return Component.translatable("commands.constructchip.givechip.has_chip_installed", name);
        }
        if (snapshotOf(player).isPresent()) return Component.translatable("commands.constructchip.givechip.already_captured", name);
        return null;
    }

    /**
     * The full capture: snapshot and strip {@code captured}, mint a new chip, register it as carried by
     * {@code receiver}, mark the player as its construct, and hand the chip over. Used by the Soul Killer and
     * {@code /construct givechip}. Returns empty (and changes nothing) if the snapshot step refuses.
     */
    public static Optional<ItemStack> captureOntoNewChip(ServerPlayer captured, ServerPlayer receiver) {
        if (whyNotCapturable(captured) != null || captured == receiver) return Optional.empty();
        if (!capture(captured)) return Optional.empty();
        MinecraftServer server = captured.server;
        ItemStack chip = ChipStacks.createCaptured(captured, captured.level().getGameTime());
        ChipIdentity id = ChipStacks.identity(chip).orElseThrow();
        ChipRegistry.get(server).register(id.chipId(), new ChipEntry(captured.getUUID(), captured.getGameProfile().getName(),
                ChipTracker.serialOf(chip), ChipLocation.of(ChipLocation.State.CARRIED, receiver.getUUID(),
                        GlobalPos.of(receiver.level().dimension(), receiver.blockPosition()), ChipRegistry.now(server))));
        captured.setData(ModAttachments.CONSTRUCT_STATE, ConstructState.of(id.chipId()));
        ItemStack handed = chip.copy();
        if (!receiver.addItem(chip) && !chip.isEmpty()) receiver.drop(chip, false);
        return Optional.of(handed);
    }

    /**
     * Puts everything back and teleports the player to where they were captured. Anything the construct somehow
     * holds is given back after the restore (or dropped if there's no room). No-op without a snapshot.
     */
    public static boolean restore(ServerPlayer player) {
        Optional<CaptureSnapshot> opt = snapshotOf(player);
        if (opt.isEmpty()) return false;
        CaptureSnapshot snapshot = opt.get();
        player.setData(ModAttachments.CAPTURE_SNAPSHOT, Optional.empty()); // first: restore can never run twice

        Inventory inv = player.getInventory();
        List<ItemStack> extras = new ArrayList<>();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty()) extras.add(stack.copy());
        }
        inv.clearContent();
        for (CaptureSnapshot.SlotItem item : snapshot.items()) {
            if (item.slot() >= 0 && item.slot() < inv.getContainerSize() && inv.getItem(item.slot()).isEmpty()) {
                inv.setItem(item.slot(), item.stack().copy());
            } else {
                extras.add(item.stack().copy());
            }
        }

        player.setGameMode(snapshot.gameMode());
        CaptureSnapshot.Position pos = snapshot.position();
        ServerLevel level = player.server.getLevel(pos.dimension());
        if (level == null) { // dimension removed (datapack/mod gone): fall back to overworld spawn
            level = player.server.overworld();
            Vec3 spawn = Vec3.atBottomCenterOf(level.getSharedSpawnPos());
            player.teleportTo(level, spawn.x, spawn.y, spawn.z, pos.yRot(), pos.xRot());
        } else {
            // never back inside a Soul Killer (older snapshots were taken on the chair spot) or a wall
            Vec3 to = io.github.constructchip.block.SafeSpot.forPlayer(level, player, pos.pos());
            player.teleportTo(level, to.x, to.y, to.z, pos.yRot(), pos.xRot());
        }

        player.setExperienceLevels(snapshot.xp().level()); // also forces an XP sync to the client
        player.experienceProgress = snapshot.xp().progress();
        player.totalExperience = snapshot.xp().total();
        player.removeAllEffects();
        for (MobEffectInstance effect : snapshot.vitals().effects()) player.addEffect(new MobEffectInstance(effect));
        player.setHealth(Math.min(player.getMaxHealth(), Math.max(1f, snapshot.vitals().health())));
        player.getFoodData().setFoodLevel(snapshot.vitals().food());
        player.getFoodData().setSaturation(snapshot.vitals().saturation());

        for (ItemStack extra : extras) {
            if (!player.addItem(extra) && !extra.isEmpty()) player.drop(extra, false);
        }
        player.containerMenu.broadcastChanges();
        ConstructChip.LOGGER.info("Restored {}: {} stacks, {} levels, {}", player.getGameProfile().getName(),
                snapshot.items().size(), snapshot.xp().level(), snapshot.gameMode());
        return true;
    }
}
