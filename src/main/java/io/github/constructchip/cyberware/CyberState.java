package io.github.constructchip.cyberware;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Per-player cyberware state (an attachment, copied on death: implants stay in your body). Mutable on purpose; entity
 * attachments are written whenever the player is saved. Transient combat values (fury, arms deployed) aren't saved.
 */
public final class CyberState {
    /** Their capacity multipliers per upgrade level. */
    public static final float[] CAPACITY_MULT = {1f, 1.08f, 1.16f, 1.25f, 1.35f, 1.50f};

    public final List<ItemStack> installed = new ArrayList<>();
    public int baseCapacity; // rolled 160..320 on first use; 0 = not rolled yet
    public int capacityLevel;
    public float pressure, degradation;
    public int phase;
    public long terminalUntil;
    public final Map<String, Long> cooldownUntil = new HashMap<>();
    public final Map<String, Long> activeUntil = new HashMap<>();
    public long immunoUntil;
    public int immunoDoses;

    // transient
    public boolean armsDeployed;
    public float fury;
    public long lastCombat, boosterUntil, lastElemental;

    public CyberState() {
        for (int i = 0; i < CyberSlot.POSITIONS; i++) installed.add(ItemStack.EMPTY);
    }

    public ItemStack at(int pos) {
        return installed.get(pos);
    }

    public Optional<Implant> implantAt(int pos) {
        return CyberwareItem.implantOf(installed.get(pos));
    }

    public boolean has(Implant implant) {
        for (ItemStack s : installed) if (CyberwareItem.implantOf(s).map(i -> i == implant).orElse(false)) return true;
        return false;
    }

    /** Upgrade level (0..4) of the installed {@code implant}, or -1 if not installed. */
    public int level(Implant implant) {
        for (ItemStack s : installed) {
            if (CyberwareItem.implantOf(s).map(i -> i == implant).orElse(false)) return CyberwareItem.upgrade(s);
        }
        return -1;
    }

    public boolean any() {
        for (ItemStack s : installed) if (!s.isEmpty()) return true;
        return false;
    }

    public int load() {
        int sum = 0;
        for (ItemStack s : installed) sum += CyberwareItem.implantOf(s).map(i -> i.load).orElse(0);
        return sum;
    }

    public void rollIfNeeded(RandomSource random) {
        if (baseCapacity <= 0) baseCapacity = 160 + random.nextInt(161);
    }

    public boolean isActive(String key, long now) {
        return activeUntil.getOrDefault(key, 0L) > now;
    }

    public long cooldown(String key, long now) {
        return Math.max(0L, cooldownUntil.getOrDefault(key, 0L) - now);
    }

    // ---------------------------------------------------------------- persistence
    private record Saved(List<ItemStack> installed, int baseCapacity, int capacityLevel, Psyche psyche,
                         Map<String, Long> cooldowns, Map<String, Long> actives, long immunoUntil, int immunoDoses) {}

    private record Psyche(float pressure, float degradation, int phase, long terminalUntil) {
        static final Codec<Psyche> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("pressure").forGetter(Psyche::pressure),
                Codec.FLOAT.fieldOf("degradation").forGetter(Psyche::degradation),
                Codec.INT.fieldOf("phase").forGetter(Psyche::phase),
                Codec.LONG.fieldOf("terminal_until").forGetter(Psyche::terminalUntil)
        ).apply(i, Psyche::new));
    }

    private static final Codec<Saved> SAVED = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("installed").forGetter(Saved::installed),
            Codec.INT.fieldOf("base_capacity").forGetter(Saved::baseCapacity),
            Codec.INT.fieldOf("capacity_level").forGetter(Saved::capacityLevel),
            Psyche.CODEC.fieldOf("psyche").forGetter(Saved::psyche),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("cooldowns").forGetter(Saved::cooldowns),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("actives").forGetter(Saved::actives),
            Codec.LONG.fieldOf("immuno_until").forGetter(Saved::immunoUntil),
            Codec.INT.fieldOf("immuno_doses").forGetter(Saved::immunoDoses)
    ).apply(i, Saved::new));

    public static final Codec<CyberState> CODEC = SAVED.xmap(CyberState::fromSaved, CyberState::toSaved);

    private Saved toSaved() {
        return new Saved(List.copyOf(installed), baseCapacity, capacityLevel, new Psyche(pressure, degradation, phase, terminalUntil),
                Map.copyOf(cooldownUntil), Map.copyOf(activeUntil), immunoUntil, immunoDoses);
    }

    private static CyberState fromSaved(Saved s) {
        CyberState c = new CyberState();
        for (int i = 0; i < Math.min(CyberSlot.POSITIONS, s.installed().size()); i++) c.installed.set(i, s.installed().get(i).copy());
        c.baseCapacity = s.baseCapacity();
        c.capacityLevel = s.capacityLevel();
        c.pressure = s.psyche().pressure();
        c.degradation = s.psyche().degradation();
        c.phase = s.psyche().phase();
        c.terminalUntil = s.psyche().terminalUntil();
        c.cooldownUntil.putAll(s.cooldowns());
        c.activeUntil.putAll(s.actives());
        c.immunoUntil = s.immunoUntil();
        c.immunoDoses = s.immunoDoses();
        return c;
    }
}
