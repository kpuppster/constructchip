package io.github.constructchip.block;

import com.mojang.serialization.MapCodec;
import io.github.constructchip.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import io.github.constructchip.Config;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Soul Killer: a capture chair, drawn as A24-N22's ripperdoc station (Project: Edgerunner, used with permission). Right-click with an empty hand to sit in it; an operator right-clicks it with a
 * blank chip to start a capture of whoever sits there. Needs redstone power. The logic is in
 * {@link SoulKillerBlockEntity}; ACTIVE lights it up while asking and capturing.
 */
public class SoulKillerBlock extends BaseEntityBlock {
    public static final MapCodec<SoulKillerBlock> CODEC = simpleCodec(SoulKillerBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public SoulKillerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    /** Drawn by GeckoLib (SoulKillerRenderer); the JSON block model is only the item icon and break particles. */
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    /** Clickable/collision part: the chair base. The rest of the station (lights, arm, table, stool) is decorative. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 10, 16);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** Where the sitter sits: the configured offset (for a north-facing chair) rotated with FACING. */
    public static Vec3 seatPosition(BlockPos pos, Direction facing) {
        double x = Config.SEAT_X.get(), z = Config.SEAT_Z.get();
        double rx, rz;
        switch (facing) {
            case SOUTH -> { rx = -x; rz = -z; }
            case WEST -> { rx = z; rz = -x; }
            case EAST -> { rx = -z; rz = x; }
            default -> { rx = x; rz = z; }
        }
        return new Vec3(pos.getX() + 0.5 + rx, pos.getY() + Config.SEAT_Y.get(), pos.getZ() + 0.5 + rz);
    }

    /** Empty hand: sit down (if the chair is free). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) { // Ripperdoc surgery
            if (player instanceof ServerPlayer sp) {
                sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> io.github.constructchip.cyberware.SurgeryMenu.open(id, inv, sp),
                        Component.translatable("container.constructchip.surgery")));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isPassenger()) return InteractionResult.PASS;
        if (!level.getEntitiesOfClass(ChairSeatEntity.class, new AABB(pos).inflate(3), s -> s.chair().equals(pos)).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ChairSeatEntity seat = new ChairSeatEntity(level, pos, seatPosition(pos, state.getValue(FACING)));
            level.addFreshEntity(seat);
            player.startRiding(seat);
            player.setYRot(state.getValue(FACING).toYRot());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoulKillerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.SOUL_KILLER.get(), SoulKillerBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SoulKillerBlockEntity machine)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // sit
        if (player instanceof ServerPlayer operator) machine.onUse(operator, hand);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SoulKillerBlockEntity machine) {
            machine.abort(net.minecraft.network.chat.Component.translatable("message.constructchip.soul_killer.broken"));
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    static BlockState withActive(BlockState state, boolean active) {
        return state.hasProperty(ACTIVE) ? state.setValue(ACTIVE, active) : state;
    }

}
