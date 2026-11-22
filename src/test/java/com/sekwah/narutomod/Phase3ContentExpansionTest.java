package com.sekwah.narutomod;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.jutsus.AshPileBurningAbility;
import com.sekwah.narutomod.abilities.jutsus.ChakraScalpelAbility;
import com.sekwah.narutomod.abilities.jutsus.WaterEncampmentWallAbility;
import com.sekwah.narutomod.capabilities.JutsuUnlockRules;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.item.MedicalPouchItem;
import com.sekwah.narutomod.item.weapons.GunbaiUchiwaItem;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JUnit tests for Phase 3 Content Expansion:
 * - Katon: Haisekisho (Ash Pile Burning)
 * - Suiton: Suijinheki (Water Encampment Wall)
 * - Chakura no Mesu (Chakra Scalpel)
 * - Gunbai Uchiwa reflection & fire bonus math
 * - Medical Shinobi Pouch cleanse & treatment rules
 */
public class Phase3ContentExpansionTest {

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Verify Phase 3 jutsu properties, unlock ranks, and mastery scaling")
    void testPhase3JutsusMetadataAndUnlocks() {
        AshPileBurningAbility ash = new AshPileBurningAbility();
        WaterEncampmentWallAbility waterWall = new WaterEncampmentWallAbility();
        ChakraScalpelAbility scalpel = new ChakraScalpelAbility();

        assertEquals(ChakraElement.FIRE, ash.getChakraElement());
        assertEquals(NinjaRank.JONIN, ash.getRequiredRank());
        assertEquals(1231L, ash.defaultCombo());
        assertEquals(36.0f, ash.getDefaultChakraCost());
        assertEquals(180, ash.getDefaultCooldown());
        assertEquals(20.0f, ash.getDefaultDamage());
        assertFalse(ash.requiresScroll());
        assertNull(ash.getRequiredClanPath());
        assertEquals(80, AshPileBurningAbility.getScaledDuration(0));
        assertTrue(AshPileBurningAbility.getScaledDuration(100) > 80);

        assertEquals(ChakraElement.WATER, waterWall.getChakraElement());
        assertEquals(NinjaRank.CHUNIN, waterWall.getRequiredRank());
        assertEquals(223L, waterWall.defaultCombo());
        assertEquals(28.0f, waterWall.getDefaultChakraCost());
        assertEquals(160, waterWall.getDefaultCooldown());
        assertEquals(10.0f, waterWall.getDefaultDamage());
        assertFalse(waterWall.requiresScroll());
        assertNull(waterWall.getRequiredClanPath());

        Vec3 wallOrigin = WaterEncampmentWallAbility.computeWallOrigin(new Vec3(10.0D, 64.0D, 10.0D), 0.0F);
        assertEquals(10.0D, wallOrigin.x, 1.0E-4D);
        assertEquals(10.0D + WaterEncampmentWallAbility.WALL_OFFSET, wallOrigin.z, 1.0E-4D);

        assertEquals(ChakraElement.NONE, scalpel.getChakraElement());
        assertEquals(NinjaRank.CHUNIN, scalpel.getRequiredRank());
        assertEquals(331L, scalpel.defaultCombo());
        assertEquals(26.0f, scalpel.getDefaultChakraCost());
        assertEquals(180, scalpel.getDefaultCooldown());
        assertEquals(9.0f, scalpel.getDefaultDamage());
        assertFalse(scalpel.requiresScroll());
        assertNull(scalpel.getRequiredClanPath());
        assertEquals(200, ChakraScalpelAbility.getScaledHoldDuration(0));
        assertEquals(217, ChakraScalpelAbility.getScaledHoldDuration(100));
        assertEquals(320, ChakraScalpelAbility.getScaledHoldDuration(700));

        assertTrue(ChakraScalpelAbility.isStrikeCooldownReady(100, -1));
        assertFalse(ChakraScalpelAbility.isStrikeCooldownReady(105, 100));
        assertTrue(ChakraScalpelAbility.isStrikeCooldownReady(108, 100));

        // Rank unlock checks without scrolls
        NinjaData genin = new NinjaData(true);
        genin.setRank(NinjaRank.GENIN.name());
        assertFalse(JutsuUnlockRules.checkAbilityUnlocked(genin, waterWall, Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "water_encampment_wall")));
        assertFalse(JutsuUnlockRules.checkAbilityUnlocked(genin, scalpel, Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "chakra_scalpel")));
        assertFalse(JutsuUnlockRules.checkAbilityUnlocked(genin, ash, Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "ash_pile_burning")));

        NinjaData chunin = new NinjaData(true);
        chunin.setRank(NinjaRank.CHUNIN.name());
        assertTrue(JutsuUnlockRules.checkAbilityUnlocked(chunin, waterWall, Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "water_encampment_wall")));
        assertTrue(JutsuUnlockRules.checkAbilityUnlocked(chunin, scalpel, Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "chakra_scalpel")));
        assertFalse(JutsuUnlockRules.checkAbilityUnlocked(chunin, ash, Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "ash_pile_burning")));

        NinjaData jonin = new NinjaData(true);
        jonin.setRank(NinjaRank.JONIN.name());
        assertTrue(JutsuUnlockRules.checkAbilityUnlocked(jonin, ash, Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "ash_pile_burning")));
    }

    @Test
    @DisplayName("Verify Gunbai Uchiwa reflection velocity and fire bonus damage helpers")
    void testGunbaiUchiwaHelpers() {
        Vec3 look = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 incoming = new Vec3(0.0D, 0.0D, -1.5D);
        Vec3 reflected = GunbaiUchiwaItem.computeReflectVelocity(look, incoming);
        assertEquals(0.0D, reflected.x, 1.0E-4D);
        assertEquals(0.0D, reflected.y, 1.0E-4D);
        assertEquals(1.5D * GunbaiUchiwaItem.REFLECT_SPEED_MULTIPLIER, reflected.z, 1.0E-4D);

        Vec3 slowIncoming = new Vec3(0.2D, 0.0D, 0.0D);
        Vec3 minClamped = GunbaiUchiwaItem.computeReflectVelocity(look, slowIncoming);
        assertEquals(1.25D, minClamped.length(), 1.0E-4D);

        assertEquals(GunbaiUchiwaItem.IGNITED_BONUS_DAMAGE, GunbaiUchiwaItem.computeBonusFireDamage(true));
        assertEquals(0.0F, GunbaiUchiwaItem.computeBonusFireDamage(false));
    }

    @Test
    @DisplayName("Verify Medical Shinobi Pouch cure list, treatment rules, and null guards")
    void testMedicalPouchHelpers() {
        var cures = MedicalPouchItem.getCureEffects();
        assertTrue(cures.contains(MobEffects.POISON));
        assertTrue(cures.contains(MobEffects.WITHER));
        assertTrue(cures.contains(MobEffects.SLOWNESS));
        assertTrue(cures.contains(MobEffects.WEAKNESS));
        assertTrue(cures.contains(MobEffects.BLINDNESS));
        assertTrue(cures.contains(MobEffects.NAUSEA));
        assertTrue(cures.contains(MobEffects.DARKNESS));

        assertFalse(MedicalPouchItem.canTreatTarget(20.0F, 20.0F, false));
        assertTrue(MedicalPouchItem.canTreatTarget(14.0F, 20.0F, false));
        assertTrue(MedicalPouchItem.canTreatTarget(20.0F, 20.0F, true));

        assertFalse(MedicalPouchItem.hasAnyCurableEffect(null));
        assertEquals(0, MedicalPouchItem.cleanseEffects(null));
    }
}
