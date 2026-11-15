package io.github.constructchip.compat.securitycraft;

import io.github.constructchip.hack.HackResult;
import net.geforcemods.securitycraft.api.ICodebreakable;
import net.geforcemods.securitycraft.api.IEMPAffected;
import net.geforcemods.securitycraft.api.IExplosive;
import net.geforcemods.securitycraft.api.IPasscodeProtected;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The calls into SecurityCraft's public API ({@code net.geforcemods.securitycraft.api}). Only ever loaded through
 * {@link SecurityCraftCompat}, which checks that SecurityCraft is installed first.
 */
final class SecurityCraftHooks {
    private SecurityCraftHooks() {}

    /** Short Circuit / System Lock on a SecurityCraft machine entity (the Sentry): shut it down for a while. */
    static boolean shutDownEntity(Entity e, int ticks) {
        if (!(e instanceof IEMPAffected emp)) return false;
        if (!emp.isShutDown()) emp.shutDown();
        ServerLevel level = (ServerLevel) e.level();
        int id = e.getId();
        SecurityCraftCompat.later(level, ticks, () -> {
            if (level.getEntity(id) instanceof IEMPAffected again && again.isShutDown()) again.reactivate();
        });
        return true;
    }

    /**
     * Remote Switch on a SecurityCraft device. Returns null if the block isn't one (the normal Remote Switch handles
     * it), otherwise the hack's result:
     * cameras and Sonic Security Systems shut down for {@code shutdownTicks} (or come back on if already down),
     * mines are defused (or armed again), and passcode / keycard devices are cracked as if by a Codebreaker,
     * acting as the host (doors open, keypads pulse, safes open for the host).
     */
    static HackResult remoteSwitch(ServerLevel level, BlockPos pos, ServerPlayer host, int shutdownTicks) {
        BlockEntity be = level.getBlockEntity(pos);
        Block block = level.getBlockState(pos).getBlock();
        Component name = block.getName();

        if (be instanceof IEMPAffected emp) {
            if (emp.isShutDown()) {
                emp.reactivate();
                return HackResult.success(Component.translatable("message.constructchip.securitycraft.rebooted", name));
            }
            emp.shutDown();
            SecurityCraftCompat.later(level, shutdownTicks, () -> {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof IEMPAffected again && again.isShutDown()) again.reactivate();
            });
            return HackResult.success(Component.translatable("message.constructchip.securitycraft.shut_down", name, shutdownTicks / 20));
        }

        if (block instanceof IExplosive mine) {
            if (mine.isActive(level, pos)) {
                if (!mine.isDefusable()) return HackResult.fail(Component.translatable("message.constructchip.securitycraft.not_defusable", name));
                return mine.defuseMine(level, pos)
                        ? HackResult.success(Component.translatable("message.constructchip.securitycraft.defused", name))
                        : HackResult.fail(Component.translatable("message.constructchip.securitycraft.failed", name));
            }
            return mine.activateMine(level, pos)
                    ? HackResult.success(Component.translatable("message.constructchip.securitycraft.armed", name))
                    : HackResult.fail(Component.translatable("message.constructchip.securitycraft.failed", name));
        }

        if (be instanceof ICodebreakable lock) {
            if (be instanceof IPasscodeProtected passcode && passcode.isOnCooldown()) {
                return HackResult.fail(Component.translatable("message.constructchip.device.busy"));
            }
            if (!lock.shouldAttemptCodebreak(host)) { // e.g. no passcode set yet (SecurityCraft tells the host why)
                return HackResult.fail(Component.translatable("message.constructchip.securitycraft.failed", name));
            }
            lock.useCodebreaker(host);
            return HackResult.success(Component.translatable("message.constructchip.securitycraft.cracked", name));
        }
        return null;
    }
}
