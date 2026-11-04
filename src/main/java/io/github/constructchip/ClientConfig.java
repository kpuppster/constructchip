package io.github.constructchip;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client options ({@code config/constructchip-client.toml}). */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SANDEVISTAN, GORILLA_ARMS, BLOOD_PUMP, FORTIFIED_ANKLES, REINFORCED_TENDONS,
            HIDE_ARMS_UNDER_GORILLA;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("3D implant models drawn on players (yours and everyone else's). Turn any of them off.").push("implant_models");
        SANDEVISTAN = b.comment("Sandevistan spine on the back").define("sandevistan", true);
        GORILLA_ARMS = b.comment("Gorilla Arms").define("gorillaArms", true);
        BLOOD_PUMP = b.comment("Blood Pump on the chest").define("bloodPump", true);
        FORTIFIED_ANKLES = b.comment("Fortified Ankles").define("fortifiedAnkles", true);
        REINFORCED_TENDONS = b.comment("Reinforced Tendons").define("reinforcedTendons", true);
        HIDE_ARMS_UNDER_GORILLA = b.comment("Hide the normal arms under the Gorilla Arms model (third person)").define("hideArmsUnderGorillaArms", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {}
}
