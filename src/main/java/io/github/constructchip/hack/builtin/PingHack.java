package io.github.constructchip.hack.builtin;

import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/** Hostile creatures within 24 blocks of the host glow for host and construct (only them) for 10 s. */
public final class PingHack extends SimpleQuickhack {
    public static final double RADIUS = 24;

    public PingHack() {
        super(2f, 20 * 20, TargetType.NONE, 0);
    }

    @Override
    public HackResult execute(HackContext ctx) {
        List<Integer> ids = ctx.level().getEntitiesOfClass(LivingEntity.class, ctx.host().getBoundingBox().inflate(RADIUS),
                        e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(ctx.host()) <= RADIUS * RADIUS)
                .stream().map(LivingEntity::getId).toList();
        HackFx.highlight(ids, 200, ctx.construct(), ctx.host());
        HackFx.glitch(ctx.construct());
        return HackResult.success(Component.translatable("message.constructchip.ping", ids.size()));
    }
}
