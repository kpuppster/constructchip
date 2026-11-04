package io.github.constructchip.cyberware;

import java.util.Locale;

/**
 * Every implant: body slot, Load, tier, and (for actives) duration, cooldown and Cyberpsychosis pressure per second
 * while running. Numbers follow Project: Edgerunner's guide (A24-N22), used with permission.
 * Effects live in {@link CyberEffects}; this is just the table.
 */
public enum Implant {
    // OS: Sandevistans. param = world slowdown (0..1)
    WARP_DANCER(CyberSlot.OS, 20, 1, 9 * 20, 70 * 20, 160, 0.20f, "sandevistan_warp_dancer"),
    ZETATECH(CyberSlot.OS, 34, 2, 6 * 20, 45 * 20, 160, 0.30f, "sandevistan_zetatech"),
    DYNALAR(CyberSlot.OS, 50, 3, 8 * 20, 55 * 20, 160, 0.50f, "sandevistan_dynalar"),
    FALCON(CyberSlot.OS, 68, 4, 9 * 20, 40 * 20, 160, 0.70f, "sandevistan_falcon"),
    APOGEE(CyberSlot.OS, 90, 5, 6 * 20, 30 * 20, 160, 0.85f, "sandevistan_apogee"),
    // CORTEX
    AXOLOTL(CyberSlot.CORTEX, 62, 4, 0, 0, 0, 0.05f, "axolotl"),
    // FACE: optics. param = zoom factor; scans run 60 s, cooldown 30 s
    BASIC_KIROSHI(CyberSlot.FACE, 8, 2, 0, 0, 0, 6f, "basic_kiroshi_optics"),
    CLAIRVOYANT(CyberSlot.FACE, 14, 3, 60 * 20, 30 * 20, 120, 8f, "clairvoyant"),
    DOOMSAYER(CyberSlot.FACE, 14, 3, 60 * 20, 30 * 20, 120, 8f, "doomsayer"),
    STALKER(CyberSlot.FACE, 14, 3, 60 * 20, 30 * 20, 120, 8f, "stalker"),
    THE_ORACLE(CyberSlot.FACE, 26, 5, 60 * 20, 30 * 20, 120, 12f, "the_oracle"),
    // ARMS: deploy toggle, pressure while deployed
    GORILLA_ARMS(CyberSlot.ARMS, 35, 4, 0, 20, 140, 6f, "gorilla_arms"),
    // CIRCULATORY: instant heal, 30 s cooldown
    BLOOD_PUMP(CyberSlot.CIRCULATORY, 24, 2, 6 * 20, 30 * 20, 100, 6f, "blood_pump"),
    // LEGS (choose one)
    FORTIFIED_ANKLES(CyberSlot.LEGS, 12, 1, 0, 0, 0, 0f, "fortified_ankles"),
    REINFORCED_TENDONS(CyberSlot.LEGS, 18, 2, 0, 0, 0, 0f, "reinforced_tendons"),
    // SKELETON (two slots, passive)
    TITANIUM_BONES(CyberSlot.SKELETON, 6, 1, 0, 0, 0, 0f, "titanium_bones"),
    BIONIC_JOINTS(CyberSlot.SKELETON, 8, 1, 0, 0, 0, 0f, "bionic_joints"),
    HOLLOW_BONES(CyberSlot.SKELETON, 10, 2, 0, 0, 0, 0f, "hollow_bones"),
    SCARAB(CyberSlot.SKELETON, 14, 2, 0, 0, 0, 0f, "scarab"),
    SCAR_COALESCER(CyberSlot.SKELETON, 20, 2, 0, 0, 0, 0f, "scar_coalescer"),
    DENSE_MARROW(CyberSlot.SKELETON, 16, 3, 0, 0, 0, 0f, "dense_marrow"),
    KINETIC_FRAME(CyberSlot.SKELETON, 16, 3, 0, 0, 0, 0f, "kinetic_frame"),
    SPRING_JOINTS(CyberSlot.SKELETON, 16, 3, 0, 0, 0, 0f, "spring_joints"),
    PARA_BELLUM(CyberSlot.SKELETON, 25, 3, 0, 0, 0, 0f, "para_bellum"),
    UNIVERSAL_BOOSTER(CyberSlot.SKELETON, 25, 4, 0, 0, 0, 0f, "universal_booster"),
    EPIMORPHIC_SKELETON(CyberSlot.SKELETON, 40, 5, 0, 0, 0, 0f, "epimorphic_skeleton"),
    RARA_AVIS(CyberSlot.SKELETON, 45, 5, 0, 0, 0, 0f, "rara_avis");

    public static final int MAX_UPGRADE = 4;

    public final CyberSlot slot;
    public final int load, tier, activeTicks, cooldownTicks, pressurePerSecond;
    public final float param;
    public final String texture;

    Implant(CyberSlot slot, int load, int tier, int activeTicks, int cooldownTicks, int pressurePerSecond, float param, String texture) {
        this.slot = slot;
        this.load = load;
        this.tier = tier;
        this.activeTicks = activeTicks;
        this.cooldownTicks = cooldownTicks;
        this.pressurePerSecond = pressurePerSecond;
        this.param = param;
        this.texture = texture;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isSandevistan() { return slot == CyberSlot.OS; }
    public boolean hasScan() { return slot == CyberSlot.FACE && activeTicks > 0; }
    public boolean scansHostiles() { return this == CLAIRVOYANT || this == STALKER || this == THE_ORACLE; }
    public boolean scansHazards() { return this == DOOMSAYER || this == THE_ORACLE; }
    public boolean tracksTrails() { return this == STALKER || this == THE_ORACLE; }

    /** Hollow Bones can't share the skeleton with heavy frames (their rule). */
    public boolean conflictsWith(Implant other) {
        boolean heavy = other == TITANIUM_BONES || other == DENSE_MARROW || other == RARA_AVIS;
        boolean heavyThis = this == TITANIUM_BONES || this == DENSE_MARROW || this == RARA_AVIS;
        return (this == HOLLOW_BONES && heavy) || (other == HOLLOW_BONES && heavyThis) || this == other;
    }
}
