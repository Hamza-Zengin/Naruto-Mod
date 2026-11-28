package com.sekwah.narutomod.util;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Manages temporary server-side {@link Blocks#LIGHT} blocks (level 15) placed by flash effects
 * and restores the exact original block state ({@link Blocks#AIR} or still {@link Blocks#WATER})
 * after expiration or on server shutdown.
 */
@EventBusSubscriber(modid = NarutoMod.MOD_ID)
public final class TemporaryLightManager {

    public static final int DEFAULT_DURATION_TICKS = 200;
    public static final int FLASH_LIGHT_LEVEL = 15;

    /**
     * Minimal world abstraction so placement, refresh, tick expiry, and shutdown cleanup
     * can be verified directly in JUnit tests without a running dedicated server.
     */
    public interface LightWorldAdapter {
        ResourceKey<Level> dimension();

        long getGameTime();

        boolean isInWorldBounds(BlockPos pos);

        BlockState getBlockState(BlockPos pos);

        @Nullable
        BlockEntity getBlockEntity(BlockPos pos);

        boolean setBlock(BlockPos pos, BlockState state);
    }

    public record LightKey(ResourceKey<Level> dimension, BlockPos pos) {
        public LightKey {
            pos = pos.immutable();
        }
    }

    public static final class LightEntry {
        private final BlockState originalState;
        private long expireGameTime;

        public LightEntry(BlockState originalState, long expireGameTime) {
            this.originalState = originalState;
            this.expireGameTime = expireGameTime;
        }

        public BlockState getOriginalState() {
            return this.originalState;
        }

        public long getExpireGameTime() {
            return this.expireGameTime;
        }
    }

    private static final Map<LightKey, LightEntry> ACTIVE_LIGHTS = new ConcurrentHashMap<>();

    private TemporaryLightManager() {
    }

    /**
     * Places a temporary level-15 light block using the default 200-tick (10-second) duration.
     */
    public static boolean placeTemporaryLight(@Nullable ServerLevel level, @Nullable BlockPos origin) {
        return placeTemporaryLight(level, origin, DEFAULT_DURATION_TICKS);
    }

    /**
     * Places a temporary level-15 light block at {@code origin} or the nearest valid air/still-water
     * neighbor, or refreshes an existing managed temporary light without losing its original state.
     */
    public static boolean placeTemporaryLight(@Nullable ServerLevel level, @Nullable BlockPos origin, int durationTicks) {
        if (level == null || level.isClientSide() || origin == null || durationTicks <= 0) {
            return false;
        }
        return placeTemporaryLight(adaptServerLevel(level), origin, durationTicks);
    }

    public static boolean placeTemporaryLight(@Nullable LightWorldAdapter world, @Nullable BlockPos origin, int durationTicks) {
        if (world == null || origin == null || durationTicks <= 0) {
            return false;
        }

        ResourceKey<Level> dimension = world.dimension();
        long newExpireGameTime = world.getGameTime() + durationTicks;

        for (BlockPos candidate : getCandidatePositions(origin)) {
            if (!world.isInWorldBounds(candidate)) {
                continue;
            }

            BlockState currentState = world.getBlockState(candidate);
            LightKey key = new LightKey(dimension, candidate);
            LightEntry existing = ACTIVE_LIGHTS.get(key);

            if (existing != null) {
                if (currentState.is(Blocks.LIGHT)) {
                    existing.expireGameTime = Math.max(existing.expireGameTime, newExpireGameTime);
                    if (currentState.getValue(LightBlock.LEVEL) != FLASH_LIGHT_LEVEL) {
                        world.setBlock(candidate, currentState.setValue(LightBlock.LEVEL, FLASH_LIGHT_LEVEL));
                    }
                    return true;
                }
                // External block change overwrote the temporary light; drop stale tracking.
                ACTIVE_LIGHTS.remove(key, existing);
            }

            if (!isReplaceableForLight(currentState) || world.getBlockEntity(candidate) != null) {
                continue;
            }

            BlockState lightState = createLightState(currentState, FLASH_LIGHT_LEVEL);
            if (world.setBlock(candidate, lightState)) {
                ACTIVE_LIGHTS.put(key, new LightEntry(currentState, newExpireGameTime));
                return true;
            }
        }

        return false;
    }

    /**
     * Returns candidate positions ordered as: origin, origin.above(), 6 cardinal neighbors,
     * and the remaining 3x3x3 neighborhood around origin.
     */
    public static List<BlockPos> getCandidatePositions(BlockPos origin) {
        BlockPos base = origin.immutable();
        List<BlockPos> candidates = new ArrayList<>(27);
        Set<BlockPos> seen = new HashSet<>(32);

        addCandidate(candidates, seen, base);
        addCandidate(candidates, seen, base.above());
        for (Direction dir : Direction.values()) {
            addCandidate(candidates, seen, base.relative(dir));
        }
        for (int dy : new int[]{1, 0, -1}) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    addCandidate(candidates, seen, base.offset(dx, dy, dz));
                }
            }
        }
        return candidates;
    }

    private static void addCandidate(List<BlockPos> candidates, Set<BlockPos> seen, BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (seen.add(immutable)) {
            candidates.add(immutable);
        }
    }

    /**
     * Only pure air or full still water source blocks without block entities may be replaced.
     * Prevents overwriting slabs, torches, chests, rails, doors, flowing water, or lava.
     */
    public static boolean isReplaceableForLight(@Nullable BlockState state) {
        if (state == null || state.hasBlockEntity()) {
            return false;
        }
        if (state.isAir()) {
            return true;
        }
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    /**
     * Builds a {@link Blocks#LIGHT} state at the requested level while preserving waterlogging
     * if the original block was still water.
     */
    public static BlockState createLightState(@Nullable BlockState originalState, int lightLevel) {
        int clampedLevel = Math.max(1, Math.min(15, lightLevel));
        boolean waterlogged = originalState != null && originalState.is(Blocks.WATER);
        return Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, clampedLevel)
                .setValue(LightBlock.WATERLOGGED, waterlogged);
    }

    public static boolean isExpired(long currentGameTime, long expireGameTime) {
        return currentGameTime >= expireGameTime;
    }

    public static void tickLevel(@Nullable ServerLevel level) {
        if (level == null || level.isClientSide() || ACTIVE_LIGHTS.isEmpty()) {
            return;
        }
        tickLevel(adaptServerLevel(level));
    }

    public static void tickLevel(@Nullable LightWorldAdapter world) {
        if (world == null || ACTIVE_LIGHTS.isEmpty()) {
            return;
        }

        long currentGameTime = world.getGameTime();
        ResourceKey<Level> dimension = world.dimension();

        Iterator<Map.Entry<LightKey, LightEntry>> it = ACTIVE_LIGHTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LightKey, LightEntry> mapEntry = it.next();
            LightKey key = mapEntry.getKey();
            if (!key.dimension().equals(dimension)) {
                continue;
            }
            LightEntry entry = mapEntry.getValue();
            if (isExpired(currentGameTime, entry.expireGameTime)) {
                restoreIfStillLight(world, key.pos(), entry.originalState);
                it.remove();
            }
        }
    }

    public static void clearAll(@Nullable MinecraftServer server) {
        clearAll(dimension -> {
            if (server == null) {
                return null;
            }
            ServerLevel level = server.getLevel(dimension);
            return level != null ? adaptServerLevel(level) : null;
        });
    }

    public static void clearAll(@Nullable Function<ResourceKey<Level>, LightWorldAdapter> levelResolver) {
        if (ACTIVE_LIGHTS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<LightKey, LightEntry>> it = ACTIVE_LIGHTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LightKey, LightEntry> mapEntry = it.next();
            LightKey key = mapEntry.getKey();
            LightEntry entry = mapEntry.getValue();
            if (levelResolver != null) {
                LightWorldAdapter world = levelResolver.apply(key.dimension());
                if (world != null) {
                    restoreIfStillLight(world, key.pos(), entry.originalState);
                }
            }
            it.remove();
        }
    }

    public static void clearAll() {
        ACTIVE_LIGHTS.clear();
    }

    public static int getActiveLightCount() {
        return ACTIVE_LIGHTS.size();
    }

    @Nullable
    public static LightEntry getActiveLight(ResourceKey<Level> dimension, BlockPos pos) {
        if (dimension == null || pos == null) {
            return null;
        }
        return ACTIVE_LIGHTS.get(new LightKey(dimension, pos));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event == null || ACTIVE_LIGHTS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }

        Set<ResourceKey<Level>> activeDimensions = new HashSet<>();
        for (LightKey key : ACTIVE_LIGHTS.keySet()) {
            activeDimensions.add(key.dimension());
        }
        for (ResourceKey<Level> dimension : activeDimensions) {
            ServerLevel level = server.getLevel(dimension);
            if (level != null) {
                tickLevel(level);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (event == null || ACTIVE_LIGHTS.isEmpty()) {
            return;
        }
        clearAll(event.getServer());
    }

    private static boolean restoreIfStillLight(LightWorldAdapter world, BlockPos pos, BlockState originalState) {
        if (!world.isInWorldBounds(pos)) {
            return false;
        }
        if (!world.getBlockState(pos).is(Blocks.LIGHT)) {
            return false;
        }
        return world.setBlock(pos, originalState);
    }

    private static LightWorldAdapter adaptServerLevel(ServerLevel level) {
        return new LightWorldAdapter() {
            @Override
            public ResourceKey<Level> dimension() {
                return level.dimension();
            }

            @Override
            public long getGameTime() {
                return level.getGameTime();
            }

            @Override
            public boolean isInWorldBounds(BlockPos pos) {
                return level.isInWorldBounds(pos);
            }

            @Override
            public BlockState getBlockState(BlockPos pos) {
                return level.getBlockState(pos);
            }

            @Override
            @Nullable
            public BlockEntity getBlockEntity(BlockPos pos) {
                return level.getBlockEntity(pos);
            }

            @Override
            public boolean setBlock(BlockPos pos, BlockState state) {
                return level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
        };
    }
}
