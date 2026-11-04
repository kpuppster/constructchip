package io.github.constructchip.compat.vista;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.hack.ModQuickhacks;
import io.github.constructchip.hack.Quickhack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.mehvahdjukaar.vista.common.broadcast.BroadcastManager;
import net.mehvahdjukaar.vista.common.broadcast.IBroadcastLocation;
import net.mehvahdjukaar.vista.common.tv.PowerState;
import net.mehvahdjukaar.vista.common.tv.TVBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Everything that touches Vista. Only class-loaded when Vista is present ({@code ConstructChip} checks
 * {@code ModList.get().isLoaded("vista")} before calling {@link #init}).
 *
 * <p>Two pieces of state need care. Forced TV power: Vista recomputes POWER_STATE on neighbor updates, so a hacked
 * TV is re-forced every tick for 60 s. Blinded feeds: unlinked from Vista's BroadcastManager and relinked after
 * 20 s, or when the server stops, so a feed is never left cut for good.
 */
public final class VistaCompat {
    public static DeferredHolder<Quickhack, VistaHacks.TvPower> TV_POWER;
    public static DeferredHolder<Quickhack, VistaHacks.CameraView> CAMERA_VIEW;
    public static DeferredHolder<Quickhack, VistaHacks.CameraBlind> CAMERA_BLIND;

    record ForcedKey(ResourceKey<Level> dimension, BlockPos pos) {}
    record Forced(PowerState state, long until) {}
    record Blind(ResourceKey<Level> dimension, IBroadcastLocation location, long until) {}

    static final Map<ForcedKey, Forced> FORCED_TVS = new HashMap<>();
    static final Map<UUID, Blind> BLINDED = new HashMap<>();

    private VistaCompat() {}

    public static void init(IEventBus modBus) {
        TV_POWER = ModQuickhacks.REGISTER.register("tv_power", VistaHacks.TvPower::new);
        CAMERA_VIEW = ModQuickhacks.REGISTER.register("camera_view", VistaHacks.CameraView::new);
        CAMERA_BLIND = ModQuickhacks.REGISTER.register("camera_blind", VistaHacks.CameraBlind::new);
        NeoForge.EVENT_BUS.addListener(VistaCompat::onServerTick);
        NeoForge.EVENT_BUS.addListener(VistaCompat::onServerStopping);
        ConstructChip.LOGGER.info("Vista found: TV and camera quickhacks enabled");
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<ForcedKey, Forced>> tvs = FORCED_TVS.entrySet().iterator();
        while (tvs.hasNext()) {
            Map.Entry<ForcedKey, Forced> e = tvs.next();
            if (now > e.getValue().until()) {
                tvs.remove();
                continue;
            }
            ServerLevel level = server.getLevel(e.getKey().dimension());
            BlockPos pos = e.getKey().pos();
            if (level == null || !level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TVBlock)) {
                tvs.remove();
                continue;
            }
            if (state.getValue(TVBlock.POWER_STATE) != e.getValue().state()) {
                level.setBlock(pos, state.setValue(TVBlock.POWER_STATE, e.getValue().state()), Block.UPDATE_ALL);
            }
        }
        Iterator<Map.Entry<UUID, Blind>> blinds = BLINDED.entrySet().iterator();
        while (blinds.hasNext()) {
            Map.Entry<UUID, Blind> e = blinds.next();
            if (now > e.getValue().until()) {
                relink(server, e.getKey(), e.getValue());
                blinds.remove();
            }
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        BLINDED.forEach((feed, blind) -> relink(event.getServer(), feed, blind));
        BLINDED.clear();
        FORCED_TVS.clear();
    }

    private static void relink(MinecraftServer server, UUID feed, Blind blind) {
        ServerLevel level = server.getLevel(blind.dimension());
        if (level == null) level = server.overworld();
        try {
            BroadcastManager.getInstance(level).linkFeed(feed, blind.location());
        } catch (RuntimeException ex) {
            ConstructChip.LOGGER.warn("Could not relink Vista feed {} after Camera Blind", feed, ex);
        }
    }
}
