package io.github.constructchip.hack;

import io.github.constructchip.skill.ChipStats;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Everything a quickhack needs, already validated.
 *
 * @param hackId    id of the running quickhack
 * @param construct the captured player who triggered it (send feedback here)
 * @param host      the player whose head the chip is in. Remote interactions act as the host (permissions, events)
 * @param chip      the installed chip (inside the host's attachment). Don't mutate it; the service writes RAM, cooldown and timers
 * @param stats     the chip's numbers after skills
 * @param entity    resolved entity target, if the hack targeted one
 * @param block     resolved block target, if the hack targeted one
 * @param face      side of the block that was aimed at (UP if unknown)
 * @param now       game time of the host's level
 */
public record HackContext(ResourceLocation hackId, ServerPlayer construct, ServerPlayer host, ItemStack chip,
                          ChipStats stats, Optional<Entity> entity, Optional<BlockPos> block, Direction face,
                          long now) {
    public ServerLevel level() {
        return host.serverLevel();
    }
}
