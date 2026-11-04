package io.github.constructchip.tracking;

import io.github.constructchip.attachment.ConstructState;
import io.github.constructchip.capture.CaptureService;
import io.github.constructchip.ghost.GhostLink;
import io.github.constructchip.attachment.ModAttachments;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * While the chip is anywhere but INSTALLED, its construct is pinned to it: spectator mode, camera on the chip's
 * ItemEntity or carrier, or held at the last known position when STORED. Re-applied every server tick, because
 * vanilla drops the camera when a spectator sneaks or the target dies or unloads.
 *
 * <p>The game mode from before the anchor (normally adventure, the construct mode) is kept in {@link ConstructState}.
 * The full pre-capture state is in {@code CaptureSnapshot} and comes back on release. Step 5 adds what happens while
 * INSTALLED (host link, hiding).
 */
public final class ConstructAnchor {
    private ConstructAnchor() {}

    public static void tick(ServerPlayer player) {
        ConstructState state = player.getData(ModAttachments.CONSTRUCT_STATE);
        if (!state.isConstruct()) return;
        MinecraftServer server = player.server;
        Optional<ChipEntry> entry = ChipRegistry.get(server).get(state.chipId().get());
        if (entry.isEmpty()) {
            clearConstruct(player);
            return;
        }
        ChipLocation loc = entry.get().location();
        if (!loc.state().anchorsConstruct()) {
            if (state.isAnchored()) unanchor(player);
            GhostLink.tickInstalled(player, loc.holder().map(server.getPlayerList()::getPlayer).orElse(null));
            return;
        }

        if (!state.isAnchored()) {
            player.setData(ModAttachments.CONSTRUCT_STATE, state.anchored(player.gameMode.getGameModeForPlayer()));
        }
        if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            player.setGameMode(GameType.SPECTATOR);
        }

        Entity target = resolveTarget(server, loc);
        if (target != null && target != player && target.isAlive()) {
            if (target.level() != player.level()) {
                player.teleportTo((ServerLevel) target.level(), target.getX(), target.getY(), target.getZ(),
                        player.getYRot(), player.getXRot());
            }
            if (player.getCamera() != target) player.setCamera(target);
            return;
        }

        // STORED, or the holder isn't loaded: hold the construct at the last known position.
        if (player.getCamera() != player) player.setCamera(player);
        loc.pos().ifPresent(pos -> holdAt(server, player, pos));
    }

    private static Entity resolveTarget(MinecraftServer server, ChipLocation loc) {
        if (loc.holder().isEmpty()) return null;
        return switch (loc.state()) {
            case CARRIED -> {
                ServerPlayer carrier = server.getPlayerList().getPlayer(loc.holder().get());
                if (carrier != null) yield carrier;
                yield loc.pos().map(p -> server.getLevel(p.dimension()))
                        .map(level -> level.getEntity(loc.holder().get())).orElse(null);
            }
            case DROPPED -> loc.pos().map(p -> server.getLevel(p.dimension()))
                    .map(level -> level.getEntity(loc.holder().get())).orElse(null);
            default -> null;
        };
    }

    private static void holdAt(MinecraftServer server, ServerPlayer player, GlobalPos pos) {
        ServerLevel level = server.getLevel(pos.dimension());
        if (level == null) return;
        Vec3 center = Vec3.atBottomCenterOf(pos.pos()).add(0, 0.5, 0);
        if (player.level() != level || player.position().distanceToSqr(center) > 4.0) {
            player.teleportTo(level, center.x, center.y, center.z, player.getYRot(), player.getXRot());
        }
    }

    /** Ends the anchor: camera back to self, previous game mode restored. The player stays a construct. */
    public static void unanchor(ServerPlayer player) {
        ConstructState state = player.getData(ModAttachments.CONSTRUCT_STATE);
        if (player.getCamera() != player) player.setCamera(player);
        state.gameModeBeforeAnchor().ifPresent(player::setGameMode);
        player.setData(ModAttachments.CONSTRUCT_STATE, state.unanchored());
    }

    /** The player is no longer a construct at all (released, or their chip is gone). */
    public static void clearConstruct(ServerPlayer player) {
        unanchor(player);
        player.setData(ModAttachments.CONSTRUCT_STATE, ConstructState.NONE);
        GhostLink.clear(player);
        CaptureService.restore(player); // inventory, XP, game mode, position, vitals (overrides the anchor's game mode)
    }
}
