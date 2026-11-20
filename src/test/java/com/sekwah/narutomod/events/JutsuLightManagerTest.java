package com.sekwah.narutomod.events;

import com.sekwah.narutomod.entity.jutsuprojectile.EarthBulletEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.MudWallEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.VacuumSphereEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WoodPrisonEntity;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JutsuLightManager} light level classification, movement hysteresis,
 * TTL expiry, empty-map zero-cost fast-path, and entity/event wiring.
 */
public class JutsuLightManagerTest {

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void resetActiveLights() {
        JutsuLightManager.clearAllTrackingStateForTest();
    }

    @Test
    @DisplayName("Fire and Lightning jutsus resolve to light level 12")
    void testFireAndLightningLightLevel12() {
        assertEquals(12, JutsuLightManager.resolveLightLevelForEntityClass(FireballJutsuEntity.class));
        assertEquals(12, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_AMATERASU_PILLAR));
        assertEquals(12, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_ASH_CLOUD));
        assertEquals(12, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_KIRIN_DRAGON));
        assertEquals(12, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_CHIDORI_BURST));
        assertEquals(12, JutsuLightManager.resolveLightLevelForHandJutsu("chidori"));
        assertEquals(12, JutsuLightManager.resolveLightLevelForHandJutsu("CHIDORI"));
    }

    @Test
    @DisplayName("Other chakra and elemental energy jutsus resolve to light level 6")
    void testOtherEnergyLightLevel6() {
        assertEquals(6, JutsuLightManager.resolveLightLevelForEntityClass(RasenshurikenEntity.class));
        assertEquals(6, JutsuLightManager.resolveLightLevelForEntityClass(WaterBulletEntity.class));
        assertEquals(6, JutsuLightManager.resolveLightLevelForEntityClass(VacuumSphereEntity.class));
        assertEquals(6, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_RASENGAN_SPHERE));
        assertEquals(6, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_WATER_VORTEX));
        assertEquals(6, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_WIND_SHOCKWAVE));
        assertEquals(6, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_ADAMANTINE_CHAINS));
        assertEquals(6, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_WATER_WALL));
        assertEquals(6, JutsuLightManager.resolveLightLevelForHandJutsu("rasengan"));
        assertEquals(6, JutsuLightManager.resolveLightLevelForHandJutsu("chakra_scalpel"));
    }

    @Test
    @DisplayName("Physical techniques and unknown/empty jutsus emit zero light")
    void testPhysicalTechniquesEmitZeroLight() {
        assertEquals(0, JutsuLightManager.resolveLightLevelForEntityClass(EarthBulletEntity.class));
        assertEquals(0, JutsuLightManager.resolveLightLevelForEntityClass(MudWallEntity.class));
        assertEquals(0, JutsuLightManager.resolveLightLevelForEntityClass(WoodPrisonEntity.class));
        assertEquals(0, JutsuLightManager.resolveLightLevelForEntityClass(null));

        assertEquals(0, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_EARTH_WALL));
        assertEquals(0, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_STONE_SPIKES));
        assertEquals(0, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_WOOD_PRISON));
        assertEquals(0, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_BONE_FOREST));
        assertEquals(0, JutsuLightManager.resolveLightLevelForVisualType(JutsuVisualEffectEntity.TYPE_SHADOW_BIND));
        assertEquals(0, JutsuLightManager.resolveLightLevelForVisualType(999));

        assertEquals(0, JutsuLightManager.resolveLightLevelForHandJutsu(null));
        assertEquals(0, JutsuLightManager.resolveLightLevelForHandJutsu(""));
        assertEquals(0, JutsuLightManager.resolveLightLevelForHandJutsu("earth_wall"));
    }

    @Test
    @DisplayName("Movement hysteresis does not move below 2 blocks before 3 ticks, moves at >=2 blocks or >=3 ticks")
    void testMovementHysteresis() {
        BlockPos origin = new BlockPos(10, 64, 10);
        BlockPos same = new BlockPos(10, 64, 10);
        BlockPos dist1 = new BlockPos(11, 64, 10);
        BlockPos dist2Diagonal = new BlockPos(11, 64, 11);
        BlockPos dist2Axis = new BlockPos(12, 64, 10);

        // Initial placement (oldPos == null) always places
        assertTrue(JutsuLightManager.shouldMoveLight(null, origin, 0));
        // Null newPos never moves
        assertFalse(JutsuLightManager.shouldMoveLight(origin, null, 10));
        // Identical position never moves regardless of ticks elapsed
        assertFalse(JutsuLightManager.shouldMoveLight(origin, same, 0));
        assertFalse(JutsuLightManager.shouldMoveLight(origin, same, 10));

        // Manhattan distance < 2 (1 block) and ticksSinceMove < 3 -> do NOT move
        assertFalse(JutsuLightManager.shouldMoveLight(origin, dist1, 0));
        assertFalse(JutsuLightManager.shouldMoveLight(origin, dist1, 1));
        assertFalse(JutsuLightManager.shouldMoveLight(origin, dist1, 2));

        // Manhattan distance == 1 but ticksSinceMove >= 3 -> move allowed
        assertTrue(JutsuLightManager.shouldMoveLight(origin, dist1, 3));

        // Manhattan distance >= 2 -> move immediately even at ticksSinceMove == 0
        assertTrue(JutsuLightManager.shouldMoveLight(origin, dist2Diagonal, 0));
        assertTrue(JutsuLightManager.shouldMoveLight(origin, dist2Axis, 0));
    }

    @Test
    @DisplayName("TTL expiration and empty-map tracking state behave safely")
    void testTtlAndEmptyMapBehavior() {
        assertFalse(JutsuLightManager.hasActiveLights());
        assertEquals(0, JutsuLightManager.getActiveLightCount());

        // Null serverLevel / null source safe no-op
        JutsuLightManager.updateLight(null, null, BlockPos.ZERO, 12);
        JutsuLightManager.removeLight((net.minecraft.server.MinecraftServer) null, UUID.randomUUID());
        JutsuLightManager.removeLight((net.minecraft.server.level.ServerLevel) null, UUID.randomUUID());
        assertFalse(JutsuLightManager.hasActiveLights());

        // Empty map onServerTick fast-path returns safely even with null event when empty
        assertDoesNotThrow(() -> JutsuLightManager.onServerTick(null));
        assertDoesNotThrow(() -> JutsuLightManager.onServerStopping(null));

        // TTL boundary check
        assertFalse(JutsuLightManager.isEntryExpired(99L, 100L));
        assertTrue(JutsuLightManager.isEntryExpired(100L, 100L));
        assertTrue(JutsuLightManager.isEntryExpired(105L, 100L));

        // Active tracking map state and removal
        UUID id = UUID.randomUUID();
        JutsuLightManager.trackForTest(id, Level.OVERWORLD, new BlockPos(0, 64, 0), 12, 10L, 14L);
        assertTrue(JutsuLightManager.hasActiveLights());
        assertEquals(1, JutsuLightManager.getActiveLightCount());

        JutsuLightManager.removeLight((net.minecraft.server.level.ServerLevel) null, id);
        assertFalse(JutsuLightManager.hasActiveLights());
        assertEquals(0, JutsuLightManager.getActiveLightCount());
    }

    @Test
    @DisplayName("Source integration verifies light safety flags and entity/event wiring")
    void testSourceWiringAndLightBlockSafety() throws IOException {
        String managerSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/events/JutsuLightManager.java"));
        assertTrue(managerSrc.contains("ServerTickEvent.Post"));
        assertTrue(managerSrc.contains("ServerStoppingEvent"));
        assertTrue(managerSrc.contains("ACTIVE_LIGHTS.isEmpty()"));
        assertTrue(managerSrc.contains("Block.UPDATE_CLIENTS"));
        assertTrue(managerSrc.contains("Blocks.LIGHT"));
        assertTrue(managerSrc.contains("LightBlock.LEVEL"));
        assertTrue(managerSrc.contains(".isAir()"));
        assertTrue(managerSrc.contains(".is(Blocks.LIGHT)"));

        for (String path : new String[]{
                "src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/JutsuVisualEffectEntity.java",
                "src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/FireballJutsuEntity.java",
                "src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/RasenshurikenEntity.java",
                "src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/WaterBulletEntity.java",
                "src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/VacuumSphereEntity.java",
                "src/main/java/com/sekwah/narutomod/events/PlayerEvents.java",
                "src/main/java/com/sekwah/narutomod/entity/ShadowCloneEntity.java"
        }) {
            String src = Files.readString(Paths.get(path));
            assertTrue(src.contains("JutsuLightManager"), path + " must integrate JutsuLightManager");
        }
    }
}
