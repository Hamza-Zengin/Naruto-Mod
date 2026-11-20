package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.VacuumSphereEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages temporary server-side dynamic light blocks ({@link Blocks#LIGHT}) for illuminated jutsus
 * and active hand jutsus. Performs zero work per server tick when no lights are active.
 */
@EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class JutsuLightManager {

    public static final int LIGHT_NONE = 0;
    public static final int LIGHT_CHAKRA = 6;
    public static final int LIGHT_FIRE_LIGHTNING = 12;

    public static final int MIN_MOVE_MANHATTAN = 2;
    public static final int MIN_MOVE_INTERVAL_TICKS = 3;
    public static final int DEFAULT_TTL_TICKS = 4;

    static final class LightEntry {
        final UUID sourceId;
        ResourceKey<Level> dimension;
        BlockPos pos;
        int lightLevel;
        long lastMoveTick;
        long expiresAtTick;

        LightEntry(UUID sourceId, ResourceKey<Level> dimension, BlockPos pos, int lightLevel,
                   long lastMoveTick, long expiresAtTick) {
            this.sourceId = sourceId;
            this.dimension = dimension;
            this.pos = pos;
            this.lightLevel = lightLevel;
            this.lastMoveTick = lastMoveTick;
            this.expiresAtTick = expiresAtTick;
        }
    }

    static final Map<UUID, LightEntry> ACTIVE_LIGHTS = new ConcurrentHashMap<>();

    public static int resolveLightLevelForVisualType(int visualType) {
        return switch (visualType) {
            case JutsuVisualEffectEntity.TYPE_AMATERASU_PILLAR,
                 JutsuVisualEffectEntity.TYPE_ASH_CLOUD,
                 JutsuVisualEffectEntity.TYPE_KIRIN_DRAGON,
                 JutsuVisualEffectEntity.TYPE_CHIDORI_BURST -> LIGHT_FIRE_LIGHTNING;
            case JutsuVisualEffectEntity.TYPE_RASENGAN_SPHERE,
                 JutsuVisualEffectEntity.TYPE_WATER_VORTEX,
                 JutsuVisualEffectEntity.TYPE_WIND_SHOCKWAVE,
                 JutsuVisualEffectEntity.TYPE_ADAMANTINE_CHAINS,
                 JutsuVisualEffectEntity.TYPE_WATER_WALL -> LIGHT_CHAKRA;
            case JutsuVisualEffectEntity.TYPE_WOOD_PRISON,
                 JutsuVisualEffectEntity.TYPE_BONE_FOREST,
                 JutsuVisualEffectEntity.TYPE_SHADOW_BIND,
                 JutsuVisualEffectEntity.TYPE_EARTH_WALL,
                 JutsuVisualEffectEntity.TYPE_STONE_SPIKES -> LIGHT_NONE;
            default -> LIGHT_NONE;
        };
    }

    public static int resolveLightLevelForHandJutsu(@Nullable String handJutsu) {
        if (handJutsu == null || handJutsu.isEmpty()) {
            return LIGHT_NONE;
        }
        return switch (handJutsu.toLowerCase(Locale.ROOT)) {
            case "chidori" -> LIGHT_FIRE_LIGHTNING;
            case "rasengan", "chakra_scalpel" -> LIGHT_CHAKRA;
            default -> LIGHT_NONE;
        };
    }

    public static int resolveLightLevelForEntityClass(@Nullable Class<?> entityClass) {
        if (entityClass == null) {
            return LIGHT_NONE;
        }
        if (FireballJutsuEntity.class.isAssignableFrom(entityClass)) {
            return LIGHT_FIRE_LIGHTNING;
        }
        if (RasenshurikenEntity.class.isAssignableFrom(entityClass)
                || WaterBulletEntity.class.isAssignableFrom(entityClass)
                || VacuumSphereEntity.class.isAssignableFrom(entityClass)) {
            return LIGHT_CHAKRA;
        }
        return LIGHT_NONE;
    }

    public static int resolveLightLevelForEntity(@Nullable Entity entity) {
        if (entity == null) {
            return LIGHT_NONE;
        }
        if (entity instanceof JutsuVisualEffectEntity visual) {
            return resolveLightLevelForVisualType(visual.getVisualType());
        }
        if (entity instanceof ShadowCloneEntity clone) {
            return resolveLightLevelForHandJutsu(clone.getActiveHandJutsu());
        }
        return resolveLightLevelForEntityClass(entity.getClass());
    }

    public static boolean shouldMoveLight(@Nullable BlockPos oldPos, @Nullable BlockPos newPos, int ticksSinceMove) {
        if (newPos == null) {
            return false;
        }
        if (oldPos == null) {
            return true;
        }
        if (oldPos.equals(newPos)) {
            return false;
        }
        return oldPos.distManhattan(newPos) >= MIN_MOVE_MANHATTAN || ticksSinceMove >= MIN_MOVE_INTERVAL_TICKS;
    }

    public static boolean isEntryExpired(long currentTick, long expiresAtTick) {
        return currentTick >= expiresAtTick;
    }

    public static int getActiveLightCount() {
        return ACTIVE_LIGHTS.size();
    }

    public static boolean hasActiveLights() {
        return !ACTIVE_LIGHTS.isEmpty();
    }

    public static void clearAllTrackingStateForTest() {
        ACTIVE_LIGHTS.clear();
    }

    static void trackForTest(UUID sourceId, ResourceKey<Level> dimension, BlockPos pos, int lightLevel,
                             long lastMoveTick, long expiresAtTick) {
        ACTIVE_LIGHTS.put(sourceId, new LightEntry(sourceId, dimension, pos, lightLevel, lastMoveTick, expiresAtTick));
    }

    /**
     * Updates or places a dynamic light for a live server-side entity.
     */
    public static void updateLight(@Nullable ServerLevel serverLevel, @Nullable Entity source,
                                   @Nullable BlockPos targetPos, int lightLevel) {
        updateLight(serverLevel, source, targetPos, lightLevel, DEFAULT_TTL_TICKS);
    }

    /**
     * Updates or places a dynamic light for a live server-side entity with a custom TTL (in server ticks).
     */
    public static void updateLight(@Nullable ServerLevel serverLevel, @Nullable Entity source,
                                   @Nullable BlockPos targetPos, int lightLevel, int ttlTicks) {
        if (serverLevel == null || serverLevel.isClientSide() || source == null || targetPos == null) {
            return;
        }
        if (!source.isAlive() || lightLevel <= LIGHT_NONE) {
            removeLight(serverLevel, source.getUUID());
            return;
        }

        int clampedLevel = Math.max(1, Math.min(15, lightLevel));
        MinecraftServer server = serverLevel.getServer();
        long currentTick = server != null ? server.getTickCount() : serverLevel.getGameTime();
        long expiresAtTick = currentTick + Math.max(2, ttlTicks);
        BlockPos immutablePos = targetPos.immutable();
        ResourceKey<Level> dimension = serverLevel.dimension();
        UUID sourceId = source.getUUID();

        LightEntry existing = ACTIVE_LIGHTS.get(sourceId);
        if (existing == null) {
            if (placeLightBlock(serverLevel, immutablePos, clampedLevel)) {
                ACTIVE_LIGHTS.put(sourceId, new LightEntry(
                        sourceId, dimension, immutablePos, clampedLevel, currentTick, expiresAtTick));
            }
            return;
        }

        existing.expiresAtTick = expiresAtTick;

        boolean dimensionChanged = !existing.dimension.equals(dimension);
        int ticksSinceMove = (int) Math.max(0L, currentTick - existing.lastMoveTick);
        boolean shouldMove = dimensionChanged || shouldMoveLight(existing.pos, immutablePos, ticksSinceMove);
        boolean levelChanged = existing.lightLevel != clampedLevel;

        if (shouldMove) {
            removeLightAt(server, existing.dimension, existing.pos, serverLevel);
            if (placeLightBlock(serverLevel, immutablePos, clampedLevel)) {
                existing.dimension = dimension;
                existing.pos = immutablePos;
                existing.lightLevel = clampedLevel;
                existing.lastMoveTick = currentTick;
            } else {
                ACTIVE_LIGHTS.remove(sourceId);
            }
        } else if (levelChanged) {
            if (updateLightBlockLevel(serverLevel, existing.pos, clampedLevel)) {
                existing.lightLevel = clampedLevel;
            }
        }
    }

    /**
     * Removes any tracked light associated with the given source entity UUID.
     */
    public static void removeLight(@Nullable MinecraftServer server, @Nullable UUID sourceId) {
        if (sourceId == null || ACTIVE_LIGHTS.isEmpty()) {
            return;
        }
        LightEntry removed = ACTIVE_LIGHTS.remove(sourceId);
        if (removed != null && server != null) {
            removeLightAt(server, removed.dimension, removed.pos, null);
        }
    }

    /**
     * Removes any tracked light for a source entity using its ServerLevel.
     */
    public static void removeLight(@Nullable ServerLevel serverLevel, @Nullable UUID sourceId) {
        if (sourceId == null || ACTIVE_LIGHTS.isEmpty()) {
            return;
        }
        LightEntry removed = ACTIVE_LIGHTS.remove(sourceId);
        if (removed != null) {
            MinecraftServer server = serverLevel != null ? serverLevel.getServer() : null;
            removeLightAt(server, removed.dimension, removed.pos, serverLevel);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE_LIGHTS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        long currentTick = server.getTickCount();

        Iterator<Map.Entry<UUID, LightEntry>> it = ACTIVE_LIGHTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, LightEntry> mapEntry = it.next();
            LightEntry entry = mapEntry.getValue();
            ServerLevel level = server.getLevel(entry.dimension);
            boolean expired = isEntryExpired(currentTick, entry.expiresAtTick);
            boolean sourceMissingOrDead = level == null;

            if (!expired && level != null) {
                Entity source = level.getEntity(entry.sourceId);
                if (source == null || !source.isAlive()) {
                    sourceMissingOrDead = true;
                }
            }

            if (expired || sourceMissingOrDead) {
                if (level != null) {
                    clearLightBlock(level, entry.pos);
                }
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (ACTIVE_LIGHTS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (LightEntry entry : ACTIVE_LIGHTS.values()) {
            removeLightAt(server, entry.dimension, entry.pos, null);
        }
        ACTIVE_LIGHTS.clear();
    }

    private static boolean placeLightBlock(ServerLevel level, BlockPos pos, int lightLevel) {
        if (!level.isInWorldBounds(pos)) {
            return false;
        }
        BlockState current = level.getBlockState(pos);
        if (!current.isAir()) {
            return false;
        }
        BlockState lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel);
        return level.setBlock(pos, lightState, Block.UPDATE_CLIENTS);
    }

    private static boolean updateLightBlockLevel(ServerLevel level, BlockPos pos, int lightLevel) {
        if (!level.isInWorldBounds(pos)) {
            return false;
        }
        BlockState current = level.getBlockState(pos);
        if (!current.is(Blocks.LIGHT)) {
            return false;
        }
        BlockState lightState = current.setValue(LightBlock.LEVEL, lightLevel);
        return level.setBlock(pos, lightState, Block.UPDATE_CLIENTS);
    }

    private static void removeLightAt(@Nullable MinecraftServer server, ResourceKey<Level> dimension,
                                      BlockPos pos, @Nullable ServerLevel fallbackLevel) {
        ServerLevel targetLevel = server != null ? server.getLevel(dimension) : null;
        if (targetLevel == null && fallbackLevel != null && fallbackLevel.dimension().equals(dimension)) {
            targetLevel = fallbackLevel;
        }
        if (targetLevel != null) {
            clearLightBlock(targetLevel, pos);
        }
    }

    private static void clearLightBlock(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos)) {
            return;
        }
        if (level.getBlockState(pos).is(Blocks.LIGHT)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
