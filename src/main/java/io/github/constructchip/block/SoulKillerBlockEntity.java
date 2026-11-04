package io.github.constructchip.block;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.ModGameRules;
import io.github.constructchip.attachment.ModAttachments;
import io.github.constructchip.capture.CaptureService;
import io.github.constructchip.capture.ConsentManager;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.registry.ModBlockEntities;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Capture state machine: IDLE → SEATED (a player sits in the chair) → AWAITING_CONSENT (an operator used a blank
 * chip on the chair; the seated player gets a consent screen) → CAPTURING (5 s; seat, power and operator re-checked
 * every tick) → DONE (player captured, chip handed to the operator) → IDLE.
 *
 * <p>Needs redstone power (lever, button held, redstone block, etc. next to the chair). The blank
 * chip is held inside while waiting and comes back on any abort. A machine loaded mid-capture (crash or
 * chunk unload) aborts and pops the chip out. The player is never captured without having accepted.
 */
public class SoulKillerBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ripperdoc.idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }


    public static final int CAPTURE_TICKS = 100;
    public static final double OPERATOR_RANGE = 16.0;

    public enum State { IDLE, SEATED, AWAITING_CONSENT, CAPTURING, DONE }

    private State state = State.IDLE;
    private UUID seated, operator;
    private int progress;
    private boolean holdingChip;

    public SoulKillerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SOUL_KILLER.get(), pos, blockState);
    }

    public State state() { return state; }

    // ------------------------------------------------------------ checks

    public boolean isPowered() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    /** The player sitting in this chair, if any. */
    public Optional<ServerPlayer> findSeated() {
        if (level == null) return Optional.empty();
        List<ChairSeatEntity> seats = level.getEntitiesOfClass(ChairSeatEntity.class, new AABB(worldPosition).inflate(3),
                s -> s.chair().equals(worldPosition));
        for (ChairSeatEntity seat : seats) {
            if (seat.getFirstPassenger() instanceof ServerPlayer p) return Optional.of(p);
        }
        return Optional.empty();
    }

    private boolean stillSeated(ServerPlayer target) {
        return findSeated().map(p -> p == target).orElse(false);
    }

    @Nullable
    private Component problemFor(ServerPlayer target, @Nullable ServerPlayer op) {
        MinecraftServer server = target.server;
        if (!isPowered()) return Component.translatable("message.constructchip.soul_killer.no_power");
        if (!stillSeated(target)) return Component.translatable("message.constructchip.soul_killer.not_seated");
        if (op == null || op.level() != level || op.distanceToSqr(worldPosition.getCenter()) > OPERATOR_RANGE * OPERATOR_RANGE) {
            return Component.translatable("message.constructchip.soul_killer.operator_left");
        }
        if (!server.getGameRules().getBoolean(ModGameRules.ALLOW_CONSTRUCT_CAPTURE)) {
            return Component.translatable("message.constructchip.install.capture_disabled");
        }
        if (target.getData(ModAttachments.OPTED_OUT)) return Component.translatable("message.constructchip.soul_killer.opted_out", target.getGameProfile().getName());
        return CaptureService.whyNotCapturable(target);
    }

    // ------------------------------------------------------------ interaction

    public void onUse(ServerPlayer op, InteractionHand hand) {
        ItemStack held = op.getItemInHand(hand);
        if (state == State.AWAITING_CONSENT || state == State.CAPTURING) {
            op.displayClientMessage(Component.translatable("message.constructchip.soul_killer.busy"), true);
            return;
        }
        Optional<ServerPlayer> target = findSeated();
        if (!ChipStacks.isChip(held) || ChipStacks.isCaptured(held)) {
            // status readout
            op.displayClientMessage(Component.translatable("message.constructchip.soul_killer.status",
                    Component.translatable(isPowered() ? "message.constructchip.soul_killer.powered" : "message.constructchip.soul_killer.unpowered"),
                    target.map(p -> p.getGameProfile().getName()).orElse("-")), true);
            return;
        }
        if (target.isEmpty()) {
            op.displayClientMessage(Component.translatable("message.constructchip.soul_killer.not_seated"), true);
            return;
        }
        ServerPlayer t = target.get();
        if (t == op) {
            op.displayClientMessage(Component.translatable("message.constructchip.soul_killer.self"), true);
            return;
        }
        Component problem = problemFor(t, op);
        if (problem != null) {
            op.displayClientMessage(problem, true);
            return;
        }
        if (ConsentManager.hasPending(t.getUUID())) {
            op.displayClientMessage(Component.translatable("message.constructchip.soul_killer.busy"), true);
            return;
        }

        held.shrink(1);
        holdingChip = true;
        seated = t.getUUID();
        operator = op.getUUID();
        setState(State.AWAITING_CONSENT);
        op.displayClientMessage(Component.translatable("message.constructchip.soul_killer.asking", t.getGameProfile().getName()), true);
        ConsentManager.request(t, Component.translatable("gui.constructchip.consent.capture.title"),
                Component.translatable("gui.constructchip.consent.capture.body", op.getGameProfile().getName()),
                ConsentManager.DEFAULT_TIMEOUT_TICKS, new ConsentManager.Handler() {
                    @Override
                    public Component revalidate(MinecraftServer server) {
                        if (isRemoved() || state != State.AWAITING_CONSENT) return Component.translatable("message.constructchip.soul_killer.broken");
                        ServerPlayer tt = server.getPlayerList().getPlayer(seated);
                        if (tt == null) return Component.translatable("message.constructchip.consent.left");
                        return problemFor(tt, server.getPlayerList().getPlayer(operator));
                    }

                    @Override
                    public void accepted(MinecraftServer server) {
                        progress = 0;
                        setState(State.CAPTURING);
                        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1f, 0.6f);
                        tell(server, Component.translatable("message.constructchip.soul_killer.capturing"));
                    }

                    @Override
                    public void declined(MinecraftServer server, Component reason) {
                        if (state == State.AWAITING_CONSENT) abort(reason);
                    }
                });
    }

    // ------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, SoulKillerBlockEntity be) {
        be.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        if (healOnFirstTick) {
            healOnFirstTick = false;
            if (state != State.AWAITING_CONSENT || !ConsentManager.hasPending(seated == null ? new UUID(0, 0) : seated)) {
                abort(Component.translatable("message.constructchip.soul_killer.interrupted"));
                return;
            }
        }
        switch (state) {
            case IDLE, SEATED -> {
                if (level.getGameTime() % 10 != 0) return;
                boolean someone = findSeated().isPresent();
                if (someone && state == State.IDLE) setState(State.SEATED);
                else if (!someone && state == State.SEATED) setState(State.IDLE);
            }
            case AWAITING_CONSENT -> { /* ConsentManager drives this */ }
            case CAPTURING -> tickCapture(level);
            case DONE -> {
                if (++progress > 40) {
                    progress = 0;
                    setState(State.IDLE);
                }
            }
        }
    }

    private void tickCapture(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ServerPlayer target = server.getPlayerList().getPlayer(seated);
        if (target == null) {
            abort(Component.translatable("message.constructchip.consent.left"));
            return;
        }
        Component problem = problemFor(target, server.getPlayerList().getPlayer(operator));
        if (problem != null) {
            abort(problem);
            return;
        }
        progress++;
        // Capri's red lightning pours into the sitter's head, faster as the capture goes on
        int every = progress < CAPTURE_TICKS / 2 ? 8 : 5;
        if (progress % every == 0) {
            io.github.constructchip.fx.BoltFx.send(level, io.github.constructchip.fx.BoltFx.Preset.CAPTURE, target,
                    target.getEyePosition(), java.util.List.of(target.position().add(0, 0.25, 0)));
            io.github.constructchip.fx.BoltFx.crackle(level, target.getEyePosition(), 0.6f + 0.6f * progress / CAPTURE_TICKS);
        }
        if (progress % 20 == 0) {
            level.playSound(null, worldPosition, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1f, 0.5f + progress / 200f);
            target.displayClientMessage(Component.translatable("message.constructchip.soul_killer.progress",
                    String.format(Locale.ROOT, "%d", progress * 100 / CAPTURE_TICKS)), true);
        }
        if (progress >= CAPTURE_TICKS) complete(level, target, server.getPlayerList().getPlayer(operator));
    }

    private void complete(ServerLevel level, ServerPlayer target, ServerPlayer op) {
        io.github.constructchip.fx.BoltFx.send(level, io.github.constructchip.fx.BoltFx.Preset.CAPTURE_END, target,
                target.getEyePosition(), java.util.List.of(target.position().add(0, 0.25, 0)));
        io.github.constructchip.fx.BoltFx.sound(level, target.getEyePosition(), io.github.constructchip.registry.ModSounds.ZAP.get(), 1.2f, 0.9f);
        target.stopRiding();
        Optional<ItemStack> chip = CaptureService.captureOntoNewChip(target, op);
        if (chip.isEmpty()) {
            abort(Component.translatable("message.constructchip.soul_killer.failed"));
            return;
        }
        holdingChip = false;
        progress = 0;
        setState(State.DONE);
        level.playSound(null, worldPosition, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 0.7f, 1.4f);
        op.displayClientMessage(Component.translatable("message.constructchip.soul_killer.done", target.getGameProfile().getName()), true);
        ConstructChip.LOGGER.info("Soul Killer at {} captured {} for {}", worldPosition, target.getGameProfile().getName(), op.getGameProfile().getName());
    }

    /** Stop whatever is going on and give the blank chip back (to the operator if near, else on top of the machine). */
    public void abort(Component reason) {
        if (level == null || level.isClientSide) return;
        MinecraftServer server = level.getServer();
        if (holdingChip) {
            holdingChip = false;
            ItemStack blank = ChipStacks.createBlank();
            ServerPlayer op = operator == null ? null : server.getPlayerList().getPlayer(operator);
            if (op == null || !op.addItem(blank)) {
                var c = worldPosition.getCenter();
                level.addFreshEntity(new ItemEntity(level, c.x, c.y + 0.7, c.z, blank));
            }
        }
        if (state == State.AWAITING_CONSENT || state == State.CAPTURING) tell(server, reason);
        progress = 0;
        setState(State.IDLE);
    }

    private void tell(MinecraftServer server, Component msg) {
        for (UUID id : new UUID[]{operator, seated}) {
            if (id == null) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) p.displayClientMessage(msg, true);
        }
    }

    private void setState(State next) {
        state = next;
        setChanged();
        if (level != null) {
            BlockState bs = getBlockState();
            BlockState lit = SoulKillerBlock.withActive(bs, next == State.CAPTURING || next == State.AWAITING_CONSENT);
            if (lit != bs) level.setBlock(worldPosition, lit, 3);
        }
    }

    // ------------------------------------------------------------ save / self-heal

    /** Set on load; handled on the first tick (changing blocks while the chunk is still loading is unsafe). */
    private boolean healOnFirstTick;

    @Override
    public void onLoad() {
        super.onLoad();
        // A capture can't survive a reload: the consent and the tick loop are gone. Give the chip back.
        healOnFirstTick = state == State.AWAITING_CONSENT || state == State.CAPTURING || holdingChip;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("state", state.name());
        if (seated != null) tag.putUUID("seated", seated);
        if (operator != null) tag.putUUID("operator", operator);
        tag.putInt("progress", progress);
        tag.putBoolean("holding_chip", holdingChip);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            state = State.valueOf(tag.getString("state"));
        } catch (IllegalArgumentException ex) {
            state = State.IDLE;
        }
        seated = tag.hasUUID("seated") ? tag.getUUID("seated") : null;
        operator = tag.hasUUID("operator") ? tag.getUUID("operator") : null;
        progress = tag.getInt("progress");
        holdingChip = tag.getBoolean("holding_chip");
    }

}
