package io.github.constructchip.hack.builtin;

import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
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
        ServerPlayer host = ctx.host();
        List<LivingEntity> found = ctx.level().getEntitiesOfClass(LivingEntity.class, host.getBoundingBox().inflate(RADIUS),
                e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(host) <= RADIUS * RADIUS);
        HackFx.highlight(found.stream().map(LivingEntity::getId).toList(), 200, ctx.construct(), host);
        HackFx.glitch(ctx.construct());
        if (found.isEmpty()) return HackResult.status(Component.translatable("message.constructchip.ping.none"));
        // count by side, relative to where the host faces (glow already shows them through walls)
        Map<String, Integer> sides = new LinkedHashMap<>();
        for (String side : new String[]{"ahead", "behind", "left", "right"}) sides.put(side, 0);
        for (LivingEntity e : found) sides.merge(Passives.sideOf(host, e.position()), 1, Integer::sum);
        MutableComponent parts = Component.empty();
        boolean first = true;
        for (Map.Entry<String, Integer> side : sides.entrySet()) {
            if (side.getValue() == 0) continue;
            if (!first) parts.append(", ");
            parts.append(Component.translatable("message.constructchip.ping.side", side.getValue(),
                    Component.translatable("message.constructchip.threat." + side.getKey())));
            first = false;
        }
        return HackResult.status(Component.translatable("message.constructchip.ping", found.size(), parts));
    }
}
