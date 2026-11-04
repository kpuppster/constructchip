package io.github.constructchip.hack.builtin;

import io.github.constructchip.ConstructChip;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.skill.ChipStats;
import io.github.constructchip.skill.ModSkills;
import io.github.constructchip.tracking.ChipTracker;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Always-on skill effects: Pain Editor (host takes less damage) and Threat Alert (warning when targeted). */
@EventBusSubscriber(modid = ConstructChip.MODID)
public final class Passives {
    public static final ResourceLocation THREAT_ALERT = ConstructChip.id("threat_alert");
    private static final Map<UUID, Long> LAST_ALERT = new HashMap<>();

    private Passives() {}

    /** The host's valid installed chip, or EMPTY. */
    private static ItemStack chipOf(ServerPlayer host) {
        ItemStack chip = ChipTracker.installedChip(host);
        if (!ChipStacks.isCaptured(chip) || ChipTracker.currentEntry(host.server, chip).isEmpty()) return ItemStack.EMPTY;
        return chip;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHostDamaged(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer host)) return;
        ItemStack chip = chipOf(host);
        if (chip.isEmpty()) return;
        float mult = ChipStats.of(chip, ModSkills.get(host.server.registryAccess())).hostDamageTaken();
        if (mult != 1f) event.setAmount(event.getAmount() * mult);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTargeted(LivingChangeTargetEvent event) {
        if (event.isCanceled() || !(event.getNewAboutToBeSetTarget() instanceof ServerPlayer host)) return;
        LivingEntity mob = event.getEntity();
        ItemStack chip = chipOf(host);
        if (chip.isEmpty() || !ChipStacks.skills(chip).has(THREAT_ALERT)
                || !ModSkills.get(host.server.registryAccess()).containsKey(THREAT_ALERT)) return;
        long now = host.level().getGameTime();
        Long last = LAST_ALERT.get(host.getUUID());
        if (last != null && now - last < 40) return;
        LAST_ALERT.put(host.getUUID(), now);

        Vec3 to = mob.position().subtract(host.position());
        float angle = Mth.wrapDegrees((float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90f - host.getYRot());
        String dir = Math.abs(angle) <= 45 ? "ahead" : Math.abs(angle) >= 135 ? "behind" : angle > 0 ? "right" : "left";
        Component msg = Component.translatable("message.constructchip.threat", mob.getDisplayName(),
                Component.translatable("message.constructchip.threat." + dir));
        host.displayClientMessage(msg, true);
        host.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 0.8f, 1.8f);
    }
}
