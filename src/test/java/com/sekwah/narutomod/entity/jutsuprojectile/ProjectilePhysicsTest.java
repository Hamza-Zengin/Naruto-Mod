package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.abilities.jutsus.FireballJutsuAbility;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Jutsu projectile launch and mid-air sticking fixes:
 * - Non-barrier JutsuVisualEffectEntity filtering for Fireball, Rasenshuriken, WaterBullet, EarthBullet, VacuumSphere
 * - FireballJutsuAbility immediate spawn without delayed freeze and mastery scaling
 * - Entity tracking range/update interval, accelerationPower, spawn offset, impact discard, and light-block clip filtering
 */
public class ProjectilePhysicsTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("JutsuProjectile.isBarrierForProjectile and isBarrierVisualType only allow Water Wall and enemy Earth Wall")
    public void testBarrierVisualTypeFiltering() {
        assertFalse(JutsuProjectile.isBarrierForProjectile(null, null),
                "Null visual effect must not be treated as a barrier");

        // Actual barrier types
        assertTrue(JutsuProjectile.isBarrierVisualType(JutsuVisualEffectEntity.TYPE_WATER_WALL, true));
        assertTrue(JutsuProjectile.isBarrierVisualType(JutsuVisualEffectEntity.TYPE_WATER_WALL, false));
        assertTrue(JutsuProjectile.isBarrierVisualType(JutsuVisualEffectEntity.TYPE_EARTH_WALL, false),
                "Enemy Earth Wall must block projectiles");
        assertFalse(JutsuProjectile.isBarrierVisualType(JutsuVisualEffectEntity.TYPE_EARTH_WALL, true),
                "Allied Earth Wall must not block allied projectiles");

        // Non-barrier visual effect types must always be ignored
        int[] nonBarrierTypes = new int[]{
                JutsuVisualEffectEntity.TYPE_KIRIN_DRAGON,
                JutsuVisualEffectEntity.TYPE_WOOD_PRISON,
                JutsuVisualEffectEntity.TYPE_BONE_FOREST,
                JutsuVisualEffectEntity.TYPE_RASENGAN_SPHERE,
                JutsuVisualEffectEntity.TYPE_CHIDORI_BURST,
                JutsuVisualEffectEntity.TYPE_WATER_VORTEX,
                JutsuVisualEffectEntity.TYPE_WIND_SHOCKWAVE,
                JutsuVisualEffectEntity.TYPE_AMATERASU_PILLAR,
                JutsuVisualEffectEntity.TYPE_SHADOW_BIND,
                JutsuVisualEffectEntity.TYPE_STONE_SPIKES,
                JutsuVisualEffectEntity.TYPE_ADAMANTINE_CHAINS,
                JutsuVisualEffectEntity.TYPE_ASH_CLOUD
        };
        for (int type : nonBarrierTypes) {
            assertFalse(JutsuProjectile.isBarrierVisualType(type, false),
                    "Non-barrier visual type " + type + " must be ignored when enemy");
            assertFalse(JutsuProjectile.isBarrierVisualType(type, true),
                    "Non-barrier visual type " + type + " must be ignored when allied");
        }
    }

    @Test
    @DisplayName("FireballJutsuEntity and RasenshurikenEntity ignore non-barrier JutsuVisualEffectEntity and Blocks.LIGHT")
    public void testFireballAndRasenshurikenStickingFixes() throws IOException {
        String fireballSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/FireballJutsuEntity.java"));
        assertTrue(fireballSrc.contains("JutsuProjectile.isBarrierForProjectile(visual, this.getOwner())"),
                "FireballJutsuEntity must use JutsuProjectile.isBarrierForProjectile in canHitEntity and clashBox");
        assertTrue(fireballSrc.contains("hitState.is(Blocks.LIGHT) || hitState.getCollisionShape(this.level(), hitPos).isEmpty()"),
                "FireballJutsuEntity must ignore Blocks.LIGHT and empty-collision blocks");
        assertTrue(fireballSrc.contains("if (it >= (mini ? 2 : 3))"),
                "FireballJutsuEntity must discard after brief 2/3-tick flash");
        assertFalse(fireballSrc.contains("if (this.time <= (int) GROW_TIME) {\n            this.refreshDimensions();\n        }"),
                "FireballJutsuEntity must not call refreshDimensions() during GROW_TIME");
        assertTrue(fireballSrc.contains("this.accelerationPower = 0.24D;"),
                "FireballJutsuEntity must set accelerationPower = 0.24D");

        String rasenSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/RasenshurikenEntity.java"));
        assertTrue(rasenSrc.contains("JutsuProjectile.isBarrierForProjectile(visual, this.getOwner())"),
                "RasenshurikenEntity must use JutsuProjectile.isBarrierForProjectile in canHitEntity and clashBox");
        assertTrue(rasenSrc.contains("hitState.is(Blocks.LIGHT) || hitState.getCollisionShape(this.level(), hitPos).isEmpty()"),
                "RasenshurikenEntity must ignore Blocks.LIGHT and empty-collision blocks");
        assertTrue(rasenSrc.contains("this.accelerationPower = 0.32D;"),
                "RasenshurikenEntity must set accelerationPower = 0.32D");

        for (String proj : new String[]{"WaterBulletEntity", "EarthBulletEntity", "VacuumSphereEntity"}) {
            String src = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/" + proj + ".java"));
            assertTrue(src.contains("JutsuProjectile.isBarrierForProjectile(visual, this.getOwner())"),
                    proj + " must filter non-barrier JutsuVisualEffectEntity in canHitEntity and clashBox");
        }
    }

    @Test
    @DisplayName("NarutoEntities configures FIREBALL_JUTSU and RASENSHURIKEN with trackingRange(10) and updateInterval(2)")
    public void testEntityTrackingAndUpdateInterval() throws IOException {
        String entitiesSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/entity/NarutoEntities.java"));
        assertTrue(entitiesSrc.contains("FIREBALL_JUTSU = register(\"fireball_jutsu\",\n            FireballJutsuEntity::new, MobCategory.MISC, builder -> builder.sized(1.5F, 1.5F).clientTrackingRange(10).updateInterval(2))"),
                "FIREBALL_JUTSU must use clientTrackingRange(10).updateInterval(2)");
        assertTrue(entitiesSrc.contains("RASENSHURIKEN = register(\"rasenshuriken\",\n            RasenshurikenEntity::new, MobCategory.MISC, builder -> builder.sized(1.6F, 0.8F).clientTrackingRange(10).updateInterval(2))"),
                "RASENSHURIKEN must use clientTrackingRange(10).updateInterval(2)");
    }

    @Test
    @DisplayName("FireballJutsuAbility spawns immediately without delayed freeze and scales speed/lifespan with mastery")
    public void testFireballAbilityImmediateSpawnAndMasteryScaling() throws IOException {
        String abilitySrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/abilities/jutsus/FireballJutsuAbility.java"));
        assertFalse(abilitySrc.contains("scheduleDelayedTickEvent"),
                "FireballJutsuAbility must not use scheduleDelayedTickEvent");
        assertTrue(abilitySrc.contains("fireball.setFireballSpeedAndRange((float) speedMult, 0);"),
                "FireballJutsuAbility must apply speedMult via setFireballSpeedAndRange");
        assertTrue(abilitySrc.contains("fireball.lifeSpan = lifeSpan;"),
                "FireballJutsuAbility must apply mastery-scaled lifeSpan");

        // Mastery 0 -> base speed 1.0, base lifespan 70
        assertEquals(1.0D, FireballJutsuAbility.getScaledSpeedMult(0), 1.0E-5D);
        assertEquals(70, FireballJutsuAbility.getScaledLifeSpan(0));

        // Higher mastery -> faster speed and longer lifespan
        double midSpeed = FireballJutsuAbility.getScaledSpeedMult(350);
        double maxSpeed = FireballJutsuAbility.getScaledSpeedMult(700);
        int midLife = FireballJutsuAbility.getScaledLifeSpan(350);
        int maxLife = FireballJutsuAbility.getScaledLifeSpan(700);

        assertTrue(midSpeed > 1.0D && maxSpeed >= midSpeed,
                "Speed multiplier must scale upward with mastery");
        assertEquals(1.25D, maxSpeed, 1.0E-5D);
        assertTrue(midLife > 70 && maxLife >= midLife,
                "LifeSpan must scale upward with mastery");
        assertEquals(95, maxLife);
    }
}
