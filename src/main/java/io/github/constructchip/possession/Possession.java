package io.github.constructchip.possession;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.attachment.ModAttachments;
import io.github.constructchip.capture.CaptureService;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.QuickhackService;
import io.github.constructchip.network.Net;
import io.github.constructchip.network.PossessionPayload;
import io.github.constructchip.sync.ChipSync;
import io.github.constructchip.tracking.ChipTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Takeover: the construct takes over its host's body for up to {@link #DURATION_TICKS}.
 *
 * <p>On start the construct steps into the host's place: the host's position, inventory (captured chips stay with the
 * host), health, hunger, XP and effects move onto the construct, which plays in the host's game mode and is drawn with
 * the host's skin and name (see the Player / AbstractClientPlayer mixins). The host turns spectator with the camera
 * locked on the construct, so they watch through its eyes. While it runs the construct is not a ghost (it can be seen,
 * hurt and build) and its chat comes out of the host's mouth.
 *
 * <p>It ends when the construct uses Takeover again, when time runs out, when either player leaves, when the chip
 * leaves the host, or when the server stops. Everything goes back to the host, who reappears where the body is, and
 * the {@link #COOLDOWN_TICKS} cooldown starts. If the body dies, the host dies there.
 *
 * <p>Both players carry a {@link PossessionRecord} while it runs; a crash is undone at the next login
 * ({@link #recover}), with the host's belongings delivered through {@link PendingBodies} if they're offline.
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class Possession {
    public static final int DURATION_TICKS = 5 * 60 * 20;
    public static final int COOLDOWN_TICKS = 30 * 60 * 20;
    public static final ResourceLocation HACK_ID = ConstructChip.id("takeover");

    public enum Reason { RELINQUISHED, TIME_UP, BODY_DIED, LEFT, CHIP_REMOVED, RELEASED, SERVER_STOP }

    private record Session(UUID host, long endsAt, List<ItemStack> constructLeftovers) {}

    /** construct UUID → session. Server only. */
    private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();

    /** Client mirror (construct → host), set from {@link PossessionPayload}. */
    public static volatile Map<UUID, UUID> clientPairs = Map.of();
    /** Client name lookup for the display-name mixin, installed by the client setup. */
    public static volatile Function<Player, Component> clientBodyName = p -> null;

    private Possession() {}

    // ------------------------------------------------------------------ queries

    /** Is this player a construct currently in its host's body? Works on both sides. */
    public static boolean isPossessing(Player p) {
        return p.level().isClientSide ? clientPairs.containsKey(p.getUUID()) : ACTIVE.containsKey(p.getUUID());
    }

    public static boolean isPossessingId(UUID construct) {
        return ACTIVE.containsKey(construct);
    }

    private static boolean isHostInUse(UUID host) {
        return ACTIVE.values().stream().anyMatch(s -> s.host().equals(host));
    }

    /** The name a possessing construct shows (its host's), or null. */
    public static Component bodyName(Player p) {
        if (p.level().isClientSide) return clientBodyName.apply(p);
        Session s = ACTIVE.get(p.getUUID());
        if (s == null || !(p instanceof ServerPlayer sp)) return null;
        ServerPlayer host = sp.server.getPlayerList().getPlayer(s.host());
        return host == null ? null : host.getDisplayName();
    }

    // ------------------------------------------------------------------ start / end

    public static HackResult start(ServerPlayer construct, ServerPlayer host) {
        if (ACTIVE.containsKey(construct.getUUID()) || isHostInUse(host.getUUID())) {
            return HackResult.fail(Component.translatable("message.constructchip.takeover.busy"));
        }
        if (host.isSleeping()) return HackResult.fail(Component.translatable("message.constructchip.takeover.sleeping"));
        if (host.isPassenger()) host.stopRiding();
        MinecraftServer server = host.server;
        ServerLevel level = host.serverLevel();

        // anything the construct somehow carries is set aside and given back afterwards
        List<ItemStack> leftovers = drain(construct.getInventory());
        Inventory from = host.getInventory(), to = construct.getInventory();
        for (int slot = 0; slot < from.getContainerSize(); slot++) {
            ItemStack stack = from.getItem(slot);
            if (stack.isEmpty() || ChipStacks.isCaptured(stack)) continue; // chips stay with the host
            to.setItem(slot, stack);
            from.setItem(slot, ItemStack.EMPTY);
        }
        to.selected = from.selected;

        GameType mode = host.gameMode.getGameModeForPlayer();
        host.setData(ModAttachments.POSSESSION, PossessionRecord.onHost(construct.getUUID(), mode));
        construct.setData(ModAttachments.POSSESSION, PossessionRecord.onConstruct(host.getUUID()));
        ACTIVE.put(construct.getUUID(), new Session(host.getUUID(), level.getGameTime() + DURATION_TICKS, leftovers));

        construct.teleportTo(level, host.getX(), host.getY(), host.getZ(), host.getYRot(), host.getXRot());
        construct.setYHeadRot(host.getYHeadRot());
        construct.setSilent(false);
        construct.setGameMode(mode == GameType.SPECTATOR ? GameType.SURVIVAL : mode);
        construct.getAbilities().flying = false;
        construct.onUpdateAbilities();
        copyVitals(host, construct);
        host.removeAllEffects();

        host.setGameMode(GameType.SPECTATOR);
        host.setCamera(construct);
        construct.inventoryMenu.broadcastChanges();
        host.inventoryMenu.broadcastChanges();

        sync(server);
        ChipSync.pushLinks(server);
        host.displayClientMessage(Component.translatable("message.constructchip.takeover.host_start", construct.getGameProfile().getName()), false);
        ConstructChip.LOGGER.info("{} took over {}'s body", construct.getGameProfile().getName(), host.getGameProfile().getName());
        return HackResult.success(Component.translatable("message.constructchip.takeover.start", host.getGameProfile().getName()));
    }

    /** Ends the construct's takeover (no-op if none). */
    public static void end(MinecraftServer server, UUID constructId, Reason reason) {
        Session s = ACTIVE.remove(constructId);
        if (s == null) return;
        ServerPlayer construct = server.getPlayerList().getPlayer(constructId);
        ServerPlayer host = server.getPlayerList().getPlayer(s.host());

        if (construct != null) {
            if (host != null) {
                Inventory from = construct.getInventory(), to = host.getInventory();
                List<ItemStack> extras = new ArrayList<>();
                for (int slot = 0; slot < from.getContainerSize(); slot++) {
                    ItemStack stack = from.getItem(slot);
                    if (stack.isEmpty()) continue;
                    from.setItem(slot, ItemStack.EMPTY);
                    if (to.getItem(slot).isEmpty()) to.setItem(slot, stack);
                    else extras.add(stack);
                }
                to.selected = from.selected;
                for (ItemStack extra : extras) give(host, extra);
                restoreHost(host);
                host.teleportTo(construct.serverLevel(), construct.getX(), construct.getY(), construct.getZ(),
                        construct.getYRot(), construct.getXRot());
                copyVitals(construct, host);
            } else {
                PendingBodies.get(server).add(s.host(), drain(construct.getInventory()));
            }
            resetConstruct(construct, s.constructLeftovers());
        } else if (host != null) {
            restoreHost(host); // the body's items stay on the construct until it logs back in (recover)
        }

        if (host != null) {
            startCooldown(host);
            host.displayClientMessage(Component.translatable("message.constructchip.takeover.host_end"), false);
            host.inventoryMenu.broadcastChanges();
        }
        if (construct != null) {
            construct.displayClientMessage(Component.translatable("message.constructchip.takeover.end",
                    Component.translatable("message.constructchip.takeover.reason." + reason.name().toLowerCase(java.util.Locale.ROOT))), false);
        }
        sync(server);
        ChipSync.pushLinks(server);
    }

    /** End any takeover the player is part of (as construct or host). */
    public static void endFor(ServerPlayer player, Reason reason) {
        if (ACTIVE.containsKey(player.getUUID())) end(player.server, player.getUUID(), reason);
        for (Map.Entry<UUID, Session> e : List.copyOf(ACTIVE.entrySet())) {
            if (e.getValue().host().equals(player.getUUID())) end(player.server, e.getKey(), reason);
        }
    }

    private static void startCooldown(ServerPlayer host) {
        ItemStack chip = ChipTracker.installedChip(host);
        if (chip.isEmpty()) return;
        long now = host.serverLevel().getGameTime();
        ChipStacks.setTimers(chip, ChipStacks.timers(chip).withCooldown(HACK_ID, COOLDOWN_TICKS, now));
        host.setData(ModAttachments.INSTALLED_CHIP, chip);
        ChipSync.syncHost(host);
    }

    private static void restoreHost(ServerPlayer host) {
        PossessionRecord rec = host.getData(ModAttachments.POSSESSION);
        if (host.getCamera() != host) host.setCamera(host);
        host.setGameMode(rec.previousMode().orElse(GameType.SURVIVAL));
        host.setData(ModAttachments.POSSESSION, PossessionRecord.NONE);
    }

    private static void resetConstruct(ServerPlayer construct, List<ItemStack> leftovers) {
        construct.getInventory().clearContent();
        for (ItemStack s : leftovers) construct.getInventory().add(s);
        construct.removeAllEffects();
        construct.setExperienceLevels(0);
        construct.experienceProgress = 0;
        construct.totalExperience = 0;
        construct.setHealth(construct.getMaxHealth());
        construct.getFoodData().setFoodLevel(20);
        construct.clearFire();
        construct.setSilent(true);
        construct.setGameMode(CaptureService.CONSTRUCT_GAME_MODE); // GhostLink takes it from here (flight, leash)
        construct.setData(ModAttachments.POSSESSION, PossessionRecord.NONE);
        construct.inventoryMenu.broadcastChanges();
    }

    /** The body's state goes with it: health, hunger, XP, effects, fire, air. */
    private static void copyVitals(ServerPlayer from, ServerPlayer to) {
        to.removeAllEffects();
        for (MobEffectInstance e : from.getActiveEffects()) to.addEffect(new MobEffectInstance(e));
        to.setExperienceLevels(from.experienceLevel);
        to.experienceProgress = from.experienceProgress;
        to.totalExperience = from.totalExperience;
        to.getFoodData().setFoodLevel(from.getFoodData().getFoodLevel());
        to.getFoodData().setSaturation(from.getFoodData().getSaturationLevel());
        to.setHealth(Math.max(1f, Math.min(to.getMaxHealth(), from.getHealth())));
        to.setAbsorptionAmount(from.getAbsorptionAmount());
        to.setRemainingFireTicks(from.getRemainingFireTicks());
        to.setAirSupply(from.getAirSupply());
    }

    private static List<ItemStack> drain(Inventory inv) {
        List<ItemStack> out = new ArrayList<>();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack s = inv.getItem(slot);
            if (!s.isEmpty()) out.add(s);
        }
        inv.clearContent();
        return out;
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack) && !stack.isEmpty()) p.drop(stack, false);
    }

    private static void sync(MinecraftServer server) {
        Net.sendToAll(server, payload());
    }

    private static PossessionPayload payload() {
        List<PossessionPayload.Entry> list = new ArrayList<>();
        ACTIVE.forEach((c, s) -> list.add(new PossessionPayload.Entry(c, s.host(), s.endsAt())));
        return new PossessionPayload(list);
    }

    // ------------------------------------------------------------------ events

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) return;
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        for (Map.Entry<UUID, Session> e : List.copyOf(ACTIVE.entrySet())) {
            ServerPlayer construct = server.getPlayerList().getPlayer(e.getKey());
            ServerPlayer host = server.getPlayerList().getPlayer(e.getValue().host());
            if (construct == null || host == null) {
                end(server, e.getKey(), Reason.LEFT);
            } else if (QuickhackService.hostOf(construct).map(h -> h != host).orElse(true)) {
                end(server, e.getKey(), Reason.CHIP_REMOVED);
            } else if (now >= e.getValue().endsAt()) {
                end(server, e.getKey(), Reason.TIME_UP);
            } else {
                // vanilla drops a spectator's camera when they sneak: keep the host behind the construct's eyes
                if (host.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) host.setGameMode(GameType.SPECTATOR);
                if (host.getCamera() != construct) host.setCamera(construct);
            }
        }
    }

    /** The body dies: the takeover ends and the host dies where the body fell. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer construct) || !ACTIVE.containsKey(construct.getUUID())) return;
        UUID hostId = ACTIVE.get(construct.getUUID()).host();
        DamageSource source = event.getSource();
        event.setCanceled(true);
        construct.setHealth(1f);
        end(construct.server, construct.getUUID(), Reason.BODY_DIED);
        ServerPlayer host = construct.server.getPlayerList().getPlayer(hostId);
        if (host != null && host.isAlive()) {
            host.invulnerableTime = 0;
            host.hurt(source, Float.MAX_VALUE);
            if (host.isAlive()) host.kill();
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) endFor(p, Reason.LEFT);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        recover(p);
        Net.send(p, payload());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        for (UUID c : List.copyOf(ACTIVE.keySet())) end(event.getServer(), c, Reason.SERVER_STOP);
    }

    /** Undo a takeover that a crash cut short, and hand over anything waiting for this player. */
    static void recover(ServerPlayer p) {
        MinecraftServer server = p.server;
        PossessionRecord rec = p.getData(ModAttachments.POSSESSION);
        if (rec.host().isPresent() && !ACTIVE.containsKey(p.getUUID())) {
            // was in a body: what it carries is the host's
            List<ItemStack> body = drain(p.getInventory());
            ServerPlayer host = server.getPlayerList().getPlayer(rec.host().get());
            if (host != null) body.forEach(s -> give(host, s));
            else PendingBodies.get(server).add(rec.host().get(), body);
            resetConstruct(p, List.of());
            ConstructChip.LOGGER.info("Undid an interrupted takeover by {}", p.getGameProfile().getName());
        }
        if (rec.construct().isPresent() && !isHostInUse(p.getUUID())) restoreHost(p);
        for (ItemStack s : PendingBodies.get(server).take(p.getUUID())) give(p, s);
    }
}
