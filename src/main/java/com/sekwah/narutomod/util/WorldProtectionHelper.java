package com.sekwah.narutomod.util;

import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.gamerules.NarutoGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import javax.annotation.Nullable;

/**
 * Centralized world protection helper for all block-breaking, burning, or block-transforming
 * mechanics in the mod.
 */
public final class WorldProtectionHelper {

    private WorldProtectionHelper() {
    }

    /**
     * Checks whether a block at {@code pos} in {@code level} may be modified by {@code source}.
     *
     * <ol>
     *     <li>Ensures server-side execution and valid world bounds.</li>
     *     <li>Checks the {@code narutomod:naruto_jutsu_griefing} GameRule.</li>
     *     <li>Checks {@link NarutoConfig#jutsuWorldDamage} (and vanilla {@code mob_griefing} via {@link EventHooks#canEntityGrief}).</li>
     *     <li>If {@code source} is a {@link ServerPlayer}, tests {@link ServerLevel#mayInteract} and posts
     *     NeoForge's {@link BreakBlockEvent} so protection/claim mods can cancel the modification.</li>
     * </ol>
     *
     * @param level  Target level
     * @param pos    Target block position
     * @param source Entity causing the modification (nullable for environmental effects)
     * @return {@code true} if the block modification is permitted
     */
    public static boolean canModifyBlock(Level level, BlockPos pos, @Nullable Entity source) {
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.isInWorldBounds(pos)) {
            return false;
        }

        if (!isJutsuGriefingRuleEnabled(serverLevel)) {
            return false;
        }

        if (!NarutoConfig.jutsuWorldDamage) {
            return false;
        }

        if (source != null && !(source instanceof ServerPlayer) && !EventHooks.canEntityGrief(serverLevel, source)) {
            return false;
        }

        if (source instanceof ServerPlayer serverPlayer) {
            if (!serverLevel.mayInteract(serverPlayer, pos)) {
                return false;
            }
            BlockState currentState = serverLevel.getBlockState(pos);
            BreakBlockEvent event = new BreakBlockEvent(serverLevel, pos, currentState, serverPlayer);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Resolves the {@link Level.ExplosionInteraction} for an explosion at {@code pos},
     * honoring both {@code configEnabled} and {@link #canModifyBlock(Level, BlockPos, Entity)}.
     */
    public static Level.ExplosionInteraction getExplosionInteraction(Level level, BlockPos pos, @Nullable Entity source, boolean configEnabled) {
        if (!configEnabled) {
            return Level.ExplosionInteraction.NONE;
        }
        return canModifyBlock(level, pos, source) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
    }

    private static boolean isJutsuGriefingRuleEnabled(ServerLevel serverLevel) {
        return serverLevel.getGameRules().get(NarutoGameRules.NARUTO_JUTSU_GRIEFING.get());
    }
}
