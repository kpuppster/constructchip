package io.github.constructchip.cyberware;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.api.CyberwareApi;
import io.github.constructchip.attachment.ModAttachments;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.network.Net;
import io.github.constructchip.tracking.ChipTracker;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Shared cyberware queries: state, capacity, load, and the attribute bonuses implants give. */
public final class Cyberware {
    /** A construct chip in your head is chrome too. */
    public static final int CHIP_LOAD = 20;

    private Cyberware() {}

    public static CyberState state(Player player) {
        return player.getData(ModAttachments.CYBER_STATE);
    }

    /** Implants do nothing for constructs (a ghost has no body) or while System Locked. */
    public static boolean suppressed(Player player) {
        return ChipTracker.isConstruct(player) || CyberwareApi.isLocked(player);
    }

    public static int load(Player player) {
        int load = state(player).load();
        if (player instanceof ServerPlayer sp && ChipStacks.isCaptured(ChipTracker.installedChip(sp))) load += CHIP_LOAD;
        return load;
    }

    public static int capacity(Player player) {
        CyberState s = state(player);
        float cap = Math.max(160, s.baseCapacity) * CyberState.CAPACITY_MULT[Math.min(5, s.capacityLevel)];
        int epi = s.level(Implant.EPIMORPHIC_SKELETON);
        if (epi >= 0) cap *= 1.10f + 0.02f * epi;
        long now = player.level().getGameTime();
        if (s.immunoUntil > now) cap *= 1.30f + 0.10f * Math.max(0, s.immunoDoses - 1);
        return Math.round(cap);
    }

    // ---------------------------------------------------------------- attributes

    private static void set(Player p, Holder<Attribute> attr, String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        ResourceLocation id = ConstructChip.id("cyber_" + name);
        if (amount == 0) inst.removeModifier(id);
        else inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    /** Re-applies every implant's attribute bonus. Cheap; called on install/eject, arms toggle, and once a second. */
    public static void refreshAttributes(Player p) {
        CyberState s = state(p);
        boolean off = suppressed(p);
        double speed = 0, health = 0, damage = 0, knockback = 0, jump = 0, fall = 0, safeFall = 0;
        if (!off) {
            if (s.has(Implant.BIONIC_JOINTS)) { speed += 0.05 + 0.01 * s.level(Implant.BIONIC_JOINTS); fall -= 0.25; knockback += 0.1; }
            if (s.has(Implant.HOLLOW_BONES)) { speed += 0.10; health -= 4; jump += 0.1; fall -= 0.5; }
            if (s.has(Implant.RARA_AVIS)) speed -= 0.10;
            if (s.has(Implant.TITANIUM_BONES)) { knockback += 0.25; fall += 0.2; }
            if (s.has(Implant.EPIMORPHIC_SKELETON)) health += 8 + 2 * s.level(Implant.EPIMORPHIC_SKELETON);
            if (s.has(Implant.DENSE_MARROW)) damage += 2 + 0.5 * s.level(Implant.DENSE_MARROW);
            if (s.has(Implant.SPRING_JOINTS)) { fall -= 0.6; safeFall += 2; }
            if (s.has(Implant.FORTIFIED_ANKLES)) safeFall += 3;
            if (s.armsDeployed && s.has(Implant.GORILLA_ARMS)) damage += Implant.GORILLA_ARMS.param;
        }
        set(p, Attributes.MOVEMENT_SPEED, "speed", speed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.MAX_HEALTH, "health", health, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.ATTACK_DAMAGE, "damage", damage, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.KNOCKBACK_RESISTANCE, "knockback", knockback, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.JUMP_STRENGTH, "jump", jump, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.FALL_DAMAGE_MULTIPLIER, "fall", Math.max(-0.9, fall), AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.SAFE_FALL_DISTANCE, "safe_fall", safeFall, AttributeModifier.Operation.ADD_VALUE);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Short Circuit / System Lock on a chromed player: everything active shuts off and cools down. */
    public static void shortCircuit(ServerPlayer p, int ticks) {
        CyberState s = state(p);
        long now = p.level().getGameTime();
        for (String key : s.activeUntil.keySet()) s.activeUntil.put(key, now);
        for (CyberSlot slot : new CyberSlot[]{CyberSlot.OS, CyberSlot.FACE, CyberSlot.CIRCULATORY, CyberSlot.ARMS}) {
            String key = slot.name();
            s.cooldownUntil.put(key, Math.max(s.cooldownUntil.getOrDefault(key, 0L), now + ticks));
        }
        s.armsDeployed = false;
        refreshAttributes(p);
        CyberSync.send(p);
        ChromeVisuals.broadcast(p);
    }

    static void sendGlitch(ServerPlayer p, int ticks, float red, float flash) {
        Net.send(p, new io.github.constructchip.network.GlitchPayload(ticks, red, flash));
    }
}
