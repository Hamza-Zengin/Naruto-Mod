package com.sekwah.narutomod;

import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RasenshurikenEntity non-griefing eroded crater trail,
 * Block.getId-based client vortex effect, and distance-attenuated camera shake.
 */
public class RasenshurikenCraterTest {

    @Test
    @DisplayName("getErodedCraterState converts surface blocks into eroded crater variants without deleting terrain")
    public void testErodedCraterStateMapping() {
        assertEquals(Blocks.COARSE_DIRT.defaultBlockState(),
                RasenshurikenEntity.getErodedCraterState(Blocks.GRASS_BLOCK.defaultBlockState()));
        assertEquals(Blocks.DIRT.defaultBlockState(),
                RasenshurikenEntity.getErodedCraterState(Blocks.PODZOL.defaultBlockState()));
        assertEquals(Blocks.COBBLESTONE.defaultBlockState(),
                RasenshurikenEntity.getErodedCraterState(Blocks.STONE.defaultBlockState()));
        assertEquals(Blocks.COBBLED_DEEPSLATE.defaultBlockState(),
                RasenshurikenEntity.getErodedCraterState(Blocks.DEEPSLATE.defaultBlockState()));
        assertEquals(Blocks.CRACKED_STONE_BRICKS.defaultBlockState(),
                RasenshurikenEntity.getErodedCraterState(Blocks.STONE_BRICKS.defaultBlockState()));
        assertNull(RasenshurikenEntity.getErodedCraterState(Blocks.OBSIDIAN.defaultBlockState()),
                "Unmapped durable blocks must remain unchanged");
    }

    @Test
    @DisplayName("RasenshurikenEntity and ClientEffectHandler wire Block.getId vortex and ClientCameraShakePacket")
    public void testSourceWiring() throws IOException {
        String entitySrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/RasenshurikenEntity.java"));
        assertTrue(entitySrc.contains("Block.getId("), "RasenshurikenEntity must encode surface block via Block.getId");
        assertTrue(entitySrc.contains("ClientJutsuEffectPacket"), "RasenshurikenEntity must emit lightweight S2C vortex packet");
        assertTrue(entitySrc.contains("ClientCameraShakePacket"), "RasenshurikenEntity must trigger distance-attenuated camera shake");
        assertTrue(entitySrc.contains("WorldProtectionHelper.canModifyBlock("),
                "RasenshurikenEntity must check WorldProtectionHelper.canModifyBlock before altering surface blocks");

        String clientSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/client/events/ClientEffectHandler.java"));
        assertTrue(clientSrc.contains("\"rasenshuriken_vortex\"") && clientSrc.contains("Block.stateById"),
                "ClientEffectHandler must decode surface block via Block.stateById for rasenshuriken_vortex");
    }
}
