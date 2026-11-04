package io.github.constructchip.hack.builtin;

import io.github.constructchip.block.SignalBlock;
import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import io.github.constructchip.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * World interaction as the host. Both hacks first fire a normal right-click event for the host on the target block,
 * so claim/protection mods can refuse, and they respect spawn protection.
 */
public final class DeviceHacks {
    private DeviceHacks() {}

    /** True if the host may act on {@code pos} (spawn protection + other mods' right-click veto). */
    static boolean allowed(ServerPlayer host, BlockPos pos, Direction face) {
        ServerLevel level = host.serverLevel();
        if (!level.mayInteract(host, pos)) return false;
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
        return !CommonHooks.onRightClickBlock(host, InteractionHand.MAIN_HAND, pos, hit).isCanceled();
    }

    public static final class RemoteSwitch extends SimpleQuickhack {
        public RemoteSwitch() { super(1f, 20, TargetType.BLOCK, 16); }

        @Override
        public HackResult execute(HackContext ctx) {
            BlockPos pos = ctx.block().orElseThrow();
            ServerLevel level = ctx.level();
            ServerPlayer host = ctx.host();
            if (!allowed(host, pos, ctx.face())) return HackResult.fail(Component.translatable("message.constructchip.device.protected"));
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof LeverBlock lever) {
                lever.pull(state, level, pos, host);
            } else if (block instanceof ButtonBlock button) {
                if (state.getValue(ButtonBlock.POWERED)) return HackResult.fail(Component.translatable("message.constructchip.device.busy"));
                button.press(state, level, pos, host);
            } else if (block instanceof DoorBlock door) {
                door.setOpen(host, level, state, pos, !state.getValue(DoorBlock.OPEN));
            } else if (block instanceof TrapDoorBlock) {
                boolean open = !state.getValue(TrapDoorBlock.OPEN);
                level.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, open), Block.UPDATE_ALL);
                level.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1f, 1f);
                level.gameEvent(host, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            } else if (block instanceof FenceGateBlock) {
                state.useWithoutItem(level, host, new BlockHitResult(Vec3.atCenterOf(pos), ctx.face(), pos, false));
            } else {
                return HackResult.fail(Component.translatable("message.constructchip.device.not_switchable", block.getName()));
            }
            HackFx.sparks(level, Vec3.atCenterOf(pos));
            return HackResult.success(Component.translatable("message.constructchip.device.switched", block.getName()));
        }
    }

    public static final class SignalInject extends SimpleQuickhack {
        public SignalInject() { super(2f, 40, TargetType.BLOCK, 16); }

        @Override
        public HackResult execute(HackContext ctx) {
            BlockPos target = ctx.block().orElseThrow();
            ServerLevel level = ctx.level();
            Direction face = ctx.face();
            BlockPos at = target.relative(face);
            if (!level.getBlockState(at).isAir()) {
                // command targets have no real face: try any free side
                face = null;
                for (Direction d : Direction.values()) {
                    if (level.getBlockState(target.relative(d)).isAir()) {
                        face = d;
                        break;
                    }
                }
                if (face == null) return HackResult.fail(Component.translatable("message.constructchip.device.no_room"));
                at = target.relative(face);
            }
            if (!allowed(ctx.host(), target, face) || !level.mayInteract(ctx.host(), at)) {
                return HackResult.fail(Component.translatable("message.constructchip.device.protected"));
            }
            level.setBlock(at, ModBlocks.SIGNAL.get().defaultBlockState().setValue(SignalBlock.FACING, face), Block.UPDATE_ALL);
            level.playSound(null, at, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 1.8f);
            HackFx.sparks(level, Vec3.atCenterOf(at));
            return HackResult.success(Component.translatable("message.constructchip.device.signal", level.getBlockState(target).getBlock().getName()));
        }
    }
}
