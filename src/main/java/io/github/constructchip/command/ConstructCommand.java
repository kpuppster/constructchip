package io.github.constructchip.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.constructchip.ConstructChip;
import io.github.constructchip.chip.ChipIdentity;
import io.github.constructchip.chip.ChipRam;
import io.github.constructchip.chip.ChipSkills;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.chip.ChipTimers;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /construct} root command. Step 1 only has chip creation and debug helpers; install/eject, release,
 * optout etc. are added in later steps.
 */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class ConstructCommand {
    private static final SimpleCommandExceptionType NOT_HOLDING_CHIP =
            new SimpleCommandExceptionType(Component.translatable("commands.constructchip.not_holding_chip"));
    private static final SimpleCommandExceptionType CHIP_BLANK =
            new SimpleCommandExceptionType(Component.translatable("commands.constructchip.chip_blank"));

    private ConstructCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("construct")
                .then(Commands.literal("givechip")
                        .requires(src -> src.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> giveBlank(ctx, List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> giveBlank(ctx, EntityArgument.getPlayers(ctx, "targets")))
                                .then(Commands.argument("captured", EntityArgument.player())
                                        .executes(ctx -> giveCaptured(ctx,
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                EntityArgument.getPlayer(ctx, "captured"))))))
                .then(Commands.literal("chipinfo")
                        .requires(src -> src.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ConstructCommand::chipInfo))
                .then(Commands.literal("ram")
                        .requires(src -> src.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("spend")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0f))
                                        .executes(ConstructCommand::spendRam)))));
    }

    private static int giveBlank(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        return give(ctx, targets, p -> ChipStacks.createBlank(), "commands.constructchip.givechip.blank", null);
    }

    private static int giveCaptured(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets,
                                    ServerPlayer captured) {
        // TODO(step 2/4): refuse if `captured` is already on a registered chip, register the new chipId in
        // ChipRegistry and the captured player's attachment. Until then this is a pure debug item factory, and
        // every call mints a new chipId (so each copy is a distinct chip, never a dupe of another).
        long now = ctx.getSource().getLevel().getGameTime();
        return give(ctx, targets, p -> ChipStacks.createCaptured(captured, now),
                "commands.constructchip.givechip.captured", captured.getGameProfile().getName());
    }

    private static int give(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets,
                            Function<ServerPlayer, ItemStack> factory, String messageKey, String capturedName) {
        for (ServerPlayer target : targets) {
            ItemStack stack = factory.apply(target);
            if (!target.addItem(stack) && !stack.isEmpty()) {
                target.drop(stack, false);
            }
        }
        int count = targets.size();
        ctx.getSource().sendSuccess(() -> capturedName == null
                ? Component.translatable(messageKey, count)
                : Component.translatable(messageKey, capturedName, count), true);
        return count;
    }

    private static ItemStack heldChip(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = ctx.getSource().getPlayerOrException().getMainHandItem();
        if (!ChipStacks.isChip(stack)) throw NOT_HOLDING_CHIP.create();
        return stack;
    }

    private static int chipInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = heldChip(ctx);
        ChipIdentity id = ChipStacks.identity(stack).orElseThrow(CHIP_BLANK::create);
        long now = ctx.getSource().getLevel().getGameTime();
        ChipRam ram = ChipStacks.ram(stack);
        ChipSkills skills = ChipStacks.skills(stack);
        ChipTimers timers = ChipStacks.timers(stack);
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.literal("Construct: " + id.capturedName() + " (" + id.capturedUuid() + ")"), false);
        src.sendSuccess(() -> Component.literal("Chip id: " + id.chipId()), false);
        src.sendSuccess(() -> Component.literal(String.format("RAM: %.2f / %d (+%.2f/s, stamped %d, now %d)",
                ram.current(now), ram.max(), ram.regenPerSecond(), ram.stampTick(), now)), false);
        src.sendSuccess(() -> Component.literal("Tier " + skills.tier() + ", " + skills.points()
                + " points, unlocked " + skills.unlocked()), false);
        src.sendSuccess(() -> Component.literal("Overclocked: " + timers.isOverclocked(now)
                + ", locked out: " + timers.isLockedOut(now)
                + ", cooldowns: " + timers.cooldowns()), false);
        return 1;
    }

    private static int spendRam(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = heldChip(ctx);
        if (!ChipStacks.isCaptured(stack)) throw CHIP_BLANK.create();
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
}
