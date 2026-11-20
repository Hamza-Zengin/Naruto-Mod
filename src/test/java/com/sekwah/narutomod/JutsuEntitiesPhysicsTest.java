package com.sekwah.narutomod;

import com.sekwah.narutomod.entity.jutsuprojectile.MudWallEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WoodPrisonEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
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
    @DisplayName("Verify NarutoRenderEvents registers EntityRenderers for the new jutsu entities")
    public void testNarutoRendererRegistrations() throws IOException {
        String rendererContent = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/client/renderer/NarutoRenderEvents.java"));
        assertTrue(rendererContent.contains("event.registerEntityRenderer(NarutoEntities.WATER_BULLET.get()"));
        assertTrue(rendererContent.contains("event.registerEntityRenderer(NarutoEntities.MUD_WALL.get()"));
        assertTrue(rendererContent.contains("event.registerEntityRenderer(NarutoEntities.WOOD_PRISON.get()"));
    }

    @Test
    @DisplayName("Verify Kawarimi spiral search avoids walls, mastery scales, and no logs/particles exist")
    public void testKawarimiSafeOpenSpaceAndStealth() throws IOException {
        // 1. Mastery scaling: distance, cooldown, and cast duration decrease as mastery goes 0 -> 100
        double dist0 = com.sekwah.narutomod.abilities.jutsus.KawarimiJutsuAbility.getScaledTeleportDistance(0);
        double dist100 = com.sekwah.narutomod.abilities.jutsus.KawarimiJutsuAbility.getScaledTeleportDistance(100);
        assertTrue(dist0 > dist100, "Kawarimi distance must decrease from max (0 mastery) to min (100 mastery)");

        int cd0 = com.sekwah.narutomod.abilities.jutsus.KawarimiJutsuAbility.getScaledCooldownTicks(0);
        int cd100 = com.sekwah.narutomod.abilities.jutsus.KawarimiJutsuAbility.getScaledCooldownTicks(100);
        assertTrue(cd0 > cd100, "Kawarimi cooldown must decrease as mastery increases");

        com.sekwah.narutomod.abilities.jutsus.KawarimiJutsuAbility kawarimi =
                new com.sekwah.narutomod.abilities.jutsus.KawarimiJutsuAbility();
        assertTrue(kawarimi.getScaledActivationDelay(0) > kawarimi.getScaledActivationDelay(100),
                "Kawarimi cast duration must decrease as mastery increases");

        // 2. Spiral search must never return a walled/blocked coordinate and must find nearest open block
        net.minecraft.core.BlockPos anchor = new net.minecraft.core.BlockPos(10, 64, 10);
        net.minecraft.core.BlockPos openSpot = new net.minecraft.core.BlockPos(12, 64, 10);
        net.minecraft.core.BlockPos found = com.sekwah.narutomod.abilities.jutsus.KawarimiJutsuAbility.findSafeOpenBlockPos(
                pos -> pos.equals(openSpot),
                anchor,
                6
        );
        assertEquals(openSpot, found, "Spiral search must skip blocked anchor and return nearest safe open spot");

        // When entire area is solid wall, must return null (cancel teleport instead of suffocating player)
        net.minecraft.core.BlockPos none = com.sekwah.narutomod.abilities.jutsus.KawarimiJutsuAbility.findSafeOpenBlockPos(
                pos -> false,
                anchor,
                6
        );
        assertNull(none, "Must return null when no safe open space exists");

        // 3. Source check: zero OAK_LOG, zero particles, zero sounds
        String src = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/abilities/jutsus/KawarimiJutsuAbility.java"));
        assertFalse(src.contains("OAK_LOG"), "Kawarimi must not place OAK_LOG");
        assertFalse(src.contains("sendParticles"), "Kawarimi must not spawn particles");
        assertFalse(src.contains("playSound"), "Kawarimi must not play sounds");
    }

    @Test
    @DisplayName("Verify WaterBulletEntity yaw=atan2(-x,z) and pitch=atan2(y,horiz) conventions")
    public void testWaterBulletMotionRotationConvention() {
        assertEquals(0.0F, WaterBulletEntity.computeYawFromMotion(new net.minecraft.world.phys.Vec3(0, 0, 1)), 0.01F);
        assertEquals(-90.0F, WaterBulletEntity.computeYawFromMotion(new net.minecraft.world.phys.Vec3(1, 0, 0)), 0.01F);
        assertEquals(90.0F, WaterBulletEntity.computeYawFromMotion(new net.minecraft.world.phys.Vec3(-1, 0, 0)), 0.01F);
        assertEquals(180.0F, Math.abs(WaterBulletEntity.computeYawFromMotion(new net.minecraft.world.phys.Vec3(0, 0, -1))), 0.01F);
        assertEquals(45.0F, WaterBulletEntity.computePitchFromMotion(new net.minecraft.world.phys.Vec3(0, 1, 1)), 0.01F);
        assertEquals(-45.0F, WaterBulletEntity.computePitchFromMotion(new net.minecraft.world.phys.Vec3(0, -1, 1)), 0.01F);
    }

    @Test
    @DisplayName("Verify JutsuCombatHelper.lightningDamage signature and ShadowCloneEntity.areAllies null safety")
    public void testLightningDamageHelperAndAllyNullSafety() throws NoSuchMethodException {
        var method = com.sekwah.narutomod.abilities.JutsuCombatHelper.class.getMethod(
                "lightningDamage",
                net.minecraft.server.level.ServerLevel.class,
                net.minecraft.world.entity.Entity.class,
                net.minecraft.world.entity.LivingEntity.class);
        assertEquals(net.minecraft.world.damagesource.DamageSource.class, method.getReturnType());

        assertFalse(com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(null, null),
                "areAllies(null, null) must safely return false");
        assertNull(com.sekwah.narutomod.entity.ShadowCloneEntity.resolveRootOwnerUUID(null),
                "resolveRootOwnerUUID(null) must safely return null");
    }

    @Test
    @DisplayName("Verify ally-clone filtering and lightning damage attribution across visual/sibling jutsu classes")
    public void testAllyCloneFilteringAndLightningAttributionInSource() throws IOException {
        String visualEffect = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/JutsuVisualEffectEntity.java"));
        assertTrue(visualEffect.contains("JutsuCombatHelper.lightningDamage(serverLevel, this, caster)"),
                "TYPE_KIRIN_DRAGON must attribute lightning damage via JutsuCombatHelper.lightningDamage");
        assertTrue(visualEffect.contains("JutsuCombatHelper.lightningDamage(serverLevel, this, fieldOwner)"),
                "TYPE_CHIDORI_BURST must attribute lightning damage via JutsuCombatHelper.lightningDamage");
        assertTrue(visualEffect.contains("ElementalInteractionHandler.boostFireballWithWind(fireball)"),
                "TYPE_WIND_SHOCKWAVE must boost allied FireballJutsuEntity via ElementalInteractionHandler");

        String woodPrison = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/WoodPrisonEntity.java"));
        assertTrue(woodPrison.contains("ShadowCloneEntity.areAllies(caster, e)"),
                "WoodPrisonEntity.tick must exclude allied shadow clones");

        String fireball = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/FireballJutsuEntity.java"));
        assertTrue(fireball.contains("if (entity == owner || com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(owner, entity))"),
                "FireballJutsuEntity.onHit must exclude owner and allied shadow clones for both normal and mini fireballs");

        String playerEvents = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/events/PlayerEvents.java"));
        assertTrue(playerEvents.contains("!ShadowCloneEntity.areAllies(player, livingTarget)"),
                "PlayerEvents.onAttackEntity must guard Byakugan chakra-lock strike against allied shadow clones");

        String rasengan = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/abilities/jutsus/RasenganAbility.java"));
        assertTrue(rasengan.contains("ShadowCloneEntity.areAllies(player, e)"),
                "RasenganAbility.executeStrike fallback box must exclude allied shadow clones");

        String chidori = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriAbility.java"));
        assertTrue(chidori.contains("ShadowCloneEntity.areAllies(player, e)")
                        && chidori.contains("JutsuCombatHelper.lightningDamage(serverLevel, chidori3D, player)"),
                "ChidoriAbility.executeStrike must exclude allies and use JutsuCombatHelper.lightningDamage");

        String chidoriStream = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/abilities/jutsus/ChidoriStreamAbility.java"));
        assertTrue(chidoriStream.contains("JutsuCombatHelper.lightningDamage(serverLevel, null, player)"),
                "ChidoriStreamAbility must use JutsuCombatHelper.lightningDamage");

        String kirin = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/abilities/jutsus/KirinAbility.java"));
        assertTrue(kirin.contains("JutsuCombatHelper.lightningDamage(serverLevel, kirinDragon, player)"),
                "KirinAbility must attribute lightning damage via JutsuCombatHelper.lightningDamage");
        assertTrue(kirin.contains("ShadowCloneEntity.areAllies(player, e)"),
                "KirinAbility must exclude allied shadow clones in target raycast and blast area");
    }

    @Test
    @DisplayName("Verify assigned Ability subclasses adopt getFinalChakraCost in handleCost")
    public void testAssignedAbilitiesAdoptFinalChakraCost() throws IOException {
        java.util.List<String> assignedAbilities = java.util.List.of(
                "AdamantineChainsAbility",
                "BoneDanceAbility",
                "KawarimiJutsuAbility",
                "KirinAbility",
                "MudWallAbility",
                "MysticalPalmAbility",
                "RasenshurikenAbility",
                "WindBreakerAbility",
                "WoodPrisonAbility"
        );
        for (String name : assignedAbilities) {
            String src = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/abilities/jutsus/" + name + ".java"));
            assertTrue(src.contains("getFinalChakraCost(player, ninjaData)"),
                    name + ".handleCost must call getFinalChakraCost(player, ninjaData)");
        }

        // Verify handleCost executes cleanly with NinjaData and consumes getFinalChakraCost
        com.sekwah.narutomod.capabilities.NinjaData data = new com.sekwah.narutomod.capabilities.NinjaData(true);
        data.setMaxChakra(200.0f);
        data.setChakra(200.0f);
        data.setMaxStamina(200.0f);
        data.setStamina(200.0f);

        var mudWall = new com.sekwah.narutomod.abilities.jutsus.MudWallAbility();
        assertTrue(mudWall.handleCost(null, data, 0));
        assertEquals(200.0f - mudWall.getFinalChakraCost(null, data), data.getChakra(), 0.01f);

        data.setChakra(200.0f);
        var kirin = new com.sekwah.narutomod.abilities.jutsus.KirinAbility();
        assertTrue(kirin.handleCost(null, data, 0));
        assertEquals(200.0f - kirin.getFinalChakraCost(null, data), data.getChakra(), 0.01f);
        assertEquals(200.0f - kirin.getStaminaCost(), data.getStamina(), 0.01f);
    }
}
