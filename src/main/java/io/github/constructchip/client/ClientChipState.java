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
    private static int selectedHack;

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

    /** Quickhacks the chip can use, in skill-tree order (tier, then column). */
    public static List<ResourceLocation> unlockedHacks() {
        List<ResourceLocation> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (!chip.present() || mc.level == null) return out;
        Registry<Skill> skills = ModSkills.get(mc.level.registryAccess());
        skills.entrySet().stream()
                .filter(e -> chip.skills().has(e.getKey().location()))
                .sorted((a, b) -> a.getValue().tier() != b.getValue().tier()
                        ? Integer.compare(a.getValue().tier(), b.getValue().tier())
                        : Integer.compare(a.getValue().column(), b.getValue().column()))
                .forEach(e -> e.getValue().quickhack().ifPresent(h -> { if (!out.contains(h)) out.add(h); }));
        return out;
    }

    public static ResourceLocation selectedHack() {
        List<ResourceLocation> hacks = unlockedHacks();
        if (hacks.isEmpty()) return null;
        return hacks.get(Math.floorMod(selectedHack, hacks.size()));
    }

    public static void cycleHack(int delta) {
        selectedHack += delta;
    }
}
