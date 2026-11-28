package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.entity.projectile.HiraishinKunaiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Server-side tracker for each player's latest active Flying Raijin (Hiraishin) Kunai marker.
 */
public final class HiraishinMarkerTracker {

    public static final long MARKER_DURATION_TICKS = 1200L; // 60 seconds
    public static final double MAX_TELEPORT_RANGE = 96.0D;

    private static final float PLAYER_HALF_WIDTH = 0.3F;
    private static final float PLAYER_HEIGHT = 1.8F;

    private static final int[][] SAFE_OFFSETS = {
            {0, 0, 0},
            {0, 1, 0},
            {0, -1, 0},
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
            {1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1},
            {0, -2, 0},
            {1, -2, 0}, {-1, -2, 0}, {0, -2, 1}, {0, -2, -1}
    };

    private static final Map<UUID, MarkerEntry> ACTIVE_MARKERS = new ConcurrentHashMap<>();

    private HiraishinMarkerTracker() {
    }

    public record MarkerEntry(UUID kunaiUuid, ResourceKey<Level> dimension, long expiryTick) {
        public boolean isExpired(long gameTime) {
            return gameTime > this.expiryTick;
        }
    }

    public enum MarkerStatus {
        ACTIVE,
        NO_MARKER,
        EXPIRED,
        WRONG_DIMENSION,
        OUT_OF_RANGE
    }

    public record MarkerResolution(MarkerStatus status, @Nullable HiraishinKunaiEntity kunai, double distance) {
        public boolean isUsable() {
            return this.status == MarkerStatus.ACTIVE && this.kunai != null;
        }
    }

    public static void registerMarker(Player owner, HiraishinKunaiEntity kunai, long gameTime) {
        if (owner == null || kunai == null || owner.level().isClientSide()) {
            return;
        }
        registerMarker(owner.getUUID(), kunai.getUUID(), kunai.level().dimension(), gameTime);
    }

    public static void registerMarker(UUID ownerUuid, UUID kunaiUuid, ResourceKey<Level> dimension, long gameTime) {
        if (ownerUuid == null || kunaiUuid == null || dimension == null) {
            return;
        }
        ACTIVE_MARKERS.put(ownerUuid, new MarkerEntry(kunaiUuid, dimension, gameTime + MARKER_DURATION_TICKS));
    }

    @Nullable
    public static MarkerEntry getMarkerEntry(UUID playerUuid) {
        return playerUuid == null ? null : ACTIVE_MARKERS.get(playerUuid);
    }

    public static boolean isKunaiMarked(HiraishinKunaiEntity kunai, long gameTime) {
        if (kunai == null || kunai.isRemoved() || !(kunai.getOwner() instanceof Player owner)) {
            return false;
        }
        MarkerEntry entry = ACTIVE_MARKERS.get(owner.getUUID());
        return entry != null
                && entry.kunaiUuid().equals(kunai.getUUID())
                && entry.dimension().equals(kunai.level().dimension())
                && !entry.isExpired(gameTime);
    }

    public static MarkerResolution resolveMarkerStatus(ServerPlayer player, long gameTime) {
        if (player == null) {
            return new MarkerResolution(MarkerStatus.NO_MARKER, null, Double.POSITIVE_INFINITY);
        }
        MarkerEntry entry = ACTIVE_MARKERS.get(player.getUUID());
        if (entry == null) {
            return new MarkerResolution(MarkerStatus.NO_MARKER, null, Double.POSITIVE_INFINITY);
        }
        if (entry.isExpired(gameTime)) {
            ACTIVE_MARKERS.remove(player.getUUID(), entry);
            return new MarkerResolution(MarkerStatus.EXPIRED, null, Double.POSITIVE_INFINITY);
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return new MarkerResolution(MarkerStatus.NO_MARKER, null, Double.POSITIVE_INFINITY);
        }
        if (!entry.dimension().equals(serverLevel.dimension())) {
            return new MarkerResolution(MarkerStatus.WRONG_DIMENSION, null, Double.POSITIVE_INFINITY);
        }
        Entity entity = serverLevel.getEntity(entry.kunaiUuid());
        if (!(entity instanceof HiraishinKunaiEntity kunai) || !kunai.isAlive() || kunai.isRemoved()) {
            ACTIVE_MARKERS.remove(player.getUUID(), entry);
            return new MarkerResolution(MarkerStatus.NO_MARKER, null, Double.POSITIVE_INFINITY);
        }
        double distance = kunai.position().distanceTo(player.position());
        if (distance > MAX_TELEPORT_RANGE) {
            return new MarkerResolution(MarkerStatus.OUT_OF_RANGE, kunai, distance);
        }
        return new MarkerResolution(MarkerStatus.ACTIVE, kunai, distance);
    }

    @Nullable
    public static HiraishinKunaiEntity resolveActiveMarker(ServerPlayer player, long gameTime) {
        MarkerResolution resolution = resolveMarkerStatus(player, gameTime);
        return resolution.isUsable() ? resolution.kunai() : null;
    }

    /**
     * Pure validation helper for marker eligibility checks.
     */
    public static MarkerStatus evaluateMarker(
            @Nullable MarkerEntry entry,
            ResourceKey<Level> playerDimension,
            Vec3 playerPos,
            @Nullable Vec3 kunaiPos,
            boolean kunaiAlive,
            long gameTime
    ) {
        if (entry == null) {
            return MarkerStatus.NO_MARKER;
        }
        if (entry.isExpired(gameTime)) {
            return MarkerStatus.EXPIRED;
        }
        if (playerDimension == null || !entry.dimension().equals(playerDimension)) {
            return MarkerStatus.WRONG_DIMENSION;
        }
        if (!kunaiAlive || kunaiPos == null || playerPos == null) {
            return MarkerStatus.NO_MARKER;
        }
        if (kunaiPos.distanceTo(playerPos) > MAX_TELEPORT_RANGE) {
            return MarkerStatus.OUT_OF_RANGE;
        }
        return MarkerStatus.ACTIVE;
    }

    public static void clearMarker(UUID playerUuid) {
        if (playerUuid != null) {
            ACTIVE_MARKERS.remove(playerUuid);
        }
    }

    public static void clearMarkerIfMatches(UUID playerUuid, UUID kunaiUuid) {
        if (playerUuid == null || kunaiUuid == null) {
            return;
        }
        ACTIVE_MARKERS.computeIfPresent(playerUuid, (id, entry) ->
                entry.kunaiUuid().equals(kunaiUuid) ? null : entry);
    }

    public static void clearAll() {
        ACTIVE_MARKERS.clear();
    }

    /**
     * Finds a safe 2-block-tall teleport position near targetPos so the player never suffocates inside solid blocks.
     */
    @Nullable
    public static Vec3 findSafeTeleportPos(Level level, Vec3 targetPos) {
        if (level == null || targetPos == null) {
            return null;
        }
        BlockPos anchor = BlockPos.containing(targetPos.x, targetPos.y, targetPos.z);
        BlockPos safePos = findSafeTeleportBlockPos(pos -> isSafeTeleportBlock(level, pos), anchor);
        if (safePos == null) {
            return null;
        }
        return new Vec3(safePos.getX() + 0.5D, safePos.getY(), safePos.getZ() + 0.5D);
    }

    /**
     * Pure-coordinate helper checking 2-block headroom at anchor and nearby 1-block offsets.
     */
    @Nullable
    public static BlockPos findSafeTeleportBlockPos(Predicate<BlockPos> feetSafetyCheck, BlockPos anchor) {
        if (feetSafetyCheck == null || anchor == null) {
            return null;
        }
        for (int[] offset : SAFE_OFFSETS) {
            BlockPos candidate = anchor.offset(offset[0], offset[1], offset[2]);
            if (feetSafetyCheck.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static boolean isSafeTeleportBlock(Level level, BlockPos feetPos) {
        if (level == null || feetPos == null) {
            return false;
        }
        BlockPos headPos = feetPos.above();
        if (!level.isInWorldBounds(feetPos) || !level.isInWorldBounds(headPos)) {
            return false;
        }
        if (!level.getBlockState(feetPos).getCollisionShape(level, feetPos).isEmpty()) {
            return false;
        }
        if (!level.getBlockState(headPos).getCollisionShape(level, headPos).isEmpty()) {
            return false;
        }
        if (level.getFluidState(feetPos).is(FluidTags.LAVA) || level.getFluidState(headPos).is(FluidTags.LAVA)) {
            return false;
        }
        double cx = feetPos.getX() + 0.5D;
        double cy = feetPos.getY();
        double cz = feetPos.getZ() + 0.5D;
        AABB playerBox = new AABB(
                cx - PLAYER_HALF_WIDTH, cy, cz - PLAYER_HALF_WIDTH,
                cx + PLAYER_HALF_WIDTH, cy + PLAYER_HEIGHT, cz + PLAYER_HALF_WIDTH
        );
        return level.noCollision(playerBox);
    }
}
