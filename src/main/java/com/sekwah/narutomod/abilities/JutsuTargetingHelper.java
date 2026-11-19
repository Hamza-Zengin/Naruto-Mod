package com.sekwah.narutomod.abilities;

import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shared raycast and ground/enemy target resolution utilities for area-of-effect jutsus.
 */
public final class JutsuTargetingHelper {

    private JutsuTargetingHelper() {}

    /**
     * Resolves an eruption or trap origin point along the caster's aim ray:
     * 1. Clips blocks up to {@code maxRange}.
     * 2. Snaps onto the closest hostile {@link LivingEntity} within {@code snapRadius} of the aim anchor.
     * 3. Otherwise scans downward up to 6 blocks for solid ground below the anchor.
     */
    public static Vec3 resolveTargetOrGroundOrigin(ServerLevel level, Player player, double maxRange, double snapRadius) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxRange));

        HitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 anchor = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : eye.add(look.scale(maxRange * 0.55D));

        AABB rayBox = new AABB(eye, anchor).inflate(snapRadius);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, rayBox,
                e -> e.isAlive() && e != player && !ShadowCloneEntity.areAllies(player, e));
        LivingEntity bestTarget = null;
        double bestDistSq = Double.MAX_VALUE;
        for (LivingEntity c : candidates) {
            double dSq = c.position().distanceToSqr(anchor);
            if (dSq < bestDistSq) {
                bestDistSq = dSq;
                bestTarget = c;
            }
        }
        if (bestTarget != null) {
            return bestTarget.position();
        }

        BlockPos pos = BlockPos.containing(anchor.x, anchor.y, anchor.z);
        for (int dy = 0; dy <= 6; dy++) {
            BlockPos check = pos.below(dy);
            if (!level.getBlockState(check).isAir()) {
                return new Vec3(anchor.x, check.getY() + 1.0D, anchor.z);
            }
        }
        return new Vec3(anchor.x, player.getY(), anchor.z);
    }
}
