package io.github.constructchip.possession;

import io.github.constructchip.fx.BoltFx;
import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import io.github.constructchip.registry.ModSounds;
import java.util.List;

/**
 * Takeover (top-tier skill): the construct takes over its host's body for up to 5 minutes; using it again lets go.
 * The 30 minute cooldown starts when it ends (see {@link Possession}), so the hack itself has none.
 */
public final class TakeoverHack extends SimpleQuickhack {
    public TakeoverHack() { super(10f, 0, TargetType.NONE, 0); }

    @Override public boolean drawsOwnFx() { return true; }

    @Override
    public HackResult execute(HackContext ctx) {
        HackResult r = Possession.start(ctx.construct(), ctx.host());
        if (r.succeeded()) {
            BoltFx.send(ctx.level(), BoltFx.Preset.CAPTURE_END, ctx.construct(), ctx.construct().getEyePosition(),
                    List.of(ctx.construct().position().add(0, 0.25, 0)));
            BoltFx.sound(ctx.level(), ctx.construct().getEyePosition(), ModSounds.ZAP.get(), 1.1f, 0.8f);
        }
        return r;
    }
}
