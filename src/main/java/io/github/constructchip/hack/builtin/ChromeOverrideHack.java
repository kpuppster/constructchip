package io.github.constructchip.hack.builtin;

import io.github.constructchip.compat.chromeware.ChromewareCompat;
import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import net.minecraft.network.chat.Component;

/**
 * Chrome Override (needs Create: Chromeware): the construct fires one of its host's implant abilities, Sandevistan
 * first, then Berserk, Blood Pump, Optical Camo, Overclock or the arm implant. Chromeware's own rules apply (disabled
 * chrome, cooldowns, power).
 */
public final class ChromeOverrideHack extends SimpleQuickhack {
    public ChromeOverrideHack() { super(6f, 45 * 20, TargetType.NONE, 0); }

    @Override
    public HackResult execute(HackContext ctx) {
        if (!ChromewareCompat.available()) return HackResult.fail(Component.translatable("message.constructchip.override.no_chromeware"));
        Component fired = ChromewareCompat.override(ctx.host());
        if (fired == null) return HackResult.fail(Component.translatable("message.constructchip.override.none"));
        HackFx.glitchFlash(ctx.construct(), 12, 0.3f, 0.4f);
        return HackResult.success(Component.translatable("message.constructchip.override", fired));
    }
}
