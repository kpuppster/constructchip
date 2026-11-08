package io.github.constructchip.sync;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.chip.ChipSkills;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.hack.QuickhackService;
import io.github.constructchip.network.ChipStatusPayload;
import io.github.constructchip.network.ConstructLinksPayload;
import io.github.constructchip.network.HostStatusPayload;
import io.github.constructchip.skill.ChipStats;
import io.github.constructchip.skill.ModSkills;
import io.github.constructchip.skill.Skill;
import io.github.constructchip.skill.SkillPointCost;
import io.github.constructchip.tracking.ChipEntry;
import io.github.constructchip.tracking.ChipLocation;
import io.github.constructchip.tracking.ChipRegistry;
import io.github.constructchip.tracking.ChipTracker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import io.github.constructchip.network.Net;

/**
 * Keeps clients informed. Every player gets the construct→host map (for render hiding) whenever it changes.
 * Construct and host get the chip status every second plus right after anything changes it. The construct gets the
 * host's vitals twice a second.
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class ChipSync {
    public static final ResourceLocation VITALS_LINK = ConstructChip.id("vitals_link");
    private static Map<UUID, UUID> lastLinks = Map.of();

    private ChipSync() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        if (tick % 10 == 0) {
            Map<UUID, UUID> links = computeLinks(server);
            if (!links.equals(lastLinks)) {
                lastLinks = links;
                Net.sendToAll(server, new ConstructLinksPayload(links));
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (ChipTracker.isConstruct(player)) QuickhackService.hostOf(player).ifPresent(host -> sendHostStatus(player, host));
            }
        }
        if (tick % 20 == 5) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) sendChipStatus(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Net.send(player, new ConstructLinksPayload(computeLinks(player.server)));
        sendChipStatus(player);
    }

    public static Map<UUID, UUID> computeLinks(MinecraftServer server) {
        Map<UUID, UUID> links = new HashMap<>();
        for (ChipEntry entry : ChipRegistry.get(server).all().values()) {
            ChipLocation loc = entry.location();
            if (loc.state() == ChipLocation.State.INSTALLED && loc.holder().isPresent()
                    && !io.github.constructchip.possession.Possession.isPossessingId(entry.capturedUuid())) { // in the body: not a ghost
                links.put(entry.capturedUuid(), loc.holder().get());
            }
        }
        return links;
    }

    /** Send the construct links now instead of on the next half-second sync (Takeover start/end). */
    public static void pushLinks(MinecraftServer server) {
        lastLinks = computeLinks(server);
        Net.sendToAll(server, new ConstructLinksPayload(lastLinks));
    }

    /** Push fresh chip status to the construct and host of {@code chipId} right now. */
    public static void syncChip(MinecraftServer server, UUID chipId) {
        ChipRegistry.get(server).get(chipId).ifPresent(entry -> {
            ServerPlayer construct = server.getPlayerList().getPlayer(entry.capturedUuid());
            if (construct != null) sendChipStatus(construct);
            entry.location().holder().map(server.getPlayerList()::getPlayer).ifPresent(ChipSync::sendChipStatus);
        });
    }

    /** Sync both players attached to the chip installed in {@code host}. */
    public static void syncHost(ServerPlayer host) {
        ChipStacks.identity(ChipTracker.installedChip(host)).ifPresentOrElse(
                id -> syncChip(host.server, id.chipId()), () -> sendChipStatus(host));
    }

    public static void sendChipStatus(ServerPlayer player) {
        ServerPlayer host;
        boolean isHost;
        if (ChipTracker.isConstruct(player)) {
            Optional<ServerPlayer> h = QuickhackService.hostOf(player);
            if (h.isEmpty()) {
                Net.send(player, ChipStatusPayload.NONE);
                return;
            }
            host = h.get();
            isHost = false;
        } else {
            host = player;
            isHost = true;
        }
        ItemStack chip = ChipTracker.installedChip(host);
        if (chip.isEmpty() || !ChipStacks.isCaptured(chip)) {
            Net.send(player, ChipStatusPayload.NONE);
            return;
        }
        String constructName = ChipStacks.identity(chip).map(id -> id.capturedName()).orElse("?");
        Registry<Skill> skills = ModSkills.get(player.server.registryAccess());
        ChipSkills chipSkills = ChipStacks.skills(chip);
        ChipStats stats = ChipStats.of(chipSkills, skills);
        Net.send(player, new ChipStatusPayload(true, isHost, constructName,
                host.getGameProfile().getName(), ChipStacks.ram(chip), ChipStacks.timers(chip), chipSkills,
                stats.ramCostMultiplier(), SkillPointCost.costOf(chipSkills.purchased())));
    }

    private static void sendHostStatus(ServerPlayer construct, ServerPlayer host) {
        ItemStack chip = ChipTracker.installedChip(host);
        Registry<Skill> skills = ModSkills.get(host.server.registryAccess());
        boolean vitals = ChipStacks.skills(chip).has(VITALS_LINK) && skills.containsKey(VITALS_LINK);
        List<MobEffectInstance> effects = vitals ? List.copyOf(host.getActiveEffects()) : List.of();
        List<ItemStack> wornOut = new ArrayList<>();
        if (vitals) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = host.getItemBySlot(slot);
                if (stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() * 0.9) wornOut.add(stack.copy());
            }
        }
        Net.send(construct, new HostStatusPayload(host.getHealth(), host.getMaxHealth(),
                host.getFoodData().getFoodLevel(), host.getFoodData().getSaturationLevel(), host.getArmorValue(),
                host.getAirSupply(), effects, wornOut));
    }
}
