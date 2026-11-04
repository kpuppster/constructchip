package io.github.constructchip.cyberware;

import java.util.Locale;

/** Assembly intermediates and Internal Parts (upgrade currency, Ripperdoc villager only). Texture = id. */
public enum Part {
    INTERNAL_PARTS_T1(1), INTERNAL_PARTS_T2(2), INTERNAL_PARTS_T3(3), INTERNAL_PARTS_T4(4), INTERNAL_PARTS_T5(5),
    KINETIC_ACTUATOR_T1(1), OS_FRAME_T1(1), TEMPORAL_MICRODRIVER_T1(1), SKELETAL_ANCHOR_T1(1), JOINT_SERVO_T1(1),
    HEMO_CHAMBER_T2(2), BIO_MONITOR_T2(2), MYOFIBER_BUNDLE_T2(2), OCULAR_SOCKET_T2(2), OS_CORE_T2(2),
    TEMPORAL_REGULATOR_T2(2), ZOOM_LENS_T2(2), HOLLOW_LATTICE_T2(2), COAGULATION_MESH_T2(2), SCARAB_SHELL_PLATE_T2(2),
    HAZARD_SCAN_KERNEL_T3(3), HOSTILE_SCAN_KERNEL_T3(3), OS_CORE_T3(3), PHASE_STABILIZER_T3(3), TRAIL_TRACKER_ARRAY_T3(3),
    ARMS_CHASSIS_T3(3), MARROW_PRESSURE_CORE_T3(3), KINETIC_DAMPER_FRAME_T3(3), SPRING_RECOIL_ARRAY_T3(3),
    BALLISTIC_SPINE_PLATE_T3(3),
    OS_CORE_T4(4), FALCON_OVERCLOCKER_T4(4), LATENCY_SHUNT_T4(4), NEURAL_MATRIX_T4(4), COOLDOWN_OPTIMIZER_T4(4),
    OPTIC_FUSION_CONTROLLER_T4(4), ARMS_ACTUATOR_MATRIX_T4(4), ARMS_POWER_CORE_T4(4), TRAUMA_BOOSTER_MODULE_T4(4),
    OS_CORE_T5(5), APOGEE_OVERDRIVE_KERNEL_T5(5), QUANTUM_SYNC_LATTICE_T5(5), ICONIC_SKELETAL_CHASSIS_T5(5),
    REGEN_NANITE_SCAFFOLD_T5(5);

    public final int tier;

    Part(int tier) {
        this.tier = tier;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Texture name in their art set ("internal parts" are named cyberware_internal_parts_tN). */
    public String texture() {
        return name().startsWith("INTERNAL_PARTS") ? "cyberware_" + id() : id();
    }

    public static Part internalParts(int tier) {
        return values()[Math.max(1, Math.min(5, tier)) - 1];
    }
}
