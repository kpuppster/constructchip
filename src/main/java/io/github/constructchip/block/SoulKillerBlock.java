package io.github.constructchip.block;

import com.mojang.serialization.MapCodec;
import io.github.constructchip.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
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
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Soul Killer: a capture chair. Right-click with an empty hand to sit in it; an operator right-clicks it with a
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
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    private static final VoxelShape BASE = Shapes.or(Block.box(3, 0, 3, 13, 6, 13), Block.box(1, 6, 1, 15, 9, 15));
    private static final java.util.Map<Direction, VoxelShape> SHAPES = new java.util.EnumMap<>(Direction.class);
    static {
        // backrest + headset on the side opposite FACING (the sitter faces FACING)
        SHAPES.put(Direction.NORTH, Shapes.or(BASE, Block.box(1, 9, 13, 15, 16, 16)));
        SHAPES.put(Direction.SOUTH, Shapes.or(BASE, Block.box(1, 9, 0, 15, 16, 3)));
        SHAPES.put(Direction.WEST, Shapes.or(BASE, Block.box(13, 9, 1, 16, 16, 15)));
        SHAPES.put(Direction.EAST, Shapes.or(BASE, Block.box(0, 9, 1, 3, 16, 15)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.getOrDefault(state.getValue(FACING), BASE);
    }

    /** Empty hand: sit down (if the chair is free). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() || player.isPassenger()) return InteractionResult.PASS;
        if (!level.getEntitiesOfClass(ChairSeatEntity.class, new AABB(pos)).isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            ChairSeatEntity seat = new ChairSeatEntity(level, pos.getX() + 0.5, pos.getY() + 0.45, pos.getZ() + 0.5);
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
