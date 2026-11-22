package com.sekwah.narutomod;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.jutsus.AmaterasuAbility;
import com.sekwah.narutomod.abilities.jutsus.ChidoriAbility;
import com.sekwah.narutomod.abilities.jutsus.EightGatesAbility;
import com.sekwah.narutomod.abilities.jutsus.RasenganAbility;
import com.sekwah.narutomod.abilities.jutsus.ShadowCloneAbility;
import com.sekwah.narutomod.abilities.jutsus.SharinganAbility;
import com.sekwah.narutomod.abilities.jutsus.SusanooRibcageAbility;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying default parameters, mastery scaling, and rank/combo
 * metadata for core jutsu abilities.
 */
public class CoreJutsuAbilitiesTest {

    @Test
    @DisplayName("Rasengan defaults and mastery hold-duration scaling")
    void testRasenganDefaultsAndScaling() {
        RasenganAbility ability = new RasenganAbility();
        assertEquals(Ability.ActivationType.INSTANT, ability.activationType());
        assertEquals(NinjaRank.CHUNIN, ability.getRequiredRank());
        assertEquals(2123L, ability.defaultCombo());
        assertEquals(40.0f, ability.getDefaultChakraCost(), 0.001f);
        assertEquals(160, ability.getDefaultCooldown());
        assertEquals(22.0f, ability.getDefaultDamage(), 0.001f);

        assertEquals(100, RasenganAbility.getScaledHoldDuration(0));
        assertEquals(100, RasenganAbility.getScaledHoldDuration(-10));
        assertEquals(109, RasenganAbility.getScaledHoldDuration(100));
        assertEquals(160, RasenganAbility.getScaledHoldDuration(700));
        assertEquals(160, RasenganAbility.getScaledHoldDuration(1400));
    }

    @Test
    @DisplayName("Chidori defaults, element, and mastery hold-duration scaling")
    void testChidoriDefaultsAndScaling() {
        ChidoriAbility ability = new ChidoriAbility();
        assertEquals(Ability.ActivationType.INSTANT, ability.activationType());
        assertEquals(ChakraElement.LIGHTNING, ability.getChakraElement());
        assertEquals(NinjaRank.CHUNIN, ability.getRequiredRank());
        assertEquals(1312L, ability.defaultCombo());
        assertEquals(35.0f, ability.getDefaultChakraCost(), 0.001f);
        assertEquals(120, ability.getDefaultCooldown());
        assertEquals(26.0f, ability.getDefaultDamage(), 0.001f);

        assertEquals(100, ChidoriAbility.getScaledHoldDuration(0));
        assertEquals(109, ChidoriAbility.getScaledHoldDuration(100));
        assertEquals(160, ChidoriAbility.getScaledHoldDuration(700));
        assertEquals(160, ChidoriAbility.getScaledHoldDuration(1400));
    }

    @Test
    @DisplayName("ShadowClone defaults, clone count, and lifespan scaling by mastery")
    void testShadowCloneDefaultsAndScaling() {
        ShadowCloneAbility ability = new ShadowCloneAbility();
        assertEquals(Ability.ActivationType.INSTANT, ability.activationType());
        assertEquals(NinjaRank.CHUNIN, ability.getRequiredRank());
        assertEquals(3123L, ability.defaultCombo());
        assertEquals(30.0f, ability.getDefaultChakraCost(), 0.001f);
        assertEquals(240, ability.getDefaultCooldown());

        assertEquals(1, ShadowCloneAbility.getCloneCount(0));
        assertEquals(2, ShadowCloneAbility.getCloneCount(100));
        assertEquals(4, ShadowCloneAbility.getCloneCount(300));
        assertEquals(8, ShadowCloneAbility.getCloneCount(700));

        assertEquals(400, ShadowCloneAbility.getCloneLifespan(0));
        assertEquals(600, ShadowCloneAbility.getCloneLifespan(100));
        assertEquals(900, ShadowCloneAbility.getCloneLifespan(300));
        assertEquals(1200, ShadowCloneAbility.getCloneLifespan(700));
    }

    @Test
    @DisplayName("EightGatesAbility.canActivate enforces JONIN rank requirement")
    void testEightGatesCanActivateEnforcesRank() {
        EightGatesAbility eightGates = new EightGatesAbility();
        eightGates.setRegistryName(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "eight_gates"));

        NinjaData ninjaData = new NinjaData(true);
        ninjaData.setRank("ACADEMY_STUDENT");
        assertFalse(eightGates.canActivate(null, ninjaData), "ACADEMY_STUDENT must not activate Eight Gates");

        ninjaData.setRank("GENIN");
        assertFalse(eightGates.canActivate(null, ninjaData), "GENIN must not activate Eight Gates");

        ninjaData.setRank("CHUNIN");
        assertFalse(eightGates.canActivate(null, ninjaData), "CHUNIN must not activate Eight Gates");

        ninjaData.setRank("JONIN");
        assertTrue(eightGates.canActivate(null, ninjaData), "JONIN must be allowed to activate Eight Gates");
    }

    @Test
    @DisplayName("SharinganStage and GateStage translation keys are properly formatted and non-empty")
    void testStageTranslationKeys() {
        for (SharinganAbility.SharinganStage stage : SharinganAbility.SharinganStage.values()) {
            assertNotNull(stage.getId());
            assertFalse(stage.getId().isBlank());
            assertNotNull(stage.getDisplayName());
            assertFalse(stage.getDisplayName().isBlank());
            String key = stage.getTranslationKey();
            assertNotNull(key);
            assertFalse(key.isBlank());
            assertTrue(key.startsWith("jutsu.sharingan.stage."));
            assertEquals("jutsu.sharingan.stage." + stage.getId(), key);
        }

        for (EightGatesAbility.GateStage stage : EightGatesAbility.GateStage.values()) {
            assertNotNull(stage.getId());
            assertFalse(stage.getId().isBlank());
            assertNotNull(stage.getDisplayName());
            assertFalse(stage.getDisplayName().isBlank());
            assertFalse(stage.getDisplayName().contains("Kapı"), "GateStage fallback label should not use hardcoded Turkish");
            String key = stage.getTranslationKey();
            assertNotNull(key);
            assertFalse(key.isBlank());
            assertTrue(key.startsWith("jutsu.eight_gates.stage."));
            assertEquals("jutsu.eight_gates.stage." + stage.getId(), key);
        }
    }

    @Test
    @DisplayName("SusanooRibcageAbility initial vs sustained cost and elemental hat / getFinalChakraCost compatibility")
    void testSusanooInitialVsSustainedCostAndHatDiscount() {
        Identifier susanooId = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "susanoo_ribcage");
        Identifier sharinganId = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "sharingan");
        SusanooRibcageAbility susanoo = new SusanooRibcageAbility();
        susanoo.setRegistryName(susanooId);

        NinjaData ninjaData = new NinjaData(true);
        ninjaData.setRank("JONIN");
        ninjaData.forceSetClan(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "uchiha"));
        ninjaData.unlockAbility(sharinganId);
        ninjaData.addMastery(sharinganId, 5000);
        ninjaData.setBaseMaxChakra(200.0f);
        ninjaData.setChakra(200.0f);

        // Simulate toggle-on: ability is added to ToggleAbilityData with activeTicks == 0 before tick 1 handleCost
        ninjaData.getToggleAbilityData().addAbilityStarted(null, ninjaData, susanoo);
        assertEquals(0, ninjaData.getToggleAbilityData().getActiveTicks(susanooId));
        assertTrue(susanoo.handleCost(null, ninjaData, 0));
        assertEquals(160.0f, ninjaData.getChakra(), 0.001f, "First tick must deduct 40f initial activation cost");

        // After tick 1 increments activeTicks, subsequent ticks must deduct sustained 1.0f cost
        assertEquals(1, ninjaData.getToggleAbilityData().incrementAbilityTicks(susanooId));
        assertTrue(susanoo.handleCost(null, ninjaData, 0));
        assertEquals(159.0f, ninjaData.getChakra(), 0.001f, "Sustained tick must deduct 1.0f chakra");

        // Elemental hat discount & getFinalChakraCost compatibility
        assertEquals(0.0f, Ability.applyElementalHatDiscount(ChakraElement.FIRE, ChakraElement.FIRE, 0.0f), 0.001f);
        assertEquals(85.0f, Ability.applyElementalHatDiscount(ChakraElement.FIRE, ChakraElement.FIRE, 100.0f), 0.001f);
        assertEquals(100.0f, Ability.applyElementalHatDiscount(ChakraElement.WATER, ChakraElement.FIRE, 100.0f), 0.001f);

        AmaterasuAbility amaterasu = new AmaterasuAbility();
        amaterasu.setRegistryName(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "amaterasu"));
        assertEquals(amaterasu.getScaledChakraCost(ninjaData), amaterasu.getFinalChakraCost(null, ninjaData), 0.001f);
        assertEquals(0.0f, susanoo.getFinalChakraCost(null, ninjaData), 0.001f);
    }

    @Test
    @DisplayName("Seal timeout mastery scaling and default 1000ms config")
    void testSealTimeoutMasteryScaling() {
        assertEquals(1000, com.sekwah.narutomod.config.NarutoConfig.sealTimeoutMs);
        assertEquals(20, Ability.getScaledSealTimeoutTicks(20, 0));
        assertEquals(20, Ability.getScaledSealTimeoutTicks(20, -15));
        assertEquals(12, Ability.getScaledSealTimeoutTicks(20, 350));
        assertEquals(4, Ability.getScaledSealTimeoutTicks(20, 700));
        assertEquals(4, Ability.getScaledSealTimeoutTicks(20, 1200));
        assertEquals(4, Ability.getScaledSealTimeoutTicks(2, 0));
    }

    @Test
    @DisplayName("Jutsu range helpers scale properly with mastery and Byakugan")
    void testJutsuRangeHelpers() {
        assertEquals(18.0D, com.sekwah.narutomod.abilities.jutsus.AshPileBurningAbility.getScaledRange(0), 0.001D);
        assertEquals(20.0D, com.sekwah.narutomod.abilities.jutsus.AshPileBurningAbility.getScaledRange(350), 0.001D);
        assertEquals(22.0D, com.sekwah.narutomod.abilities.jutsus.AshPileBurningAbility.getScaledRange(700), 0.001D);

        assertEquals(22.0D, com.sekwah.narutomod.abilities.jutsus.StoneSpikesAbility.getScaledRange(0), 0.001D);
        assertEquals(26.0D, com.sekwah.narutomod.abilities.jutsus.StoneSpikesAbility.getScaledRange(700), 0.001D);

        assertEquals(22.0D, com.sekwah.narutomod.abilities.jutsus.AdamantineChainsAbility.getScaledRange(0), 0.001D);
        assertEquals(26.0D, com.sekwah.narutomod.abilities.jutsus.AdamantineChainsAbility.getScaledRange(700), 0.001D);

        assertEquals(20.0D, com.sekwah.narutomod.abilities.jutsus.WoodPrisonAbility.getScaledRange(0), 0.001D);
        assertEquals(24.0D, com.sekwah.narutomod.abilities.jutsus.WoodPrisonAbility.getScaledRange(700), 0.001D);

        assertEquals(22, com.sekwah.narutomod.abilities.jutsus.HakkeKushoAbility.getScaledRange(false));
        assertEquals(26, com.sekwah.narutomod.abilities.jutsus.HakkeKushoAbility.getScaledRange(true));

        assertEquals(20.0D, com.sekwah.narutomod.abilities.jutsus.ShadowPossessionAbility.getScaledRange(0), 0.001D);
        assertEquals(25.0D, com.sekwah.narutomod.abilities.jutsus.ShadowPossessionAbility.getScaledRange(350), 0.001D);
        assertEquals(30.0D, com.sekwah.narutomod.abilities.jutsus.ShadowPossessionAbility.getScaledRange(700), 0.001D);

        net.minecraft.world.phys.Vec3 anchor = com.sekwah.narutomod.abilities.JutsuTargetingHelper.computeMissAnchor(
                new net.minecraft.world.phys.Vec3(0, 0, 0),
                new net.minecraft.world.phys.Vec3(0, 0, 1),
                20.0D);
        assertEquals(17.0D, anchor.z, 0.001D);
    }

    @Test
    @DisplayName("Item 3: Ability mastery progress, clamped duration ticks, Fireball cast delay, and MysticalPalm heal range")
    void testItem3MasteryProgressAndJutsuScaling() {
        assertEquals(0.0D, Ability.getMasteryProgress(-25), 0.0001D);
        assertEquals(0.0D, Ability.getMasteryProgress(0), 0.0001D);
        assertEquals(0.5D, Ability.getMasteryProgress(350), 0.0001D);
        assertEquals(1.0D, Ability.getMasteryProgress(700), 0.0001D);
        assertEquals(1.0D, Ability.getMasteryProgress(2000), 0.0001D);

        assertEquals(100, Ability.getScaledDurationTicks(100, 0, 0.60D));
        assertEquals(130, Ability.getScaledDurationTicks(100, 350, 0.60D));
        assertEquals(160, Ability.getScaledDurationTicks(100, 700, 0.60D));
        assertEquals(160, Ability.getScaledDurationTicks(100, 1500, 0.60D));

        assertEquals(10, com.sekwah.narutomod.abilities.jutsus.FireballJutsuAbility.getScaledCastDelayTicks(0));
        assertEquals(6, com.sekwah.narutomod.abilities.jutsus.FireballJutsuAbility.getScaledCastDelayTicks(350));
        assertEquals(2, com.sekwah.narutomod.abilities.jutsus.FireballJutsuAbility.getScaledCastDelayTicks(700));
        assertEquals(2, com.sekwah.narutomod.abilities.jutsus.FireballJutsuAbility.getScaledCastDelayTicks(1500));

        assertEquals(4.5D, com.sekwah.narutomod.abilities.jutsus.MysticalPalmAbility.getScaledHealRange(0), 0.001D);
        assertEquals(5.85D, com.sekwah.narutomod.abilities.jutsus.MysticalPalmAbility.getScaledHealRange(350), 0.001D);
        assertEquals(7.2D, com.sekwah.narutomod.abilities.jutsus.MysticalPalmAbility.getScaledHealRange(700), 0.001D);
        assertEquals(7.2D, com.sekwah.narutomod.abilities.jutsus.MysticalPalmAbility.getScaledHealRange(1500), 0.001D);

        assertEquals(200, com.sekwah.narutomod.abilities.jutsus.ChakraScalpelAbility.getScaledHoldDuration(0));
        assertEquals(320, com.sekwah.narutomod.abilities.jutsus.ChakraScalpelAbility.getScaledHoldDuration(700));
        assertEquals(320, com.sekwah.narutomod.abilities.jutsus.ChakraScalpelAbility.getScaledHoldDuration(1500));

        assertEquals(80, com.sekwah.narutomod.abilities.jutsus.AshPileBurningAbility.getScaledDuration(0));
        assertEquals(128, com.sekwah.narutomod.abilities.jutsus.AshPileBurningAbility.getScaledDuration(700));
        assertEquals(128, com.sekwah.narutomod.abilities.jutsus.AshPileBurningAbility.getScaledDuration(1500));

        assertEquals(30, com.sekwah.narutomod.abilities.jutsus.ShadowPossessionAbility.getScaledHoldDuration(0));
        assertEquals(60, com.sekwah.narutomod.abilities.jutsus.ShadowPossessionAbility.getScaledHoldDuration(700));
        assertEquals(60, com.sekwah.narutomod.abilities.jutsus.ShadowPossessionAbility.getScaledHoldDuration(1500));

        assertEquals(22, com.sekwah.narutomod.abilities.jutsus.HakkeKushoAbility.getScaledRange(false, 0));
        assertEquals(26, com.sekwah.narutomod.abilities.jutsus.HakkeKushoAbility.getScaledRange(false, 700));
        assertEquals(26, com.sekwah.narutomod.abilities.jutsus.HakkeKushoAbility.getScaledRange(true, 0));
        assertEquals(30, com.sekwah.narutomod.abilities.jutsus.HakkeKushoAbility.getScaledRange(true, 700));
    }
}
