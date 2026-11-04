package io.github.constructchip.network;

import io.github.constructchip.capture.ConsentManager;
import io.github.constructchip.chip.ChipSkills;
import io.github.constructchip.chip.ChipStacks;
import io.github.constructchip.hack.QuickhackService;
import io.github.constructchip.menu.HostInventoryMenu;
import io.github.constructchip.skill.ChipStats;
import io.github.constructchip.skill.ModSkills;
import io.github.constructchip.skill.Skill;
import io.github.constructchip.skill.SkillPointCost;
import io.github.constructchip.skill.SkillTree;
import io.github.constructchip.sync.ChipSync;
import io.github.constructchip.tracking.ChipTracker;
import io.github.constructchip.tracking.SharedChip;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server side of the small client → server requests. Nothing from the client is trusted. */
final class ServerHandlers {
    private ServerHandlers() {}

    static void openHostInventory(ServerboundPayloads.OpenHostInventory payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer construct) || !ChipTracker.isConstruct(construct)) return;
            Optional<ServerPlayer> host = QuickhackService.hostOf(construct);
            if (host.isEmpty()) {
                construct.displayClientMessage(Component.translatable("message.constructchip.hack.chip_not_installed"), true);
                return;
            }
            ServerPlayer h = host.get();
            construct.openMenu(new SimpleMenuProvider((id, inv, p) -> new HostInventoryMenu(id, h.getInventory(), h),
                    Component.translatable("container.constructchip.host_inventory", h.getGameProfile().getName())));
        });
    }

    static void unlockSkill(ServerboundPayloads.UnlockSkill payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            Optional<SharedChip> shared = SharedChip.of(player);
            if (shared.isEmpty()) return;
            SharedChip chip = shared.get();
            Registry<Skill> registry = ModSkills.get(player.server.registryAccess());
            ChipSkills skills = ChipStacks.skills(chip.chip());
            SkillTree.UnlockResult result = SkillTree.check(skills, payload.skill(), registry);
            if (result == SkillTree.UnlockResult.OK) {
                ChipStacks.setSkills(chip.chip(), SkillTree.unlock(skills, payload.skill(), registry));
                ChipStats.of(chip.chip(), registry).applyRam(chip.chip(), player.level().getGameTime());
                chip.save();
                ChipSync.syncHost(chip.host());
            }
            player.displayClientMessage(Component.translatable(result.translationKey(), payload.skill().toString()), true);
        });
    }

    static void buySkillPoint(ServerboundPayloads.BuySkillPoint payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            Optional<SharedChip> shared = SharedChip.of(player);
            if (shared.isEmpty() || !shared.get().viewerIsHost()) {
                player.displayClientMessage(Component.translatable("message.constructchip.skill.host_only"), true);
                return;
            }
            int count = Math.max(1, Math.min(10, payload.count()));
            SharedChip chip = shared.get();
            ChipSkills skills = ChipStacks.skills(chip.chip());
            long cost = SkillPointCost.costOf(skills.purchased(), count);
            if (!SkillPointCost.canAfford(player, cost)) {
                player.displayClientMessage(Component.translatable("commands.constructchip.skill.cannot_afford",
                        count, cost, SkillPointCost.totalXp(player)), true);
                return;
            }
            SkillPointCost.charge(player, cost);
            ChipStacks.setSkills(chip.chip(), skills.withPurchased(count));
            chip.save();
            ChipSync.syncHost(player);
        });
    }

    static void consentReply(ServerboundPayloads.ConsentReply payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) ConsentManager.reply(player, payload.requestId(), payload.accept());
        });
    }
}
