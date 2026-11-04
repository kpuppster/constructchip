package io.github.constructchip.compat.vista;

import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import java.util.UUID;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.vista.common.broadcast.BroadcastManager;
import net.mehvahdjukaar.vista.common.broadcast.IBroadcastLocation;
import net.mehvahdjukaar.vista.common.tv.PowerState;
import net.mehvahdjukaar.vista.common.tv.TVBlock;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.network.ClientBoundControlViewFinderPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The three Vista quickhacks. See {@link VistaCompat} for the state they leave behind. */
public final class VistaHacks {
    private VistaHacks() {}

    /** Turn a TV on or off; held for 60 s against redstone updates. */
    public static final class TvPower extends SimpleQuickhack {
        public TvPower() { super(1f, 20, TargetType.BLOCK, 24); }

        @Override
        public HackResult execute(HackContext ctx) {
            BlockPos pos = ctx.block().orElseThrow();
            ServerLevel level = ctx.level();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TVBlock)) return HackResult.fail(Component.translatable("message.constructchip.vista.not_tv"));
            PowerState next = state.getValue(TVBlock.POWER_STATE).isOn() ? PowerState.OFF : PowerState.DIRECT;
            level.setBlock(pos, state.setValue(TVBlock.POWER_STATE, next), Block.UPDATE_ALL);
            VistaCompat.FORCED_TVS.put(new VistaCompat.ForcedKey(level.dimension(), pos.immutable()),
                    new VistaCompat.Forced(next, ctx.now() + 1200));
            return HackResult.success(Component.translatable(next.isOn() ? "message.constructchip.vista.tv_on" : "message.constructchip.vista.tv_off"));
        }
    }

    /** Take control of a View Finder and see through it (Vista's own client camera controller). */
    public static final class CameraView extends SimpleQuickhack {
        public CameraView() { super(2f, 100, TargetType.BLOCK, 16); }

        @Override
        public HackResult execute(HackContext ctx) {
            BlockEntity be = ctx.level().getBlockEntity(ctx.block().orElseThrow());
            if (!(be instanceof ViewFinderBlockEntity camera)) return HackResult.fail(Component.translatable("message.constructchip.vista.not_camera"));
            ServerPlayer construct = ctx.construct();
            UUID user = camera.getCurrentUser();
            if (user != null && !user.equals(construct.getUUID()) && ctx.level().getServer().getPlayerList().getPlayer(user) != null) {
                return HackResult.fail(Component.translatable("message.constructchip.vista.camera_busy"));
            }
            camera.setCurrentUser(construct.getUUID());
            NetworkHelper.sendToClientPlayer(construct, new ClientBoundControlViewFinderPacket(TileOrEntityTarget.of(camera)));
            return HackResult.success(Component.translatable("message.constructchip.vista.camera_view"));
        }
    }

    /** Cut a View Finder's feed for 20 s. Linked TVs lose the picture. */
    public static final class CameraBlind extends SimpleQuickhack {
        public static final int DURATION = 400;

        public CameraBlind() { super(4f, 30 * 20, TargetType.BLOCK, 24); }

        @Override
        public HackResult execute(HackContext ctx) {
            BlockEntity be = ctx.level().getBlockEntity(ctx.block().orElseThrow());
            if (!(be instanceof ViewFinderBlockEntity camera)) return HackResult.fail(Component.translatable("message.constructchip.vista.not_camera"));
            UUID feed = camera.getBroadcastUUID();
            if (feed == null) return HackResult.fail(Component.translatable("message.constructchip.vista.no_feed"));
            if (VistaCompat.BLINDED.containsKey(feed)) return HackResult.fail(Component.translatable("message.constructchip.vista.already_blind"));
            BroadcastManager manager = BroadcastManager.getInstance(ctx.level());
            IBroadcastLocation location = manager.getFeedLocationById(feed);
            if (location == null) return HackResult.fail(Component.translatable("message.constructchip.vista.no_feed"));
            manager.unlinkFeed(feed);
            VistaCompat.BLINDED.put(feed, new VistaCompat.Blind(ctx.level().dimension(), location,
                    ctx.level().getServer().overworld().getGameTime() + DURATION));
            return HackResult.success(Component.translatable("message.constructchip.vista.blind", DURATION / 20));
        }
    }
}
