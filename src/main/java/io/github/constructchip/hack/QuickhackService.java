package io.github.constructchip.hack;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.attachment.ConstructState;
import io.github.constructchip.attachment.ModAttachments;
import io.github.constructchip.chip.ChipRam;
import io.github.constructchip.chip.ChipSkills;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.chip.ChipTimers;
import io.github.constructchip.skill.ChipStats;
import io.github.constructchip.skill.ModSkills;
import io.github.constructchip.skill.Skill;
import io.github.constructchip.skill.SkillTree;
import io.github.constructchip.tracking.ChipEntry;
import io.github.constructchip.tracking.ChipLocation;
import io.github.constructchip.tracking.ChipRegistry;
import io.github.constructchip.tracking.ChipTracker;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The single server-side entry point for using a quickhack (from the keybind payload or the debug command).
 * Check order: construct → chip installed → host usable → chip valid → hack exists → unlocked → lockout → cooldown
 * → target type → target exists → range → line of sight → RAM → execute. RAM and cooldown are charged only on success.
 */
public final class QuickhackService {
    /** Players (construct and host) inside a running hack; step 5's interaction-cancel handlers let these through. */
    private static final Set<UUID> HACK_IN_PROGRESS = new HashSet<>();

    private QuickhackService() {}

    public static boolean isHackInProgress(Entity entity) {
        return HACK_IN_PROGRESS.contains(entity.getUUID());
    }

    public enum Status {
        OK, NOT_A_CONSTRUCT, CHIP_NOT_INSTALLED, HOST_OFFLINE, HOST_UNAVAILABLE, CHIP_INVALID, UNKNOWN_HACK,
        NOT_UNLOCKED, LOCKED_OUT, ON_COOLDOWN, NO_TARGET, WRONG_TARGET, TARGET_NOT_FOUND, OUT_OF_RANGE,
        NO_LINE_OF_SIGHT, NOT_ENOUGH_RAM, HACK_FAILED;

        public String translationKey() {
            return "message.constructchip.hack." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** @param message what to show the construct (a failure reason, or the hack's own output) */
    public record Attempt(Status status, Optional<Component> message) {
        public boolean ok() {
            return status == Status.OK;
        }
    }

    private static Attempt fail(Status status, Object... args) {
        return new Attempt(status, Optional.of(Component.translatable(status.translationKey(), args)));
    }

    public static Attempt use(ServerPlayer construct, ResourceLocation hackId, HackTarget target) {
        MinecraftServer server = construct.server;

        // --- who and where
        ConstructState state = construct.getData(ModAttachments.CONSTRUCT_STATE);
        if (!state.isConstruct()) return fail(Status.NOT_A_CONSTRUCT);
        UUID chipId = state.chipId().get();
        Optional<ChipEntry> entry = ChipRegistry.get(server).get(chipId);
        if (entry.isEmpty() || entry.get().location().state() != ChipLocation.State.INSTALLED
                || entry.get().location().holder().isEmpty()) {
            return fail(Status.CHIP_NOT_INSTALLED);
        }
        ServerPlayer host = server.getPlayerList().getPlayer(entry.get().location().holder().get());
        if (host == null) return fail(Status.HOST_OFFLINE);
        if (!host.isAlive() || host.isSpectator() || host == construct) return fail(Status.HOST_UNAVAILABLE);
        ItemStack chip = ChipTracker.installedChip(host);
        boolean sameChip = ChipStacks.identity(chip).map(id -> id.chipId().equals(chipId)).orElse(false);
        if (!sameChip || ChipTracker.currentEntry(server, chip).isEmpty()) return fail(Status.CHIP_INVALID);

        // --- what
        Quickhack hack = ModQuickhacks.REGISTRY.get(hackId);
        if (hack == null) return fail(Status.UNKNOWN_HACK, hackId.toString());
        Registry<Skill> skillRegistry = ModSkills.get(server.registryAccess());
        ChipSkills skills = ChipStacks.skills(chip);
        if (!SkillTree.grantsQuickhack(skills, skillRegistry, hackId)) return fail(Status.NOT_UNLOCKED, hackId.toString());

        // --- when (game time is shared by all dimensions)
        long now = host.serverLevel().getGameTime();
        ChipTimers timers = ChipStacks.timers(chip);
        if (timers.isLockedOut(now)) return fail(Status.LOCKED_OUT, seconds(timers.lockoutUntil() - now));
        ChipStats stats = ChipStats.of(skills, skillRegistry);
        boolean ignoreCooldown = stats.noCooldownsWhileOverclocked() && timers.isOverclocked(now);
        long cooldown = timers.cooldownRemaining(hackId, now);
        if (cooldown > 0 && !ignoreCooldown) return fail(Status.ON_COOLDOWN, seconds(cooldown));

        // --- target
        Optional<Entity> entity = Optional.empty();
        Optional<BlockPos> block = Optional.empty();
        if (hack.targetType() != Quickhack.TargetType.NONE) {
            if (!hack.targetType().accepts(target.kind())) {
                return fail(target.kind() == HackTarget.Kind.NONE ? Status.NO_TARGET : Status.WRONG_TARGET);
            }
            ServerLevel level = host.serverLevel();
            Vec3 eye = host.getEyePosition();
            double range = hack.range(stats);
            if (target.kind() == HackTarget.Kind.ENTITY) {
                Entity e = level.getEntity(target.entityId());
                if (e == null || !e.isAlive() || e == host || e == construct) return fail(Status.TARGET_NOT_FOUND);
                if (eye.distanceToSqr(closestPoint(e.getBoundingBox(), eye)) > range * range) {
                    return fail(Status.OUT_OF_RANGE, Math.round(range));
                }
                if (hack.requiresLineOfSight() && !host.hasLineOfSight(e)) return fail(Status.NO_LINE_OF_SIGHT);
                entity = Optional.of(e);
            } else {
                BlockPos pos = target.pos();
                if (!level.isLoaded(pos)) return fail(Status.TARGET_NOT_FOUND);
                Vec3 center = Vec3.atCenterOf(pos);
                if (eye.distanceToSqr(center) > range * range) return fail(Status.OUT_OF_RANGE, Math.round(range));
                if (hack.requiresLineOfSight() && !canSee(level, host, eye, center, pos)) return fail(Status.NO_LINE_OF_SIGHT);
                block = Optional.of(pos);
            }
        }

        // --- RAM (skills may have changed max/regen since the pool was last written)
        boolean chipChanged = stats.applyRam(chip, now);
        float cost = stats.ramCost(hack.baseRamCost(), timers, now);
        ChipRam ram = ChipStacks.ram(chip);
        if (!ram.canAfford(cost, now)) {
            if (chipChanged) host.setData(ModAttachments.INSTALLED_CHIP, chip);
            return fail(Status.NOT_ENOUGH_RAM, String.format(Locale.ROOT, "%.1f", cost),
                    String.format(Locale.ROOT, "%.1f", ram.current(now)));
        }

        // --- execute
        HackContext context = new HackContext(hackId, construct, host, chip, stats, entity, block, now);
        HackResult result;
        HACK_IN_PROGRESS.add(construct.getUUID());
        HACK_IN_PROGRESS.add(host.getUUID());
        try {
            result = hack.execute(context);
        } catch (RuntimeException ex) {
            ConstructChip.LOGGER.error("Quickhack {} threw", hackId, ex);
            result = HackResult.fail(Component.translatable("message.constructchip.hack.error"));
        } finally {
            HACK_IN_PROGRESS.remove(construct.getUUID());
            HACK_IN_PROGRESS.remove(host.getUUID());
        }

        if (!result.success()) {
            if (chipChanged) host.setData(ModAttachments.INSTALLED_CHIP, chip);
            return new Attempt(Status.HACK_FAILED, result.message()
                    .or(() -> Optional.of(Component.translatable(Status.HACK_FAILED.translationKey()))));
        }
        // Re-read RAM and timers: the hack may have changed them (Overclock starts its own timers).
        ChipStacks.setRam(chip, ChipStacks.ram(chip).spend(cost, now));
        if (hack.cooldownTicks() > 0) {
            ChipStacks.setTimers(chip, ChipStacks.timers(chip).withCooldown(hackId, hack.cooldownTicks(), now));
        }
        host.setData(ModAttachments.INSTALLED_CHIP, chip); // mark the attachment dirty so it saves
        // TODO(step 6): send the construct a RAM/cooldown sync payload here (RAM is synced on spend only).
        return new Attempt(Status.OK, result.message());
    }

    private static String seconds(long ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20f);
    }

    private static Vec3 closestPoint(AABB box, Vec3 from) {
        return new Vec3(Mth.clamp(from.x, box.minX, box.maxX), Mth.clamp(from.y, box.minY, box.maxY),
                Mth.clamp(from.z, box.minZ, box.maxZ));
    }

    /** Clear ray from the eyes to the block, using outline shapes so levers and buttons count. */
    private static boolean canSee(ServerLevel level, LivingEntity viewer, Vec3 eye, Vec3 center, BlockPos pos) {
        BlockHitResult hit = level.clip(new ClipContext(eye, center, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, viewer));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }
}
