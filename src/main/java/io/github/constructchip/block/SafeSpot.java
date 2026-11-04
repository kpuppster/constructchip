package io.github.constructchip.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Finds somewhere a player can stand that isn't inside (or on top of) a Soul Killer station. Getting out of the
 * chair and being released from a chip both used to put the player on the chair spot, inside the station's low
 * collision box: their movement glitched, and their body sat where the host clicks and builds.
 */
public final class SafeSpot {
    private SafeSpot() {}

    /** Beside the chair: in front of it first, then the sides, then anywhere within 3 blocks. */
    public static Vec3 besideChair(Level level, BlockPos chair, Direction facing, Vec3 fallback) {
        Direction[] order = {facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite()};
        for (Direction d : order) {
            Vec3 v = standable(level, chair.relative(d));
            if (v != null) return v;
        }
        Vec3 near = near(level, chair, 3);
        return near != null ? near : fallback;
    }

    /** {@code want} if a player fits there and it isn't a Soul Killer; otherwise the nearest such spot within 3 blocks. */
    public static Vec3 forPlayer(Level level, Player player, Vec3 want) {
        BlockPos at = BlockPos.containing(want);
        if (!isStation(level, at) && !isStation(level, at.below())
                && level.noCollision(player, player.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(want))) {
            return want;
        }
        Vec3 near = near(level, at, 3);
        return near != null ? near : want;
    }

    private static Vec3 near(Level level, BlockPos center, int radius) {
        for (int r = 1; r <= radius; r++) {
            for (int dy : new int[]{0, 1, -1, 2}) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue; // ring only
                        Vec3 v = standable(level, center.offset(dx, dy, dz));
                        if (v != null) return v;
                    }
                }
            }
        }
        return null;
    }

    private static Vec3 standable(Level level, BlockPos pos) {
        if (isStation(level, pos) || isStation(level, pos.below())) return null;
        return DismountHelper.findSafeDismountLocation(EntityType.PLAYER, level, pos, false);
    }

    private static boolean isStation(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof SoulKillerBlock;
    }
}
