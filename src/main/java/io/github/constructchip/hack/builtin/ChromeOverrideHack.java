package io.github.constructchip.hack.builtin;

import io.github.constructchip.cyberware.CyberEffects;
import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import net.minecraft.network.chat.Component;

/** Chrome Override: the construct fires the host's Sandevistan, or failing that their Blood Pump. */
public final class ChromeOverrideHack extends SimpleQuickhack {
    public ChromeOverrideHack() {
        super(6f, 20 * 20, TargetType.NONE, 0);
    }

    @Override
    public HackResult execute(HackContext ctx) {
        Component os = CyberEffects.activate(ctx.host(), CyberEffects.Action.OS);
        if (os == null) return HackResult.success();
        Component pump = CyberEffects.activate(ctx.host(), CyberEffects.Action.CIRCULATORY);
        if (pump == null) return HackResult.success();
        return HackResult.fail(Component.translatable("message.constructchip.override.none"));
    }
}
