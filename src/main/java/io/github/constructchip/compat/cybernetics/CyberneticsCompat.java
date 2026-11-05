package io.github.constructchip.compat.cybernetics;

import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.CyberwareAccess;
import com.perigrine3.createcybernetics.common.toggle.CyberwareToggleController;
import io.github.constructchip.ConstructChip;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Optional compatibility with Perigrine3's Cybernetics ({@code createcybernetics}). Only ever loaded when that mod is
 * installed (callers check {@link #LOADED} first), so nothing here is touched otherwise. Neither mod's content is
 * shared: our hacks just use Cybernetics' own ways of knocking cyberware out.
 *
 * <ul>
 *   <li>Short Circuit: Cybernetics' EMP effect for the hack's duration. It drains the target's implant energy every
 *       tick (so powered implants stop) and freezes cyber-mobs in place; a Capacitor Frame resists it, as in their mod.</li>
 *   <li>System Lock: the same EMP, plus every implant on the target's toggle wheel is switched off. They can switch
 *       them back on, but nothing has power until the EMP runs out.</li>
 * </ul>
 */
public final class CyberneticsCompat {
    public static final String MOD_ID = "createcybernetics";
    private static final ResourceLocation EMP = ResourceLocation.fromNamespaceAndPath(MOD_ID, "emp_effect");

    private CyberneticsCompat() {}

    /** Returns how many Cybernetics implants the target has installed (0 if none, or if anything goes wrong). */
    public static int disable(LivingEntity target, int ticks, boolean lock) {
        try {
            int installed = countInstalled(target);
            if (installed == 0) return 0;
            Optional<Holder.Reference<MobEffect>> emp = BuiltInRegistries.MOB_EFFECT.getHolder(EMP);
            emp.ifPresent(e -> target.addEffect(new MobEffectInstance(e, ticks, 0, false, true, true)));
            if (lock && target instanceof ServerPlayer player) {
                for (ResourceLocation id : CyberwareToggleController.collectToggleables(player).keySet()) {
                    if (CyberwareToggleController.isActive(player, id)) CyberwareToggleController.setActive(player, id, false);
                }
            }
            return installed;
        } catch (RuntimeException | LinkageError e) {
            // a Cybernetics update changed something: don't break the hack, just skip their implants
            ConstructChip.LOGGER.warn("Cybernetics compat failed; skipping its implants", e);
            return 0;
        }
    }

    private static int countInstalled(LivingEntity target) {
        Map<?, InstalledCyberware[]> all = CyberwareAccess.getAll(target);
        if (all == null) return 0;
        int n = 0;
        for (InstalledCyberware[] slot : all.values()) {
            if (slot == null) continue;
            for (InstalledCyberware c : slot) {
                if (c == null) continue;
                ItemStack item = c.getItem();
                if (item != null && !item.isEmpty()) n++;
            }
        }
        return n;
    }
}
