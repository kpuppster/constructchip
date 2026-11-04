package io.github.constructchip.hack.builtin;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.hack.HackContext;
import io.github.constructchip.hack.HackResult;
import io.github.constructchip.hack.SimpleQuickhack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Marks a creature: it takes 15% more damage from the host for 10 s, and glows for host and construct. */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class WeakPointHack extends SimpleQuickhack {
    public static final int DURATION = 200;
    public static final float BONUS = 1.15f;
    private record Mark(UUID host, long until) {}
    private static final Map<UUID, Mark> MARKS = new HashMap<>();

    public WeakPointHack() {
        super(3f, 15 * 20, TargetType.ENTITY, 16);
    }

    @Override
    public HackResult execute(HackContext ctx) {
        Entity target = ctx.entity().orElseThrow();
        if (!(target instanceof LivingEntity living)) return HackResult.fail(Component.translatable("message.constructchip.hack.wrong_target"));
        MARKS.put(living.getUUID(), new Mark(ctx.host().getUUID(), ctx.now() + DURATION));
        HackFx.highlight(List.of(living.getId()), DURATION, ctx.construct(), ctx.host());
        HackFx.sparks(ctx.level(), living);
        return HackResult.success(Component.translatable("message.constructchip.weak_point", living.getDisplayName()));
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (MARKS.isEmpty() || event.getEntity().level().isClientSide) return;
        Mark mark = MARKS.get(event.getEntity().getUUID());
        if (mark == null) return;
        if (event.getEntity().level().getGameTime() > mark.until()) {
            MARKS.remove(event.getEntity().getUUID());
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && attacker.getUUID().equals(mark.host())) event.setAmount(event.getAmount() * BONUS);
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        MARKS.clear();
    }
}
