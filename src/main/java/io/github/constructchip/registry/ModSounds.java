package io.github.constructchip.registry;

import io.github.constructchip.ConstructChip;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sounds by Ikeda (Capri mod), used with permission; made from CC0 recordings (see licenses/SOUNDS_CC0.txt). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, ConstructChip.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> CHIP_IN = sound("chip_in");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIP_OUT = sound("chip_out");
    public static final DeferredHolder<SoundEvent, SoundEvent> CONSTRUCT_APPEAR = sound("construct_appear");
    public static final DeferredHolder<SoundEvent, SoundEvent> CONSTRUCT_VANISH = sound("construct_vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> HACK = sound("hack");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT = sound("bolt");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM = sound("storm");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_HOVER = sound("ui_hover");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLIP = sound("blip");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(ConstructChip.id(name)));
    }
}
