package io.github.constructchip.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.constructchip.ConstructChip;
import io.github.constructchip.attachment.ConstructState;
import io.github.constructchip.attachment.ModAttachments;
import io.github.constructchip.chip.ChipIdentity;
import io.github.constructchip.chip.ChipRam;
import io.github.constructchip.chip.ChipSkills;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.chip.ChipTimers;
import io.github.constructchip.skill.ModSkills;
import io.github.constructchip.skill.Skill;
import io.github.constructchip.skill.SkillPointCost;
import io.github.constructchip.skill.SkillTree;
import io.github.constructchip.tracking.ChipEntry;
import io.github.constructchip.tracking.ChipLocation;
import io.github.constructchip.tracking.ChipRegistry;
import io.github.constructchip.tracking.ChipTracker;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /construct}. Player commands: install, eject, optout, skill price/buy. Everything else is admin/debug (permission level 2).
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class ConstructCommand {
    private static final SimpleCommandExceptionType NOT_HOLDING_CHIP =
            new SimpleCommandExceptionType(Component.translatable("commands.constructchip.not_holding_chip"));
    private static final SimpleCommandExceptionType CHIP_BLANK =
            new SimpleCommandExceptionType(Component.translatable("commands.constructchip.chip_blank"));
    private static final SimpleCommandExceptionType ONE_TARGET =
            new SimpleCommandExceptionType(Component.translatable("commands.constructchip.givechip.one_target"));
    private static final DynamicCommandExceptionType ALREADY_CAPTURED = new DynamicCommandExceptionType(
            name -> Component.translatable("commands.constructchip.givechip.already_captured", name));
    private static final DynamicCommandExceptionType HAS_CHIP_INSTALLED = new DynamicCommandExceptionType(
            name -> Component.translatable("commands.constructchip.givechip.has_chip_installed", name));
    private static final SimpleCommandExceptionType NOTHING_INSTALLED =
            new SimpleCommandExceptionType(Component.translatable("commands.constructchip.eject.nothing"));
    private static final SimpleCommandExceptionType NO_CHIP_TO_BUY_FOR =
            new SimpleCommandExceptionType(Component.translatable("commands.constructchip.skill.no_chip"));
    private static final DynamicCommandExceptionType NOT_A_CONSTRUCT = new DynamicCommandExceptionType(
            name -> Component.translatable("commands.constructchip.release.not_a_construct", name));

    private ConstructCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static boolean isAdmin(CommandSourceStack src) {
        return src.hasPermission(Commands.LEVEL_GAMEMASTERS);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("construct")
                // ---- player commands
                .then(Commands.literal("install")
                        .executes(ctx -> install(ctx, ctx.getSource().getPlayerOrException(), false)))
                .then(Commands.literal("eject")
                        .executes(ConstructCommand::eject))
                .then(Commands.literal("optout")
                        .executes(ConstructCommand::toggleOptOut))
                // ---- admin
                .then(Commands.literal("installon").requires(ConstructCommand::isAdmin)
                        .then(Commands.argument("host", EntityArgument.player())
                                .executes(ctx -> install(ctx, EntityArgument.getPlayer(ctx, "host"), true))))
                .then(Commands.literal("release").requires(ConstructCommand::isAdmin)
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ConstructCommand::releasePlayer)))
                .then(Commands.literal("releaseid").requires(ConstructCommand::isAdmin)
                        .then(Commands.argument("chipId", UuidArgument.uuid())
                                .executes(ConstructCommand::releaseId)))
                .then(Commands.literal("chips").requires(ConstructCommand::isAdmin)
                        .executes(ConstructCommand::listChips))
                .then(Commands.literal("givechip").requires(ConstructCommand::isAdmin)
                        .executes(ctx -> giveBlank(ctx, java.util.List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> giveBlank(ctx, EntityArgument.getPlayers(ctx, "targets")))
                                .then(Commands.argument("captured", EntityArgument.player())
                                        .executes(ctx -> giveCaptured(ctx,
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                EntityArgument.getPlayer(ctx, "captured"))))))
                .then(Commands.literal("chipinfo").requires(ConstructCommand::isAdmin)
                        .executes(ConstructCommand::chipInfo))
                .then(Commands.literal("ram").requires(ConstructCommand::isAdmin)
                        .then(Commands.literal("spend")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0f))
                                        .executes(ConstructCommand::spendRam))))
                .then(Commands.literal("skill")
                        // player: buy points with XP for the chip in your main hand, or the one installed in you
                        .then(Commands.literal("price")
                                .executes(ctx -> showPrice(ctx, 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> showPrice(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(Commands.literal("buy")
                                .executes(ctx -> buyPoints(ctx, 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> buyPoints(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
                        // admin / debug
                        .then(Commands.literal("list").requires(ConstructCommand::isAdmin).executes(ConstructCommand::listSkills))
                        .then(Commands.literal("points").requires(ConstructCommand::isAdmin)
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ConstructCommand::setPoints)))
                        .then(Commands.literal("tier").requires(ConstructCommand::isAdmin)
                                .then(Commands.argument("tier", IntegerArgumentType.integer(1, ChipSkills.MAX_TIER))
                                        .executes(ConstructCommand::setTier)))
                        .then(Commands.literal("unlock").requires(ConstructCommand::isAdmin)
                                .then(Commands.argument("skill", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                ModSkills.get(ctx.getSource().registryAccess()).keySet(), builder))
                                        .executes(ConstructCommand::unlockSkill)))));
    }

    // ------------------------------------------------------------ install / eject / optout

    private static int install(CommandContext<CommandSourceStack> ctx, ServerPlayer host, boolean admin)
            throws CommandSyntaxException {
        ServerPlayer installer = ctx.getSource().getPlayerOrException();
        if (!ChipStacks.isChip(installer.getMainHandItem())) throw NOT_HOLDING_CHIP.create();
        ChipTracker.InstallResult result = ChipTracker.install(installer, host, InteractionHand.MAIN_HAND, admin);
        Component msg = Component.translatable(result.translationKey());
        if (result == ChipTracker.InstallResult.OK) {
            ctx.getSource().sendSuccess(() -> msg, admin);
            return 1;
        }
        ctx.getSource().sendFailure(msg);
        return 0;
    }

    private static int eject(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer host = ctx.getSource().getPlayerOrException();
        if (!ChipTracker.eject(host)) throw NOTHING_INSTALLED.create();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.constructchip.eject.done"), false);
        return 1;
    }

    private static int toggleOptOut(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean now = !player.getData(ModAttachments.OPTED_OUT);
        player.setData(ModAttachments.OPTED_OUT, now);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                now ? "commands.constructchip.optout.on" : "commands.constructchip.optout.off"), false);
        return now ? 1 : 0;
    }

    // ------------------------------------------------------------ release / registry

    private static int releasePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
        int released = 0;
        ChipRegistry registry = ChipRegistry.get(ctx.getSource().getServer());
        for (GameProfile profile : profiles) {
            Optional<UUID> chip = registry.chipOf(profile.getId());
            if (chip.isEmpty()) {
                if (profiles.size() == 1) throw NOT_A_CONSTRUCT.create(profile.getName());
                continue;
            }
            if (ChipTracker.release(ctx.getSource().getServer(), chip.get(), "released by " + ctx.getSource().getTextName())) {
                released++;
                ctx.getSource().sendSuccess(() -> Component.translatable("commands.constructchip.release.done",
                        profile.getName()), true);
            }
        }
        return released;
    }

    private static int releaseId(CommandContext<CommandSourceStack> ctx) {
        UUID chipId = UuidArgument.getUuid(ctx, "chipId");
        if (!ChipTracker.release(ctx.getSource().getServer(), chipId, "released by " + ctx.getSource().getTextName())) {
            ctx.getSource().sendFailure(Component.translatable("commands.constructchip.release.unknown_chip", chipId.toString()));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.constructchip.release.done", chipId.toString()), true);
        return 1;
    }

    private static int listChips(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        Map<UUID, ChipEntry> all = ChipRegistry.get(src.getServer()).all();
        long now = ChipRegistry.now(src.getServer());
        src.sendSuccess(() -> Component.literal(all.size() + " registered chip(s)"), false);
        all.forEach((chipId, e) -> {
            ChipLocation loc = e.location();
            String holder = loc.holder().map(uuid -> {
                ServerPlayer p = src.getServer().getPlayerList().getPlayer(uuid);
                return p != null ? p.getGameProfile().getName() : uuid.toString();
            }).orElse("-");
            String pos = loc.pos().map(GlobalPos::toString).orElse("-");
            src.sendSuccess(() -> Component.literal(String.format("%s  %s  %s by %s at %s, seen %d ticks ago, serial %d",
                    chipId, e.capturedName(), loc.state().getSerializedName(), holder, pos,
                    now - loc.lastSeenTick(), e.serial())), false);
        });
        return all.size();
    }

    // ------------------------------------------------------------ givechip

    private static int giveBlank(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) giveTo(target, ChipStacks.createBlank());
        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.constructchip.givechip.blank", count), true);
        return count;
    }

    /**
     * Debug capture: puts {@code captured} on a new chip and hands it to the single target. Registers the chip, so the
     * construct is immediately anchored to it. The real capture flow (Soul Killer, step 11) will snapshot the player first.
     */
    private static int giveCaptured(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets,
                                    ServerPlayer captured) throws CommandSyntaxException {
        if (targets.size() != 1) throw ONE_TARGET.create();
        ServerPlayer target = targets.iterator().next();
        String name = captured.getGameProfile().getName();
        ChipRegistry registry = ChipRegistry.get(ctx.getSource().getServer());
        if (registry.chipOf(captured.getUUID()).isPresent() || ChipTracker.isConstruct(captured)) {
            throw ALREADY_CAPTURED.create(name);
        }
        if (!ChipTracker.installedChip(captured).isEmpty()) throw HAS_CHIP_INSTALLED.create(name);

        long now = ChipRegistry.now(ctx.getSource().getServer());
        ItemStack chip = ChipStacks.createCaptured(captured, ctx.getSource().getLevel().getGameTime());
        ChipIdentity id = ChipStacks.identity(chip).orElseThrow();
        registry.register(id.chipId(), new ChipEntry(captured.getUUID(), name, ChipTracker.serialOf(chip),
                ChipLocation.of(ChipLocation.State.CARRIED, target.getUUID(),
                        GlobalPos.of(target.level().dimension(), target.blockPosition()), now)));
        captured.setData(ModAttachments.CONSTRUCT_STATE, ConstructState.of(id.chipId()));
        giveTo(target, chip);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.constructchip.givechip.captured",
                name, target.getGameProfile().getName()), true);
        return 1;
    }

    private static void giveTo(ServerPlayer target, ItemStack stack) {
        if (!target.addItem(stack) && !stack.isEmpty()) target.drop(stack, false);
    }

    // ------------------------------------------------------------ held-chip debug

    private static ItemStack heldChip(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = ctx.getSource().getPlayerOrException().getMainHandItem();
        if (!ChipStacks.isChip(stack)) throw NOT_HOLDING_CHIP.create();
        if (!ChipStacks.isCaptured(stack)) throw CHIP_BLANK.create();
        return stack;
    }

    private static int chipInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = heldChip(ctx);
        ChipIdentity id = ChipStacks.identity(stack).orElseThrow();
        long now = ctx.getSource().getLevel().getGameTime();
        ChipRam ram = ChipStacks.ram(stack);
        ChipSkills skills = ChipStacks.skills(stack);
        ChipTimers timers = ChipStacks.timers(stack);
        boolean valid = ChipTracker.currentEntry(ctx.getSource().getServer(), stack).isPresent();
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.literal("Construct: " + id.capturedName() + " (" + id.capturedUuid() + ")"), false);
        src.sendSuccess(() -> Component.literal("Chip id: " + id.chipId() + ", serial " + ChipTracker.serialOf(stack)
                + (valid ? " (valid)" : " (NOT the registered copy)")), false);
        src.sendSuccess(() -> Component.literal(String.format("RAM: %.2f / %d (+%.2f/s, stamped %d, now %d)",
                ram.current(now), ram.max(), ram.regenPerSecond(), ram.stampTick(), now)), false);
        src.sendSuccess(() -> Component.literal("Mk." + skills.tier() + ", " + skills.points()
                + " points, unlocked " + skills.unlocked()), false);
        src.sendSuccess(() -> Component.literal("Overclocked: " + timers.isOverclocked(now)
                + ", locked out: " + timers.isLockedOut(now) + ", cooldowns: " + timers.cooldowns()), false);
        return 1;
    }

    private static int spendRam(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = heldChip(ctx);
        float amount = FloatArgumentType.getFloat(ctx, "amount");
        long now = ctx.getSource().getLevel().getGameTime();
        if (!ChipStacks.trySpendRam(stack, amount, now)) {
            ctx.getSource().sendFailure(Component.translatable("commands.constructchip.ram.insufficient",
                    String.format("%.2f", ChipStacks.ram(stack).current(now))));
            return 0;
        }
        float left = ChipStacks.ram(stack).current(now);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.constructchip.ram.spent",
                String.format("%.2f", amount), String.format("%.2f", left)), false);
        return 1;
    }

    // ------------------------------------------------------------ skills

    /** The valid chip the player is buying for: main hand first, then the one installed in them. */
    private record ChipRef(ItemStack stack, boolean installed) {}

    private static ChipRef buyerChip(ServerPlayer player) throws CommandSyntaxException {
        ItemStack hand = player.getMainHandItem();
        if (ChipStacks.isCaptured(hand)) {
            if (ChipTracker.currentEntry(player.server, hand).isEmpty()) throw CHIP_BLANK.create();
            return new ChipRef(hand, false);
        }
        ItemStack installed = ChipTracker.installedChip(player);
        if (!installed.isEmpty() && ChipTracker.currentEntry(player.server, installed).isPresent()) {
            return new ChipRef(installed, true);
        }
        throw NO_CHIP_TO_BUY_FOR.create();
    }

    private static int showPrice(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ChipSkills skills = ChipStacks.skills(buyerChip(player).stack());
        long cost = SkillPointCost.costOf(skills.purchased(), count);
        long have = SkillPointCost.totalXp(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.constructchip.skill.price",
                count, cost, have, skills.purchased()), false);
        return (int) Math.min(Integer.MAX_VALUE, cost);
    }

    private static int buyPoints(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ChipRef chip = buyerChip(player);
        ChipSkills skills = ChipStacks.skills(chip.stack());
        long cost = SkillPointCost.costOf(skills.purchased(), count);
        if (!SkillPointCost.canAfford(player, cost)) {
            ctx.getSource().sendFailure(Component.translatable("commands.constructchip.skill.cannot_afford",
                    count, cost, SkillPointCost.totalXp(player)));
            return 0;
        }
        SkillPointCost.charge(player, cost);
        ChipSkills next = skills.withPurchased(count);
        ChipStacks.setSkills(chip.stack(), next);
        if (chip.installed()) player.setData(ModAttachments.INSTALLED_CHIP, chip.stack()); // mark the attachment dirty
        long nextPrice = SkillPointCost.costOf(next.purchased());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.constructchip.skill.bought",
                count, cost, next.points(), nextPrice), false);
        return count;
    }

    private static int listSkills(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = heldChip(ctx);
        ChipSkills skills = ChipStacks.skills(stack);
        Registry<Skill> registry = ModSkills.get(ctx.getSource().registryAccess());
        ctx.getSource().sendSuccess(() -> Component.literal("Mk." + skills.tier() + ", " + skills.points() + " points"), false);
        registry.entrySet().stream()
                .sorted(java.util.Comparator.comparingInt((Map.Entry<net.minecraft.resources.ResourceKey<Skill>, Skill> e) -> e.getValue().tier())
                        .thenComparingInt(e -> e.getValue().column()))
                .forEach(e -> {
                    ResourceLocation id = e.getKey().location();
                    Skill skill = e.getValue();
                    SkillTree.UnlockResult status = SkillTree.check(skills, id, registry);
                    String mark = status == SkillTree.UnlockResult.ALREADY_UNLOCKED ? "[x]"
                            : status == SkillTree.UnlockResult.OK ? "[+]" : "[ ]";
                    ctx.getSource().sendSuccess(() -> Component.literal(mark + " T" + skill.tier() + " "
                            + skill.branch().getSerializedName() + " ").append(skill.name())
                            .append(Component.literal(" (" + id + ", cost " + skill.cost() + ")")), false);
                });
        return registry.size();
    }

    private static int setPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = heldChip(ctx);
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        ChipStacks.setSkills(stack, ChipStacks.skills(stack).withPoints(amount));
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.constructchip.skill.points", amount), false);
        return amount;
    }

    private static int setTier(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = heldChip(ctx);
        int tier = IntegerArgumentType.getInteger(ctx, "tier");
        ChipStacks.setSkills(stack, ChipStacks.skills(stack).withTier(tier));
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.constructchip.skill.tier", tier), false);
        return tier;
    }

    private static int unlockSkill(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = heldChip(ctx);
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "skill");
        Registry<Skill> registry = ModSkills.get(ctx.getSource().registryAccess());
        ChipSkills skills = ChipStacks.skills(stack);
        SkillTree.UnlockResult result = SkillTree.check(skills, id, registry);
        if (result != SkillTree.UnlockResult.OK) {
            ctx.getSource().sendFailure(Component.translatable(result.translationKey(), id.toString()));
            return 0;
        }
        ChipStacks.setSkills(stack, SkillTree.unlock(skills, id, registry));
        ctx.getSource().sendSuccess(() -> Component.translatable(result.translationKey(), id.toString()), false);
        return 1;
    }
}
