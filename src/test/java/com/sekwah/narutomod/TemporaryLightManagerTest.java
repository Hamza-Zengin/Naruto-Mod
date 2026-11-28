package com.sekwah.narutomod;

import com.sekwah.narutomod.entity.projectile.FlashBombEntity;
import com.sekwah.narutomod.util.TemporaryLightManager;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TemporaryLightManager} and {@link FlashBombEntity} contracts.
 */
public class TemporaryLightManagerTest {

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void resetLights() {
        TemporaryLightManager.clearAll();
    }

    private static final class FakeLightWorld implements TemporaryLightManager.LightWorldAdapter {
        private final ResourceKey<Level> dimension;
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();
        private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
        private long gameTime = 0L;

        FakeLightWorld(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }

        void setGameTime(long gameTime) {
            this.gameTime = gameTime;
        }

        void putBlock(BlockPos pos, BlockState state) {
            this.blocks.put(pos.immutable(), state);
        }

        @Override
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        @Override
        public long getGameTime() {
            return this.gameTime;
        }

        @Override
        public boolean isInWorldBounds(BlockPos pos) {
            return pos.getY() >= -64 && pos.getY() < 320;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return this.blocks.getOrDefault(pos.immutable(), Blocks.AIR.defaultBlockState());
        }

        @Override
        @Nullable
        public BlockEntity getBlockEntity(BlockPos pos) {
            return this.blockEntities.get(pos.immutable());
        }

        @Override
        public boolean setBlock(BlockPos pos, BlockState state) {
            this.blocks.put(pos.immutable(), state);
            return true;
        }
    }

    @Test
    @DisplayName("FlashBombEntity exposes updated radius/duration constants, class contracts, and facing-aware flash calculation")
    void testFlashBombConstantsAndPlayerFlashCalculation() throws IOException {
        assertEquals(ThrowableItemProjectile.class, FlashBombEntity.class.getSuperclass());
        assertEquals(7.5D, FlashBombEntity.FLASH_RADIUS, 1.0E-6D);
        assertEquals(3.0D, FlashBombEntity.SELF_FLASH_RADIUS, 1.0E-6D);
        assertEquals(200, FlashBombEntity.FLASH_LIGHT_DURATION_TICKS);

        Vec3 explosion = new Vec3(10.0D, 64.0D, 10.0D);
        Vec3 eyeLookingAtFlash = new Vec3(4.0D, 64.0D, 10.0D);
        Vec3 lookForward = new Vec3(1.0D, 0.0D, 0.0D);
        FlashBombEntity.PlayerFlashParams directFlash = FlashBombEntity.computePlayerFlash(
                eyeLookingAtFlash, lookForward, explosion, 6.0D);
        assertTrue(directFlash.strongFlash());
        assertEquals(90, directFlash.durationTicks());
        assertTrue(directFlash.intensity() >= 0.85F && directFlash.intensity() <= 1.0F);

        Vec3 lookAway = new Vec3(-1.0D, 0.0D, 0.0D);
        FlashBombEntity.PlayerFlashParams awayFarFlash = FlashBombEntity.computePlayerFlash(
                eyeLookingAtFlash, lookAway, explosion, 6.0D);
        assertFalse(awayFarFlash.strongFlash());
        assertEquals(65, awayFarFlash.durationTicks());
        assertTrue(awayFarFlash.intensity() >= 0.55F && awayFarFlash.intensity() <= 0.80F);

        Vec3 eyeVeryClose = new Vec3(8.0D, 64.0D, 10.0D);
        FlashBombEntity.PlayerFlashParams awayCloseFlash = FlashBombEntity.computePlayerFlash(
                eyeVeryClose, lookAway, explosion, 2.0D);
        assertTrue(awayCloseFlash.strongFlash(), "Very close targets (< 3.5 blocks) must still receive strong flash");
        assertEquals(90, awayCloseFlash.durationTicks());
        assertTrue(awayCloseFlash.intensity() >= 0.85F && awayCloseFlash.intensity() <= 1.0F);

        String src = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/entity/projectile/FlashBombEntity.java"));
        assertTrue(src.contains("TemporaryLightManager.placeTemporaryLight"));
        assertTrue(src.contains("MobEffects.BLINDNESS"));
        assertTrue(src.contains("MobEffects.SLOWNESS, 80, 6"));
        assertTrue(src.contains("MobEffects.WEAKNESS, 100, 1"));
        assertTrue(src.contains("MobEffects.GLOWING, 140, 0"));
        assertTrue(src.contains("mob.needsSync = true"));
        assertTrue(src.contains("0.40F, 20"));
    }

    @Test
    @DisplayName("Only pure air and full still water source blocks are replaceable for temporary light")
    void testReplaceabilityAndLightStateCreation() {
        assertTrue(TemporaryLightManager.isReplaceableForLight(Blocks.AIR.defaultBlockState()));
        assertTrue(TemporaryLightManager.isReplaceableForLight(Blocks.CAVE_AIR.defaultBlockState()));
        assertTrue(TemporaryLightManager.isReplaceableForLight(Blocks.WATER.defaultBlockState()));

        BlockState flowingWater = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3);
        assertFalse(TemporaryLightManager.isReplaceableForLight(flowingWater));
        assertFalse(TemporaryLightManager.isReplaceableForLight(Blocks.LAVA.defaultBlockState()));
        assertFalse(TemporaryLightManager.isReplaceableForLight(Blocks.STONE.defaultBlockState()));
        assertFalse(TemporaryLightManager.isReplaceableForLight(Blocks.OAK_SLAB.defaultBlockState()));
        assertFalse(TemporaryLightManager.isReplaceableForLight(Blocks.TORCH.defaultBlockState()));
        assertFalse(TemporaryLightManager.isReplaceableForLight(Blocks.CHEST.defaultBlockState()));
        assertFalse(TemporaryLightManager.isReplaceableForLight(Blocks.RAIL.defaultBlockState()));
        assertFalse(TemporaryLightManager.isReplaceableForLight(Blocks.OAK_DOOR.defaultBlockState()));
        assertFalse(TemporaryLightManager.isReplaceableForLight(Blocks.LIGHT.defaultBlockState()));
        assertFalse(TemporaryLightManager.isReplaceableForLight(null));

        BlockState airLight = TemporaryLightManager.createLightState(Blocks.AIR.defaultBlockState(), 15);
        assertTrue(airLight.is(Blocks.LIGHT));
        assertEquals(15, airLight.getValue(LightBlock.LEVEL));
        assertFalse(airLight.getValue(LightBlock.WATERLOGGED));

        BlockState waterLight = TemporaryLightManager.createLightState(Blocks.WATER.defaultBlockState(), 15);
        assertTrue(waterLight.is(Blocks.LIGHT));
        assertEquals(15, waterLight.getValue(LightBlock.LEVEL));
        assertTrue(waterLight.getValue(LightBlock.WATERLOGGED));
    }

    @Test
    @DisplayName("TemporaryLightManager places waterlogged light in still water, refreshes without losing originalState, and restores water on expiry")
    void testWaterPlacementRefreshAndRestore() {
        FakeLightWorld world = new FakeLightWorld(Level.OVERWORLD);
        BlockPos impactGround = new BlockPos(5, 64, 5);
        BlockPos waterPos = impactGround.above();

        world.putBlock(impactGround, Blocks.STONE.defaultBlockState());
        world.putBlock(waterPos, Blocks.WATER.defaultBlockState());
        world.setGameTime(100L);

        // Origin is solid stone, so it should fall back to origin.above() (water source)
        assertTrue(TemporaryLightManager.placeTemporaryLight(world, impactGround, 200));
        assertEquals(1, TemporaryLightManager.getActiveLightCount());

        BlockState placed = world.getBlockState(waterPos);
        assertTrue(placed.is(Blocks.LIGHT));
        assertEquals(15, placed.getValue(LightBlock.LEVEL));
        assertTrue(placed.getValue(LightBlock.WATERLOGGED));

        TemporaryLightManager.LightEntry entry = TemporaryLightManager.getActiveLight(Level.OVERWORLD, waterPos);
        assertNotNull(entry);
        assertEquals(Blocks.WATER.defaultBlockState(), entry.getOriginalState());
        assertEquals(300L, entry.getExpireGameTime());

        // Second flash at same stone block at t=200 must refresh timer without overwriting original water state
        world.setGameTime(200L);
        assertTrue(TemporaryLightManager.placeTemporaryLight(world, impactGround, 200));
        assertEquals(1, TemporaryLightManager.getActiveLightCount());
        assertEquals(Blocks.WATER.defaultBlockState(), entry.getOriginalState());
        assertEquals(400L, entry.getExpireGameTime());

        // Still active at t=399
        world.setGameTime(399L);
        TemporaryLightManager.tickLevel(world);
        assertEquals(1, TemporaryLightManager.getActiveLightCount());
        assertTrue(world.getBlockState(waterPos).is(Blocks.LIGHT));

        // Expires at t=400 and restores still water
        world.setGameTime(400L);
        TemporaryLightManager.tickLevel(world);
        assertEquals(0, TemporaryLightManager.getActiveLightCount());
        assertEquals(Blocks.WATER.defaultBlockState(), world.getBlockState(waterPos));
    }

    @Test
    @DisplayName("Expiration and shutdown cleanup never overwrite blocks placed by players")
    void testNonDestructiveRestorationWhenBlockReplacedByPlayer() {
        FakeLightWorld world = new FakeLightWorld(Level.OVERWORLD);
        BlockPos airPos = new BlockPos(12, 70, -4);
        BlockPos secondAirPos = new BlockPos(15, 70, -4);

        world.putBlock(airPos, Blocks.AIR.defaultBlockState());
        world.putBlock(secondAirPos, Blocks.AIR.defaultBlockState());
        world.setGameTime(10L);

        assertTrue(TemporaryLightManager.placeTemporaryLight(world, airPos, 200));
        assertTrue(TemporaryLightManager.placeTemporaryLight(world, secondAirPos, 200));
        assertEquals(2, TemporaryLightManager.getActiveLightCount());

        // Player replaces the first light block with cobblestone before expiration
        world.putBlock(airPos, Blocks.COBBLESTONE.defaultBlockState());

        // Expiry at t=210 preserves player's cobblestone and restores second light back to air
        world.setGameTime(210L);
        TemporaryLightManager.tickLevel(world);
        assertEquals(0, TemporaryLightManager.getActiveLightCount());
        assertEquals(Blocks.COBBLESTONE.defaultBlockState(), world.getBlockState(airPos));
        assertEquals(Blocks.AIR.defaultBlockState(), world.getBlockState(secondAirPos));
    }

    @Test
    @DisplayName("clearAll restores active temporary lights across dimensions and null safety holds")
    void testClearAllAcrossDimensionsAndNullSafety() {
        FakeLightWorld overworld = new FakeLightWorld(Level.OVERWORLD);
        FakeLightWorld nether = new FakeLightWorld(Level.NETHER);
        BlockPos owPos = new BlockPos(0, 65, 0);
        BlockPos netherPos = new BlockPos(3, 50, 3);

        overworld.putBlock(owPos, Blocks.AIR.defaultBlockState());
        nether.putBlock(netherPos, Blocks.AIR.defaultBlockState());

        assertTrue(TemporaryLightManager.placeTemporaryLight(overworld, owPos, 200));
        assertTrue(TemporaryLightManager.placeTemporaryLight(nether, netherPos, 200));
        assertEquals(2, TemporaryLightManager.getActiveLightCount());

        TemporaryLightManager.clearAll(dim -> {
            if (Level.OVERWORLD.equals(dim)) return overworld;
            if (Level.NETHER.equals(dim)) return nether;
            return null;
        });

        assertEquals(0, TemporaryLightManager.getActiveLightCount());
        assertEquals(Blocks.AIR.defaultBlockState(), overworld.getBlockState(owPos));
        assertEquals(Blocks.AIR.defaultBlockState(), nether.getBlockState(netherPos));

        // Null-safety checks
        assertFalse(TemporaryLightManager.placeTemporaryLight((ServerLevel) null, BlockPos.ZERO, 200));
        assertFalse(TemporaryLightManager.placeTemporaryLight(overworld, owPos, 0));
        assertDoesNotThrow(() -> TemporaryLightManager.tickLevel((ServerLevel) null));
        assertDoesNotThrow(() -> TemporaryLightManager.onServerTick(null));
        assertDoesNotThrow(() -> TemporaryLightManager.onServerStopping(null));
        assertDoesNotThrow(() -> TemporaryLightManager.clearAll((MinecraftServer) null));
    }
}
