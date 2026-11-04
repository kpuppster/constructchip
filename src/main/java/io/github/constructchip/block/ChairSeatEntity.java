package io.github.constructchip.block;

import io.github.constructchip.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Invisible seat for the Soul Killer chair. Exists only while someone sits on it and the chair is still there.
 * The sitter's feet origin is put exactly on the seat, and their body is locked to the chair's facing (the seat's
 * own yaw, which reaches clients in the spawn packet). Looking around only turns the head, up to {@link #HEAD_LIMIT}
 * degrees either side, like sitting in a boat.
 */
public class ChairSeatEntity extends Entity {
    /** How far (degrees) the sitter can turn their head away from the chair's facing. */
    private static final float HEAD_LIMIT = 100f;
    private BlockPos chair = BlockPos.ZERO;

    public ChairSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public ChairSeatEntity(Level level, BlockPos chair, Vec3 at, float yaw) {
        this(ModEntities.CHAIR_SEAT.get(), level);
        this.chair = chair.immutable();
        setPos(at.x, at.y, at.z);
        setYRot(yaw);
        yRotO = yaw;
    }

    public BlockPos chair() {
        return chair;
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
        callback.accept(passenger, getX(), getY(), getZ());
        lockBody(passenger);
    }

    @Override
    public void onPassengerTurned(Entity entity) {
        lockBody(entity);
    }

    /** Body faces the chair; the view (and with it the head) may turn at most HEAD_LIMIT degrees away. */
    private void lockBody(Entity passenger) {
        float seatYaw = getYRot();
        if (passenger instanceof LivingEntity living) {
            living.setYBodyRot(seatYaw);
            living.yBodyRotO = seatYaw;
        }
        float off = Mth.wrapDegrees(passenger.getYRot() - seatYaw);
        float clamped = Mth.clamp(off, -HEAD_LIMIT, HEAD_LIMIT);
        passenger.yRotO += clamped - off;
        passenger.setYRot(passenger.getYRot() + clamped - off);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    public void setDeltaMovement(Vec3 motion) {}

    @Override
    public void tick() {
        if (level().isClientSide) return;
        boolean chairThere = level().getBlockState(chair).getBlock() instanceof SoulKillerBlock;
        if (!isVehicle() || !chairThere) discard();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 fallback = super.getDismountLocationForPassenger(passenger).add(0, 0.5, 0);
        net.minecraft.world.level.block.state.BlockState state = level().getBlockState(chair);
        if (!(state.getBlock() instanceof SoulKillerBlock)) return fallback;
        // step out beside the station, never into its collision box
        return SafeSpot.besideChair(level(), chair, state.getValue(SoulKillerBlock.FACING), fallback);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
