package io.github.constructchip.block;

import io.github.constructchip.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Invisible seat for the Soul Killer chair. Exists only while someone sits on it and the chair is still there.
 * (Seat positioning follows the approach of Create's seats, which are MIT-licensed.)
 */
public class ChairSeatEntity extends Entity {
    public ChairSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public ChairSeatEntity(Level level, double x, double y, double z) {
        this(ModEntities.CHAIR_SEAT.get(), level);
        setPos(x, y, z);
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        AABB bb = getBoundingBox();
        setBoundingBox(bb.move(new Vec3(x, y, z).subtract(bb.getCenter())));
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger)) return;
        double height = getPassengerRidingPosition(passenger).y - passenger.getVehicleAttachmentPoint(this).y;
        callback.accept(passenger, getX(), height + 1.0 / 16.0, getZ());
    }

    @Override
    public void onPassengerTurned(Entity entity) {
        entity.setYHeadRot(entity.getYRot());
    }

    @Override
    public void setDeltaMovement(Vec3 motion) {}

    @Override
    public void tick() {
        if (level().isClientSide) return;
        boolean chairThere = level().getBlockState(blockPosition()).getBlock() instanceof SoulKillerBlock;
        if (!isVehicle() || !chairThere) discard();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return super.getDismountLocationForPassenger(passenger).add(0, 0.5, 0);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
