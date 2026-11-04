package io.github.constructchip.gametest;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.attachment.ModAttachments;
import io.github.constructchip.capture.CaptureService;
import io.github.constructchip.chip.ChipSkills;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.chip.ChipTimers;
import io.github.constructchip.hack.HackTarget;
import io.github.constructchip.hack.QuickhackService;
import io.github.constructchip.hack.QuickhackService.Status;
import io.github.constructchip.skill.ModSkills;
import io.github.constructchip.skill.Skill;
import io.github.constructchip.skill.SkillPointCost;
import io.github.constructchip.skill.SkillTree;
import io.github.constructchip.tracking.ChipLocation;
import io.github.constructchip.tracking.ChipRegistry;
import io.github.constructchip.tracking.ChipTracker;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import com.mojang.authlib.GameProfile;
import io.github.constructchip.ghost.GhostState;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Server-side logic tests with fake players. Run with {@code ./gradlew runGameTestServer -PnoVista} (CI does this;
 * Moonlight sends its own packets to joining players, which fake players can't receive).
 * Each test captures, checks, and releases, so tests don't leak constructs into each other.
 */
@GameTestHolder(ConstructChip.MODID)
@PrefixGameTestTemplate(false)
public final class ConstructGameTests {
    private static final ResourceLocation SCANNER = ConstructChip.id("scanner");
    private static final ResourceLocation OVERCLOCK = ConstructChip.id("overclock");

    private ConstructGameTests() {}

    private static final AtomicInteger COUNTER = new AtomicInteger();

    /**
     * A NeoForge FakePlayer put straight into the player list (so getPlayer(uuid) finds it) without a network
     * login. A real login would make Create and Moonlight send packets the fake connection can't take.
     */
    private static ServerPlayer player(GameTestHelper h, double x, double z) {
        ServerLevel level = h.getLevel();
        FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "gt" + COUNTER.incrementAndGet()));
        Vec3 v = h.absoluteVec(new Vec3(x + 0.5, 1, z + 0.5));
        p.moveTo(v.x, v.y, v.z, 0f, 0f);
        PlayerList list = level.getServer().getPlayerList();
        list.players.add(p);
        list.playersByUUID.put(p.getUUID(), p);
        return p;
    }

    private static void done(GameTestHelper h, ServerPlayer... players) {
        PlayerList list = h.getLevel().getServer().getPlayerList();
        for (ServerPlayer p : players) {
            list.players.remove(p);
            list.playersByUUID.remove(p.getUUID());
        }
        h.succeed();
    }

    /** Captures {@code victim} onto a chip, puts it in {@code host}'s hand (and nowhere else), returns the chip id. */
    private static UUID captureToHand(GameTestHelper h, ServerPlayer victim, ServerPlayer host) {
        ItemStack chip = CaptureService.captureOntoNewChip(victim, host).orElseThrow(() -> new AssertionError("capture failed"));
        host.getInventory().clearContent();
        host.setItemInHand(InteractionHand.MAIN_HAND, chip.copy());
        return ChipStacks.identity(chip).orElseThrow().chipId();
    }

    private static void installOn(GameTestHelper h, ServerPlayer host) {
        ChipTracker.InstallResult r = ChipTracker.install(host, host, InteractionHand.MAIN_HAND, false);
        h.assertTrue(r == ChipTracker.InstallResult.OK, "install: " + r);
    }

    private static void expect(GameTestHelper h, QuickhackService.Attempt a, Status want, String what) {
        h.assertTrue(a.status() == want, what + ": expected " + want + " got " + a.status());
    }

    @GameTest(template = "empty")
    public static void captureThenReleaseRestoresEverything(GameTestHelper h) {
        ServerPlayer victim = player(h, 1, 1), host = player(h, 3, 1);
        victim.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
        victim.getInventory().setItem(39, new ItemStack(Items.IRON_HELMET));
        victim.getInventory().setItem(40, new ItemStack(Items.SHIELD));
        victim.setExperienceLevels(12);
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600));

        UUID chipId = captureToHand(h, victim, host);
        h.assertTrue(victim.getInventory().isEmpty(), "inventory not stripped");
        h.assertTrue(victim.experienceLevel == 0, "xp not stripped");
        h.assertTrue(victim.getActiveEffects().isEmpty(), "effects not stripped");
        h.assertTrue(ChipTracker.isConstruct(victim), "not a construct");
        h.assertTrue(CaptureService.snapshotOf(victim).isPresent(), "no snapshot");

        h.assertTrue(ChipTracker.release(h.getLevel().getServer(), chipId, "test"), "release failed");
        h.assertTrue(victim.getInventory().getItem(0).is(Items.DIAMOND) && victim.getInventory().getItem(0).getCount() == 5, "diamonds");
        h.assertTrue(victim.getInventory().getItem(39).is(Items.IRON_HELMET), "helmet slot");
        h.assertTrue(victim.getInventory().getItem(40).is(Items.SHIELD), "offhand slot");
        h.assertTrue(victim.experienceLevel == 12, "xp level " + victim.experienceLevel);
        h.assertTrue(victim.hasEffect(MobEffects.MOVEMENT_SPEED), "effect");
        h.assertTrue(!ChipTracker.isConstruct(victim), "still a construct");
        h.assertTrue(CaptureService.snapshotOf(victim).isEmpty(), "snapshot left behind");
        h.assertTrue(!CaptureService.restore(victim), "restore ran twice");
        done(h, victim, host);
    }

    @GameTest(template = "empty")
    public static void installAndScannerChecks(GameTestHelper h) {
        ServerPlayer victim = player(h, 1, 1), host = player(h, 2, 1);
        UUID chipId = captureToHand(h, victim, host);
        installOn(h, host);
        h.assertTrue(ChipRegistry.get(h.getLevel().getServer()).get(chipId).orElseThrow().location().state()
                == ChipLocation.State.INSTALLED, "not INSTALLED");

        BlockPos stone = new BlockPos(2, 1, 3);
        h.setBlock(stone, Blocks.STONE);
        BlockPos far = h.absolutePos(new BlockPos(2, 1, 40));
        h.getLevel().setBlockAndUpdate(far, Blocks.STONE.defaultBlockState());

        expect(h, QuickhackService.use(victim, SCANNER, HackTarget.block(h.absolutePos(new BlockPos(1, 2, 3)))), Status.TARGET_NOT_FOUND, "air");
        expect(h, QuickhackService.use(victim, SCANNER, HackTarget.block(far)), Status.OUT_OF_RANGE, "far block");
        expect(h, QuickhackService.use(victim, SCANNER, HackTarget.NONE), Status.NO_TARGET, "no target");
        expect(h, QuickhackService.use(victim, SCANNER, HackTarget.block(h.absolutePos(stone))), Status.OK, "stone");
        expect(h, QuickhackService.use(victim, SCANNER, HackTarget.block(h.absolutePos(stone))), Status.ON_COOLDOWN, "repeat");
        expect(h, QuickhackService.use(victim, OVERCLOCK, HackTarget.NONE), Status.NOT_UNLOCKED, "overclock locked");
        expect(h, QuickhackService.use(host, SCANNER, HackTarget.block(h.absolutePos(stone))), Status.NOT_A_CONSTRUCT, "host");

        h.getLevel().setBlockAndUpdate(far, Blocks.AIR.defaultBlockState());
        ChipTracker.release(h.getLevel().getServer(), chipId, "test");
        done(h, victim, host);
    }

    @GameTest(template = "empty")
    public static void dupeGuardWipesCopies(GameTestHelper h) {
        ServerPlayer victim = player(h, 1, 1), host = player(h, 3, 3);
        UUID chipId = captureToHand(h, victim, host);
        ItemStack a = host.getMainHandItem().copy();
        ItemStack b = a.copy();
        // two stacks with the same chip heartbeating in one tick: the second is a duplicate
        ChipTracker.heartbeatCarried(a, host);
        ChipTracker.heartbeatCarried(b, host);
        h.assertTrue(ChipStacks.isCaptured(a), "original wiped");
        h.assertTrue(!ChipStacks.isCaptured(b), "duplicate survived");

        // a copy left behind when the real one is installed is stale
        ItemStack leftover = host.getMainHandItem().copy();
        installOn(h, host);
        ChipTracker.heartbeatCarried(leftover, host);
        h.assertTrue(!ChipStacks.isCaptured(leftover), "stale copy survived install");

        ChipTracker.release(h.getLevel().getServer(), chipId, "test");
        done(h, victim, host);
    }

    @GameTest(template = "empty")
    public static void skillTreeRulesAndPrices(GameTestHelper h) {
        Registry<Skill> reg = ModSkills.get(h.getLevel().registryAccess());
        boolean vista = net.neoforged.fml.ModList.get().isLoaded("vista");
        h.assertTrue(reg.size() == (vista ? 28 : 25), "skills loaded: " + reg.size() + " (vista " + vista + ")");
        h.assertTrue(vista == reg.containsKey(ConstructChip.id("camera_view")), "Vista skill condition not applied");
        ResourceLocation overclock = ConstructChip.id("overclock"), shortCircuit = ConstructChip.id("short_circuit");
        ChipSkills s = ChipSkills.DEFAULT;
        h.assertTrue(s.has(ConstructChip.id("scanner")), "scanner not free");
        h.assertTrue(SkillTree.check(s, overclock, reg) == SkillTree.UnlockResult.TIER_TOO_LOW, "mk1 overclock");
        s = s.withTier(2);
        h.assertTrue(SkillTree.check(s, overclock, reg) == SkillTree.UnlockResult.NOT_ENOUGH_POINTS, "no points");
        h.assertTrue(SkillTree.check(s.withPoints(9), shortCircuit, reg) == SkillTree.UnlockResult.MISSING_PREREQUISITE, "prereq");
        s = SkillTree.unlock(s.withPoints(2), overclock, reg);
        h.assertTrue(s.has(overclock) && s.points() == 0, "unlock spent points");
        h.assertTrue(SkillPointCost.costOf(0) == 160 && SkillPointCost.costOf(1) == 192, "prices " + SkillPointCost.costOf(0) + "," + SkillPointCost.costOf(1));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void overclockThenLockout(GameTestHelper h) {
        ServerPlayer victim = player(h, 1, 1), host = player(h, 3, 1);
        UUID chipId = captureToHand(h, victim, host);
        installOn(h, host);
        ItemStack chip = ChipTracker.installedChip(host);
        ChipStacks.setSkills(chip, new ChipSkills(2, 0, Set.of(SCANNER, OVERCLOCK), 0));
        host.setData(ModAttachments.INSTALLED_CHIP, chip);

        expect(h, QuickhackService.use(victim, OVERCLOCK, HackTarget.NONE), Status.OK, "overclock");
        long now = h.getLevel().getGameTime();
        h.assertTrue(ChipStacks.timers(ChipTracker.installedChip(host)).isOverclocked(now), "timers not set");
        h.assertTrue(host.hasEffect(MobEffects.REGENERATION), "host not buffed");
        h.assertTrue(Math.abs(ChipStacks.ram(ChipTracker.installedChip(host)).current(now) - 4f) < 0.01f, "RAM after overclock");
        expect(h, QuickhackService.use(victim, OVERCLOCK, HackTarget.NONE), Status.HACK_FAILED, "double overclock");

        ItemStack c = ChipTracker.installedChip(host);
        ChipStacks.setTimers(c, new ChipTimers(now - 1, now + 200, Map.of()));
        host.setData(ModAttachments.INSTALLED_CHIP, c);
        expect(h, QuickhackService.use(victim, SCANNER, HackTarget.NONE), Status.LOCKED_OUT, "lockout");

        ChipTracker.release(h.getLevel().getServer(), chipId, "test");
        done(h, victim, host);
    }

    @GameTest(template = "empty")
    public static void constructFlagFollowsCaptureAndRelease(GameTestHelper h) {
        ServerPlayer victim = player(h, 1, 1), host = player(h, 3, 1);
        UUID chipId = captureToHand(h, victim, host);
        h.assertTrue(GhostState.isConstruct(victim) && !GhostState.isConstruct(host), "ghost flag");
        h.assertTrue(QuickhackService.hostOf(victim).isEmpty(), "host before install");
        installOn(h, host);
        h.assertTrue(QuickhackService.hostOf(victim).map(p -> p == host).orElse(false), "host after install");
        h.assertTrue(ChipTracker.eject(host), "eject");
        h.assertTrue(QuickhackService.hostOf(victim).isEmpty(), "host after eject");
        ChipTracker.release(h.getLevel().getServer(), chipId, "test");
        h.assertTrue(!GhostState.isConstruct(victim), "still ghost after release");
        done(h, victim, host);
    }

    @GameTest(template = "empty")
    public static void soulKillerChairNeedsRedstone(GameTestHelper h) {
        BlockPos chairPos = new BlockPos(2, 1, 2);
        h.setBlock(chairPos, io.github.constructchip.registry.ModBlocks.SOUL_KILLER.get());
        var machine = (io.github.constructchip.block.SoulKillerBlockEntity) h.getBlockEntity(chairPos);
        h.assertTrue(!machine.isPowered(), "powered without redstone");
        h.setBlock(chairPos.east(), Blocks.REDSTONE_BLOCK);
        h.assertTrue(machine.isPowered(), "redstone block next to it doesn't power it");

        // Riding can't be tested here: fake players can't mount entities. Sitting is covered by the in-game test plan.
        h.assertTrue(machine.findSeated().isEmpty(), "phantom sitter");
        h.assertTrue(machine.state() == io.github.constructchip.block.SoulKillerBlockEntity.State.IDLE, "not idle");
        h.succeed();
    }

    // ---------------------------------------------------------------- cyberware

    private static void implant(ServerPlayer p, io.github.constructchip.cyberware.Implant implant, int pos) {
        var s = io.github.constructchip.cyberware.Cyberware.state(p);
        s.rollIfNeeded(p.getRandom());
        s.installed.set(pos, new ItemStack(io.github.constructchip.cyberware.CyberItems.IMPLANTS.get(implant).get()));
        io.github.constructchip.cyberware.Cyberware.refreshAttributes(p);
    }

    @GameTest(template = "empty")
    public static void cyberwareLoadCapacityAndFoodRule(GameTestHelper h) {
        ServerPlayer p = player(h, 1, 1);
        var s = io.github.constructchip.cyberware.Cyberware.state(p);
        implant(p, io.github.constructchip.cyberware.Implant.WARP_DANCER, 0);
        implant(p, io.github.constructchip.cyberware.Implant.BLOOD_PUMP, 4);
        h.assertTrue(io.github.constructchip.cyberware.Cyberware.load(p) == 44, "load " + io.github.constructchip.cyberware.Cyberware.load(p));
        int cap = io.github.constructchip.cyberware.Cyberware.capacity(p);
        h.assertTrue(cap >= 160 && cap <= 320, "capacity " + cap);
        s.capacityLevel = 5;
        h.assertTrue(io.github.constructchip.cyberware.Cyberware.capacity(p) == Math.round(s.baseCapacity * 1.5f), "capacity x1.5");
        implant(p, io.github.constructchip.cyberware.Implant.HOLLOW_BONES, 6);
        h.assertTrue(io.github.constructchip.cyberware.Implant.HOLLOW_BONES.conflictsWith(io.github.constructchip.cyberware.Implant.TITANIUM_BONES), "hollow/titanium conflict");
        h.assertTrue(p.getMaxHealth() == 16f, "hollow bones max health " + p.getMaxHealth());
        done(h, p);
    }

    @GameTest(template = "empty")
    public static void bloodPumpHealsThenCoolsDown(GameTestHelper h) {
        ServerPlayer p = player(h, 1, 1);
        implant(p, io.github.constructchip.cyberware.Implant.BLOOD_PUMP, 4);
        p.setHealth(4f);
        var first = io.github.constructchip.cyberware.CyberEffects.activate(p, io.github.constructchip.cyberware.CyberEffects.Action.CIRCULATORY);
        h.assertTrue(first == null, "activation failed: " + (first == null ? "" : first.getString()));
        h.assertTrue(p.getHealth() >= 10f, "not healed: " + p.getHealth());
        h.assertTrue(io.github.constructchip.cyberware.CyberEffects.activate(p, io.github.constructchip.cyberware.CyberEffects.Action.CIRCULATORY) != null, "no cooldown");
        h.assertTrue(io.github.constructchip.cyberware.CyberEffects.activate(p, io.github.constructchip.cyberware.CyberEffects.Action.OS) != null, "empty OS slot activated");
        done(h, p);
    }

    @GameTest(template = "empty")
    public static void cyberpsychosisClimbsAndImmunoRollsBack(GameTestHelper h) {
        ServerPlayer p = player(h, 1, 1);
        var s = io.github.constructchip.cyberware.Cyberware.state(p);
        implant(p, io.github.constructchip.cyberware.Implant.WARP_DANCER, 0);
        s.pressure = 800;
        s.degradation = 338;
        io.github.constructchip.cyberware.Psychosis.update(p);
        h.assertTrue(s.phase == 2, "phase " + s.phase + " at degradation " + s.degradation);
        io.github.constructchip.cyberware.Psychosis.dose(p);
        h.assertTrue(s.degradation < 260 && s.pressure < 800, "dose didn't help: " + s.degradation + " / " + s.pressure);
        io.github.constructchip.cyberware.Psychosis.update(p);
        h.assertTrue(s.phase == 1, "no rollback, phase " + s.phase);
        done(h, p);
    }

    @GameTest(template = "empty")
    public static void shortCircuitShutsOffChrome(GameTestHelper h) {
        ServerPlayer p = player(h, 1, 1);
        var s = io.github.constructchip.cyberware.Cyberware.state(p);
        implant(p, io.github.constructchip.cyberware.Implant.WARP_DANCER, 0);
        h.assertTrue(io.github.constructchip.cyberware.CyberEffects.activate(p, io.github.constructchip.cyberware.CyberEffects.Action.OS) == null, "sandevistan");
        long now = p.level().getGameTime();
        h.assertTrue(s.isActive("OS", now), "not active");
        io.github.constructchip.cyberware.Cyberware.shortCircuit(p, 200);
        h.assertTrue(!s.isActive("OS", p.level().getGameTime() + 1), "still active after short circuit");
        h.assertTrue(s.cooldown("CIRCULATORY", now) >= 199, "no lockout cooldown");
        done(h, p);
    }

    @GameTest(template = "empty")
    public static void paintRoundTripsOnTheItem(GameTestHelper h) {
        int[] diff = new int[128 * 128];
        diff[5] = 0xFFF90759;
        diff[128 * 64 + 7] = 0xFF5EF6FF;
        String data = io.github.constructchip.paint.Paint.encode(diff);
        h.assertTrue(!data.isEmpty() && data.length() < io.github.constructchip.paint.Paint.MAX_LENGTH, "encoded length " + data.length());
        ItemStack arms = new ItemStack(io.github.constructchip.cyberware.CyberItems.IMPLANTS.get(io.github.constructchip.cyberware.Implant.GORILLA_ARMS).get());
        io.github.constructchip.paint.Paint.set(arms, "gorilla_arm_texture", data);
        int[] back = io.github.constructchip.paint.Paint.decode(io.github.constructchip.paint.Paint.of(arms).get("gorilla_arm_texture"), diff.length);
        h.assertTrue(back[5] == diff[5] && back[128 * 64 + 7] == diff[128 * 64 + 7] && back[6] == 0, "pixels changed");
        h.assertTrue(io.github.constructchip.paint.Paint.encode(new int[16]).isEmpty(), "empty paint should store nothing");
        h.assertTrue(io.github.constructchip.paint.Paint.textures(arms).size() == 2, "gorilla arms have two paintable textures");
        h.succeed();
    }
}
