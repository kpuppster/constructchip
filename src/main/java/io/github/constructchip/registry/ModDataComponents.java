package io.github.constructchip.registry;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.chip.ChipIdentity;
import io.github.constructchip.chip.ChipRam;
import io.github.constructchip.chip.ChipSkills;
import io.github.constructchip.chip.ChipTimers;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> REGISTER =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ConstructChip.MODID);

    /** Who is on the chip. Absent means a blank chip. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ChipIdentity>> CHIP_IDENTITY =
            REGISTER.register("chip_identity", () -> DataComponentType.<ChipIdentity>builder()
                    .persistent(ChipIdentity.CODEC)
                    .networkSynchronized(ChipIdentity.STREAM_CODEC)
                    .build());

    /** RAM pool, lazily regenerated from a game-time stamp. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ChipRam>> CHIP_RAM =
            REGISTER.register("chip_ram", () -> DataComponentType.<ChipRam>builder()
                    .persistent(ChipRam.CODEC)
                    .networkSynchronized(ChipRam.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ChipSkills>> CHIP_SKILLS =
            REGISTER.register("chip_skills", () -> DataComponentType.<ChipSkills>builder()
                    .persistent(ChipSkills.CODEC)
                    .networkSynchronized(ChipSkills.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ChipTimers>> CHIP_TIMERS =
            REGISTER.register("chip_timers", () -> DataComponentType.<ChipTimers>builder()
                    .persistent(ChipTimers.CODEC)
                    .networkSynchronized(ChipTimers.STREAM_CODEC)
                    .build());

    /**
     * Bumped on every legitimate hand-over (install, eject, host death). The registry stores the current serial;
     * a stack with an older serial is a stale copy and gets wiped to a blank chip.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHIP_SERIAL =
            REGISTER.register("chip_serial", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Cyberware upgrade level (0..4), bought at the Ripperdoc with Internal Parts. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CYBER_UPGRADE =
            REGISTER.register("cyber_upgrade", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, 4)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    /** Gorilla Arms element: thermal, electric, toxic or frozen. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ARMS_ELEMENT =
            REGISTER.register("arms_element", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

    /** Implant paint: model texture name → painted pixels (see paint.Paint). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.Map<String, String>>> CYBER_PAINT =
            REGISTER.register("cyber_paint", () -> DataComponentType.<java.util.Map<String, String>>builder()
                    .persistent(Codec.unboundedMap(Codec.STRING, Codec.STRING))
                    .networkSynchronized(ByteBufCodecs.<io.netty.buffer.ByteBuf, String, String, java.util.Map<String, String>>map(
                            java.util.HashMap::new, ByteBufCodecs.stringUtf8(64), ByteBufCodecs.stringUtf8(io.github.constructchip.paint.Paint.MAX_LENGTH)))
                    .build());

    private ModDataComponents() {}
}
