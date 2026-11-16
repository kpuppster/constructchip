package io.github.constructchip.client;

import io.github.constructchip.network.ChipStatusPayload;
import io.github.constructchip.network.HostStatusPayload;
import io.github.constructchip.skill.ModSkills;
import io.github.constructchip.skill.Skill;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

/** What this client knows about its chip (as construct or host). Filled by payloads; read by HUD and screens. */
public final class ClientChipState {
    private static ChipStatusPayload chip = ChipStatusPayload.NONE;
    private static HostStatusPayload host;
    private static ResourceLocation selected;

    private ClientChipState() {}

    public static ChipStatusPayload chip() { return chip; }
    public static HostStatusPayload host() { return host; }

    public static boolean isConstruct() { return chip.present() && !chip.isHost(); }
    public static boolean isHost() { return chip.present() && chip.isHost(); }

    static void setChip(ChipStatusPayload payload) {
        chip = payload;
        if (!payload.present()) host = null;
    }

    static void setHost(HostStatusPayload payload) { host = payload; }

    public static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0L : mc.level.getGameTime();
    }

    /** Cache for {@link #unlockedHacks}: rebuilt only when the chip's skills or the skill registry change. */
    private static List<ResourceLocation> hackCache = List.of();
    private static Object hackCacheSkills, hackCacheRegistry;

    /** Quickhacks the chip can use, in skill-tree order (tier, then column). Cached; don't modify the list. */
    public static List<ResourceLocation> unlockedHacks() {
        Minecraft mc = Minecraft.getInstance();
        if (!chip.present() || mc.level == null) return List.of();
        Registry<Skill> skills = ModSkills.get(mc.level.registryAccess());
        if (chip.skills() == hackCacheSkills && skills == hackCacheRegistry) return hackCache;
        List<ResourceLocation> out = new ArrayList<>();
        skills.entrySet().stream()
                .filter(e -> chip.skills().has(e.getKey().location()))
                .sorted((a, b) -> a.getValue().tier() != b.getValue().tier()
                        ? Integer.compare(a.getValue().tier(), b.getValue().tier())
                        : Integer.compare(a.getValue().column(), b.getValue().column()))
                .forEach(e -> e.getValue().quickhack().ifPresent(h -> { if (!out.contains(h)) out.add(h); }));
        hackCache = List.copyOf(out);
        hackCacheSkills = chip.skills();
        hackCacheRegistry = skills;
        return hackCache;
    }

    /**
     * The selected quickhack. Remembered by id, so unlocking a new skill (which can reorder the list) never silently
     * swaps it for another hack; falls back to the first one if it's no longer available.
     */
    public static ResourceLocation selectedHack() {
        List<ResourceLocation> hacks = unlockedHacks();
        if (hacks.isEmpty()) return null;
        if (selected == null || !hacks.contains(selected)) selected = hacks.get(0);
        return selected;
    }

    /** Make {@code hack} the selected quickhack (from the power wheel). */
    public static void selectHack(ResourceLocation hack) {
        if (unlockedHacks().contains(hack)) selected = hack;
    }

    /** Icon of the skill that grants {@code hack}, for the wheel. */
    public static net.minecraft.world.item.ItemStack iconFor(ResourceLocation hack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return net.minecraft.world.item.ItemStack.EMPTY;
        for (Skill s : ModSkills.get(mc.level.registryAccess())) {
            if (s.quickhack().map(hack::equals).orElse(false)) return new net.minecraft.world.item.ItemStack(s.icon());
        }
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
    }

    public static void cycleHack(int delta) {
        List<ResourceLocation> hacks = unlockedHacks();
        if (hacks.isEmpty()) return;
        int i = Math.max(0, hacks.indexOf(selectedHack()));
        selected = hacks.get(Math.floorMod(i + delta, hacks.size()));
    }
}
