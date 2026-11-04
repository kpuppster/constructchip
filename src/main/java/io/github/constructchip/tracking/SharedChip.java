package io.github.constructchip.tracking;

import io.github.constructchip.attachment.ModAttachments;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.hack.QuickhackService;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The installed chip a player is connected to: the construct's (via its host) or the host's own.
 * After mutating {@link #chip()}, call {@link #save()} so the host's attachment is marked dirty.
 */
public record SharedChip(ServerPlayer host, ItemStack chip, boolean viewerIsHost) {
    public static Optional<SharedChip> of(ServerPlayer player) {
        if (ChipTracker.isConstruct(player)) {
            return QuickhackService.hostOf(player).flatMap(host -> valid(host, false));
        }
        return valid(player, true);
    }

    private static Optional<SharedChip> valid(ServerPlayer host, boolean viewerIsHost) {
        ItemStack chip = ChipTracker.installedChip(host);
        if (!ChipStacks.isCaptured(chip) || ChipTracker.currentEntry(host.server, chip).isEmpty()) return Optional.empty();
        return Optional.of(new SharedChip(host, chip, viewerIsHost));
    }

    public void save() {
        host.setData(ModAttachments.INSTALLED_CHIP, chip);
    }
}
