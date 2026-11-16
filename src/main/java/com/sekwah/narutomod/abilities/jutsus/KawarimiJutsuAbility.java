package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * Kawarimi (Substitution Technique)
 * Silently swaps the caster with a short-lived decoy clone and teleports to a guaranteed
 * safe open space without particles, sounds, or wooden logs.
 */
public class KawarimiJutsuAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public com.sekwah.narutomod.capabilities.NinjaRank getRequiredRank() {
        return com.sekwah.narutomod.capabilities.NinjaRank.ACADEMY_STUDENT;
    }

    @Override
    public long defaultCombo() {
        return 12L;
    }

    @Override
    public float getDefaultChakraCost() {
        return 20.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return NarutoConfig.kawarimiMaxCooldownTicks;
    }

    public static double getScaledTeleportDistance(int mastery) {
        double t = Mth.clamp(mastery / 100.0, 0.0, 1.0);
        return Mth.lerp(t, NarutoConfig.kawarimiMaxDistance, NarutoConfig.kawarimiMinDistance);
    }

    public static int getScaledCooldownTicks(int mastery) {
        float t = Mth.clamp(mastery / 100.0f, 0.0f, 1.0f);
        return Math.round(Mth.lerp(t, NarutoConfig.kawarimiMaxCooldownTicks, NarutoConfig.kawarimiMinCooldownTicks));
    }

    @Override
    public int getScaledCooldown(int baseCooldown, int mastery) {
        return Math.max(5, getScaledCooldownTicks(mastery));
    }

    @Override
    public int getScaledActivationDelay(int mastery) {
        float t = Mth.clamp(mastery / 100.0f, 0.0f, 1.0f);
        return Math.max(0, Math.round(Mth.lerp(t, NarutoConfig.kawarimiMaxCastTicks, NarutoConfig.kawarimiMinCastTicks)));
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        return tryConsume(player, ninjaData, getScaledChakraCost(ninjaData), 0, 20);
    }

    /**
     * Verifies all 4 safety conditions for a candidate teleport position:
     * 1. Feet and head blocks have empty collision shapes and entity AABB has no collision.
     * 2. No fluid (water/lava) at feet or head.
     * 3. Solid ground exists within 1..3 blocks below.
     * 4. Zero wall suffocation risk.
     */
    public static boolean isSafeOpenSpace(Level level, Entity entity, BlockPos pos) {
        if (level == null || entity == null || pos == null) return false;
        BlockPos headPos = pos.above();
        BlockState feetState = level.getBlockState(pos);
        BlockState headState = level.getBlockState(headPos);

        if (!feetState.getCollisionShape(level, pos).isEmpty()
                || !headState.getCollisionShape(level, headPos).isEmpty()) {
            return false;
        }
        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(headPos).isEmpty()) {
            return false;
        }

        // Verify solid floor within 1..3 blocks below
        boolean hasFloor = false;
        for (int dy = 1; dy <= 3; dy++) {
            BlockPos below = pos.below(dy);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                hasFloor = true;
                break;
            }
        }
        if (!hasFloor) {
            return false;
        }

        // Check entity AABB collision at target center
        double cx = pos.getX() + 0.5;
        double cy = pos.getY();
        double cz = pos.getZ() + 0.5;
        float halfW = entity.getBbWidth() * 0.5f;
        AABB targetBox = new AABB(
                cx - halfW, cy, cz - halfW,
                cx + halfW, cy + entity.getBbHeight(), cz + halfW
        );
        return level.noCollision(entity, targetBox);
    }

    /**
     * Searches in an expanding horizontal & vertical spiral around targetCenter for a safe open space.
     * Returns null if no safe open space is found within maxRadius.
     */
    @Nullable
    public static Vec3 findSafeOpenPosition(Level level, Entity entity, Vec3 targetCenter, int maxRadius) {
        BlockPos anchor = BlockPos.containing(targetCenter.x, targetCenter.y, targetCenter.z);
        BlockPos found = findSafeOpenBlockPos(
                pos -> isSafeOpenSpace(level, entity, pos),
                anchor,
                maxRadius
        );
        if (found == null) return null;

        // Align directly onto the solid surface below found (found itself is open, check if below(1) is solid or drop up to 2 blocks)
        int surfaceY = found.getY();
        for (int dy = 1; dy <= 3; dy++) {
            BlockPos below = found.below(dy);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                surfaceY = below.getY() + 1;
                break;
            }
        }
        return new Vec3(found.getX() + 0.5, surfaceY, found.getZ() + 0.5);
    }

    /**
     * Pure-coordinate spiral search helper (also unit-tested directly without requiring a live Level).
     */
    @Nullable
    public static BlockPos findSafeOpenBlockPos(Predicate<BlockPos> safetyCheck, BlockPos anchor, int maxRadius) {
        if (safetyCheck.test(anchor)) {
            return anchor;
        }
        int[] dyOrder = {0, 1, -1, 2, -2};
        for (int r = 1; r <= maxRadius; r++) {
            for (int dy : dyOrder) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                        BlockPos candidate = anchor.offset(dx, dy, dz);
                        if (safetyCheck.test(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getAbilityMastery(this) : 0;
        double distance = getScaledTeleportDistance(mastery);

        Vec3 oldPos = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 horizLook = new Vec3(look.x, 0, look.z);
        if (horizLook.lengthSqr() < 1e-4) {
            horizLook = new Vec3(0, 0, 1);
        } else {
            horizLook = horizLook.normalize();
        }

        // Escape backwards along horizontal facing
        Vec3 candidateCenter = oldPos.subtract(horizLook.scale(distance));
        Vec3 safePos = findSafeOpenPosition(serverLevel, player, candidateCenter, 6);

        if (safePos == null) {
            // Cancel teleport and refund chakra so player is NEVER placed inside a wall
            if (ninjaData != null) {
                ninjaData.addChakra(getScaledChakraCost(ninjaData));
            }
            player.sendOverlayMessage(Component.translatable("jutsu.fail.notonground",
                    Component.translatable(this.getTranslationKey())).withStyle(ChatFormatting.GRAY));
            return;
        }

        // Spawn silent decoy ShadowCloneEntity at oldPos before teleporting
        ShadowCloneEntity decoy = NarutoEntities.SHADOW_CLONE.get().create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (decoy != null) {
            decoy.snapTo(oldPos.x, oldPos.y, oldPos.z, player.getYRot(), player.getXRot());
            decoy.yHeadRot = player.yHeadRot;
            decoy.yBodyRot = player.yBodyRot;
            decoy.setOwner(player);
            decoy.setMaxLifespan(40); // 2 seconds decoy
            decoy.setDecoyMode(true);
            serverLevel.addFreshEntity(decoy);

            // Redirect nearby hostile mobs targeting the player onto the decoy
            AABB aggroBox = player.getBoundingBox().inflate(20.0D);
            List<Mob> nearbyMobs = serverLevel.getEntitiesOfClass(Mob.class, aggroBox,
                    m -> m.isAlive() && m.getTarget() == player);
            for (Mob mob : nearbyMobs) {
                mob.setTarget(decoy);
            }
        }

        // Silent teleport with zero particles and zero sound
        player.teleportTo(safePos.x, safePos.y, safePos.z);
        player.resetFallDistance();
    }
}
