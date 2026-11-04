package io.github.constructchip.chip;

import io.github.constructchip.registry.ModEntities;
import io.github.constructchip.tracking.ChipTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * A dropped chip: invulnerable, never despawns, rescued from the void, and heartbeats to the registry every tick.
 * The construct cannot pick up their own chip.
 */
public class ChipItemEntity extends ItemEntity {
    private Vec3 lastSafePos;

    public ChipItemEntity(EntityType<? extends ChipItemEntity> type, Level level) {
        super(type, level);
        setUnlimitedLifetime();
    }

    public ChipItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(ModEntities.CHIP_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
        setDeltaMovement(level.random.nextDouble() * 0.2 - 0.1, 0.2, level.random.nextDouble() * 0.2 - 0.1);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) return;
        if (onGround()) lastSafePos = position();
        ChipTracker.heartbeatDropped(this);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false; // cactus, lava, fire, explosions, anvils: nothing destroys the chip except /kill
    }

    @Override
    protected void onBelowWorld() {
        // Vanilla discards items that fall out of the world. Put the chip back on solid ground instead.
        Vec3 target = lastSafePos;
        if (target == null && level() instanceof ServerLevel server) {
            BlockPos spawn = server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, server.getSharedSpawnPos());
            target = Vec3.atBottomCenterOf(spawn);
        }
        if (target == null) {
            super.onBelowWorld();
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        teleportTo(target.x, target.y + 0.5, target.z);
    }

    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide) return;
        if (ChipTracker.isConstruct(player)) return; // constructs can't pick anything up
        ItemStack before = getItem().copy();
        super.playerTouch(player);
        if (isRemoved()) ChipTracker.onPickedUp(before, player);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.KILLED && level() instanceof ServerLevel server) {
            ChipTracker.onDestroyed(server.getServer(), getItem());
        }
        super.remove(reason);
    }
}
