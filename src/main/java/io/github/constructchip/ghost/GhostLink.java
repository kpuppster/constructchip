package io.github.constructchip.ghost;

import io.github.constructchip.capture.CaptureService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * While the chip is INSTALLED, the construct floats around its host: adventure mode with flight, silent (no footsteps
 * for others to hear), never hungry, never burning or drowning, and leashed. More than {@link #LEASH} blocks away or
 * in another dimension, they snap back to the host.
 */
public final class GhostLink {
    public static final double LEASH = 16.0;

    private GhostLink() {}

    public static void tickInstalled(ServerPlayer construct, ServerPlayer host) {
        if (construct.gameMode.getGameModeForPlayer() != CaptureService.CONSTRUCT_GAME_MODE) {
            construct.setGameMode(CaptureService.CONSTRUCT_GAME_MODE);
        }
        if (!construct.getAbilities().mayfly) {
            construct.getAbilities().mayfly = true;
            construct.onUpdateAbilities();
        }
        if (!construct.isSilent()) construct.setSilent(true);
        if (construct.getFoodData().getFoodLevel() < 20) construct.getFoodData().setFoodLevel(20);
        if (construct.isOnFire()) construct.clearFire();
        if (construct.getAirSupply() < construct.getMaxAirSupply()) construct.setAirSupply(construct.getMaxAirSupply());

        if (host == null || host.isRemoved()) return; // host offline: wait where we are
        boolean otherLevel = construct.level() != host.level();
        if (otherLevel || construct.distanceToSqr(host) > LEASH * LEASH) {
            construct.teleportTo((ServerLevel) host.level(), host.getX(), host.getY() + 0.5, host.getZ(),
                    construct.getYRot(), construct.getXRot());
        }
    }

    /** Undo the ghost-only flags when the player stops being a construct (game mode is restored separately). */
    public static void clear(ServerPlayer player) {
        player.setSilent(false);
        if (player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE || player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }
}
