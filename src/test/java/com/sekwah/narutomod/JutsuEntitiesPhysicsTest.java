package com.sekwah.narutomod;

import com.sekwah.narutomod.entity.jutsuprojectile.MudWallEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WoodPrisonEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Sub-stage 2.1: Jutsu Entity and Physics Decoupling.
 * Verifies modular entity hierarchy, physics constants, registrations, and ability decoupling.
 */
public class JutsuEntitiesPhysicsTest {

    @Test
    @DisplayName("Verify WaterBulletEntity extends ThrowableItemProjectile")
    public void testWaterBulletHierarchy() {
        assertTrue(ThrowableItemProjectile.class.isAssignableFrom(WaterBulletEntity.class),
                "WaterBulletEntity must extend ThrowableItemProjectile");
    }

    @Test
    @DisplayName("Verify MudWallEntity physical AABB dimensions, health, and duration constants")
    public void testMudWallPhysicsConstants() {
        assertEquals(3.2F, MudWallEntity.DEFAULT_WIDTH, 0.01F);
        assertEquals(2.8F, MudWallEntity.DEFAULT_HEIGHT, 0.01F);
        assertTrue(MudWallEntity.DEFAULT_MAX_HEALTH > 0.0F, "MudWallEntity must have positive health");
        assertTrue(MudWallEntity.DEFAULT_MAX_LIFE > 0, "MudWallEntity must have positive duration limit");
    }

    @Test
    @DisplayName("Verify WoodPrisonEntity cage trap dimensions and duration constants")
    public void testWoodPrisonConstants() {
        assertTrue(WoodPrisonEntity.CAGE_RADIUS > 0.0D);
        assertTrue(WoodPrisonEntity.CAGE_HEIGHT > 0.0D);
        assertTrue(WoodPrisonEntity.DEFAULT_MAX_LIFE > 0);
    }

    @Test
    @DisplayName("Verify NarutoEntities registers WATER_BULLET, MUD_WALL, and WOOD_PRISON")
    public void testEntityRegistrationsInSource() throws IOException {
        String content = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/entity/NarutoEntities.java"));
        assertTrue(content.contains("WATER_BULLET = register(\"water_bullet\""));
        assertTrue(content.contains("MUD_WALL = register(\"mud_wall\""));
        assertTrue(content.contains("WOOD_PRISON = register(\"wood_prison\""));
    }

    @Test
    @DisplayName("Verify Ability classes spawn modular entities instead of JutsuVisualEffectEntity")
    public void testAbilitiesDecoupledFromVisualEffectEntity() throws IOException {
        String waterAbility = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/abilities/jutsus/WaterBulletAbility.java"));
        String mudWallAbility = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/abilities/jutsus/MudWallAbility.java"));
        String woodPrisonAbility = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/abilities/jutsus/WoodPrisonAbility.java"));

        assertFalse(waterAbility.contains("JutsuVisualEffectEntity"), "WaterBulletAbility should not reference JutsuVisualEffectEntity");
        assertTrue(waterAbility.contains("WaterBulletEntity"), "WaterBulletAbility must spawn WaterBulletEntity");

        assertFalse(mudWallAbility.contains("JutsuVisualEffectEntity"), "MudWallAbility should not reference JutsuVisualEffectEntity");
        assertTrue(mudWallAbility.contains("MudWallEntity"), "MudWallAbility must spawn MudWallEntity");

        assertFalse(woodPrisonAbility.contains("JutsuVisualEffectEntity"), "WoodPrisonAbility should not reference JutsuVisualEffectEntity");
        assertTrue(woodPrisonAbility.contains("WoodPrisonEntity"), "WoodPrisonAbility must spawn WoodPrisonEntity");
    }

    @Test
    @DisplayName("Verify NarutoRenderer registers EntityRenderers for the new jutsu entities")
    public void testNarutoRendererRegistrations() throws IOException {
        String rendererContent = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/client/renderer/NarutoRenderer.java"));
        assertTrue(rendererContent.contains("EntityRenderers.register(NarutoEntities.WATER_BULLET.get()"));
        assertTrue(rendererContent.contains("EntityRenderers.register(NarutoEntities.MUD_WALL.get()"));
        assertTrue(rendererContent.contains("EntityRenderers.register(NarutoEntities.WOOD_PRISON.get()"));
    }
}
