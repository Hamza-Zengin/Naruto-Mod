package com.sekwah.narutomod;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.jutsus.AmaterasuAbility;
import com.sekwah.narutomod.abilities.jutsus.EarthDragonBulletAbility;
import com.sekwah.narutomod.abilities.jutsus.FireballJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.StoneSpikesAbility;
import com.sekwah.narutomod.abilities.jutsus.VacuumSpheresAbility;
import com.sekwah.narutomod.abilities.jutsus.WaterDragonAbility;
import com.sekwah.narutomod.abilities.utility.ChakraChargeAbility;
import com.sekwah.narutomod.capabilities.ChakraStaminaLogic;
import com.sekwah.narutomod.capabilities.JutsuUnlockRules;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.events.PlayerEvents;
import com.sekwah.narutomod.item.BlankJutsuScrollItem;
import com.sekwah.narutomod.item.SealedAmmoContents;
import com.sekwah.narutomod.item.weapons.AnbuTantoItem;
import com.sekwah.narutomod.item.weapons.ChakraBladesItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests covering the expansion features:
 * - 4 new elemental jutsus (combos, ranks, elements, hybrid prereq/scroll unlock rules)
 * - ANBU Tanto backstab geometry & Chakra Blades constants
 * - Sealed Ammo Scroll immutable container & Blank Jutsu Scroll eligibility filters
 * - Armor passives (Kage Hat +60 Max Chakra, Jonin Vest -1t hand-seal delay, Warring States -15% damage)
 */
public class ExpansionFeaturesTest {

    @Test
    @DisplayName("Verify metadata and combos for the 4 new elemental jutsus")
    void testNewElementalJutsuMetadata() {
        EarthDragonBulletAbility doryudan = new EarthDragonBulletAbility();
        assertEquals(ChakraElement.EARTH, doryudan.getChakraElement());
        assertEquals(NinjaRank.CHUNIN, doryudan.getRequiredRank());
        assertEquals(231L, doryudan.defaultCombo());

        VacuumSpheresAbility shinkugyoku = new VacuumSpheresAbility();
        assertEquals(ChakraElement.WIND, shinkugyoku.getChakraElement());
        assertEquals(NinjaRank.CHUNIN, shinkugyoku.getRequiredRank());
        assertEquals(221L, shinkugyoku.defaultCombo());

        StoneSpikesAbility doryuso = new StoneSpikesAbility();
        assertEquals(ChakraElement.EARTH, doryuso.getChakraElement());
        assertEquals(NinjaRank.JONIN, doryuso.getRequiredRank());
        assertEquals(2312L, doryuso.defaultCombo());
        assertTrue(doryuso.getPrerequisites().contains(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "mud_wall")));
        assertEquals(150, doryuso.getRequiredMastery());

        WaterDragonAbility suiryudan = new WaterDragonAbility();
        assertEquals(ChakraElement.WATER, suiryudan.getChakraElement());
        assertEquals(NinjaRank.JONIN, suiryudan.getRequiredRank());
        assertEquals(2121L, suiryudan.defaultCombo());
        assertTrue(suiryudan.getPrerequisites().contains(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "water_bullet")));
        assertEquals(150, suiryudan.getRequiredMastery());
    }

    @Test
    @DisplayName("Verify hybrid scroll OR prerequisite unlock rules for Stone Spikes and Water Dragon")
    void testHybridScrollOrPrerequisiteUnlock() {
        Identifier stoneSpikesId = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "stone_spikes");
        Identifier mudWallId = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "mud_wall");
        StoneSpikesAbility stoneSpikes = new StoneSpikesAbility();

        // Low-rank shinobi without scroll or prereq cannot cast
        NinjaData genin = new NinjaData(true);
        genin.setRank(NinjaRank.GENIN.name());
        assertFalse(JutsuUnlockRules.checkAbilityUnlocked(genin, stoneSpikes, stoneSpikesId));

        // Scroll unlock works immediately even at Chunin rank
        NinjaData chuninWithScroll = new NinjaData(true);
        chuninWithScroll.setRank(NinjaRank.CHUNIN.name());
        chuninWithScroll.unlockAbility(stoneSpikesId);
        assertTrue(JutsuUnlockRules.checkAbilityUnlocked(chuninWithScroll, stoneSpikes, stoneSpikesId));

        // Jonin with 150 Mud Wall mastery unlocks naturally without scroll
        NinjaData joninWithPrereq = new NinjaData(true);
        joninWithPrereq.setRank(NinjaRank.JONIN.name());
        joninWithPrereq.unlockAbility(mudWallId);
        joninWithPrereq.addMastery(mudWallId, 150);
        assertTrue(JutsuUnlockRules.checkAbilityUnlocked(joninWithPrereq, stoneSpikes, stoneSpikesId));

        // Jonin with insufficient Mud Wall mastery remains locked
        NinjaData joninLowMastery = new NinjaData(true);
        joninLowMastery.setRank(NinjaRank.JONIN.name());
        joninLowMastery.unlockAbility(mudWallId);
        joninLowMastery.addMastery(mudWallId, 100);
        assertFalse(JutsuUnlockRules.checkAbilityUnlocked(joninLowMastery, stoneSpikes, stoneSpikesId));
    }

    @Test
    @DisplayName("Verify ANBU Tanto backstab cone geometry and Chakra Blades Wind Flow reach config")
    void testWeaponMechanics() {
        Vec3 targetPos = new Vec3(0.0, 64.0, 0.0);
        Vec3 targetFacingSouth = new Vec3(0.0, 0.0, 1.0); // Facing +Z

        // Attacker behind target (-Z) -> backstab
        assertTrue(AnbuTantoItem.isBehindTarget(new Vec3(0.0, 64.0, -2.0), targetPos, targetFacingSouth));
        // Diagonal back (-Z, +X) -> backstab
        assertTrue(AnbuTantoItem.isBehindTarget(new Vec3(1.0, 64.0, -2.0), targetPos, targetFacingSouth));
        // Attacker in front (+Z) -> not behind
        assertFalse(AnbuTantoItem.isBehindTarget(new Vec3(0.0, 64.0, 2.0), targetPos, targetFacingSouth));
        // Attacker directly perpendicular -> not behind (dot = 0)
        assertFalse(AnbuTantoItem.isBehindTarget(new Vec3(2.0, 64.0, 0.0), targetPos, targetFacingSouth));

        assertEquals(1.50F, AnbuTantoItem.BACKSTAB_MULTIPLIER, 0.001F);
        assertEquals(200, ChakraBladesItem.ACTIVE_DURATION_TICKS);
        assertEquals(1.5D, ChakraBladesItem.WIND_FLOW_EXTRA_REACH, 0.001D);
    }

    @Test
    @DisplayName("Verify SealedAmmoContents immutability, clamping at 64, and non-empty slot cycling")
    void testSealedAmmoContentsBehavior() {
        SealedAmmoContents empty = SealedAmmoContents.EMPTY;
        assertEquals(0, empty.getTotalCount());
        assertEquals(SealedAmmoContents.AmmoType.KUNAI, empty.getSelectedType());

        // Clamps above MAX_PER_TYPE (64) and below 0
        SealedAmmoContents filled = empty
                .withCount(SealedAmmoContents.AmmoType.KUNAI, 100)
                .withCount(SealedAmmoContents.AmmoType.SENBON, 24)
                .withCount(SealedAmmoContents.AmmoType.SMOKE_BOMB, -5);

        assertEquals(64, filled.getCount(SealedAmmoContents.AmmoType.KUNAI));
        assertEquals(24, filled.getCount(SealedAmmoContents.AmmoType.SENBON));
        assertEquals(0, filled.getCount(SealedAmmoContents.AmmoType.SMOKE_BOMB));
        assertEquals(88, filled.getTotalCount());

        // Cycling skips empty SHURIKEN and lands on SENBON, then skips EXPLOSIVE_KUNAI/SMOKE_BOMB back to KUNAI
        SealedAmmoContents next1 = filled.cycleSelected();
        assertEquals(SealedAmmoContents.AmmoType.SENBON, next1.getSelectedType());
        SealedAmmoContents next2 = next1.cycleSelected();
        assertEquals(SealedAmmoContents.AmmoType.KUNAI, next2.getSelectedType());
    }

    @Test
    @DisplayName("Verify BlankJutsuScrollItem Tier 4+ and non-clan transcription eligibility rules")
    void testBlankJutsuScrollEligibility() {
        NinjaData data = new NinjaData(true);
        data.setRank(NinjaRank.CHUNIN.name());

        Identifier fireballId = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "fireball");
        FireballJutsuAbility fireball = new FireballJutsuAbility();

        // Below Tier 4 (350 mastery) -> not eligible
        data.addMastery(fireballId, 349);
        assertFalse(BlankJutsuScrollItem.isTranscribableJutsu(fireballId, fireball, data));

        // Reaching 350+ mastery -> Tier 4 -> eligible
        data.addMastery(fireballId, 1);
        assertTrue(Ability.getMasteryTier(data.getMastery(fireballId)) >= 4);
        assertTrue(BlankJutsuScrollItem.isTranscribableJutsu(fireballId, fireball, data));

        // Clan-restricted jutsu (Amaterasu) must be rejected even with 1000 mastery
        NinjaData uchiha = new NinjaData(true);
        uchiha.setClan(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "uchiha"));
        uchiha.setRank(NinjaRank.KAGE.name());
        Identifier amaterasuId = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "amaterasu");
        uchiha.unlockAbility(amaterasuId);
        uchiha.addMastery(amaterasuId, 1000);
        assertFalse(BlankJutsuScrollItem.isTranscribableJutsu(amaterasuId, new AmaterasuAbility(), uchiha));

        // Utility with 0 chakraCost (Chakra Charge) must be rejected
        Identifier chargeId = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "chakra_charge");
        uchiha.addMastery(chargeId, 1000);
        assertFalse(BlankJutsuScrollItem.isTranscribableJutsu(chargeId, new ChakraChargeAbility(), uchiha));
    }

    @Test
    @DisplayName("Verify passive armor bonuses: Kage Hat +60 Max Chakra, Jonin Vest -1t delay, Warring States -15% damage")
    void testArmorPassivesMath() {
        // Kage Hat +60 Max Chakra
        float withoutHat = ChakraStaminaLogic.computeMaxChakra(100.0F, NinjaRank.KAGE.name(),
                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "shinobi"), false);
        float withHat = ChakraStaminaLogic.computeMaxChakra(100.0F, NinjaRank.KAGE.name(),
                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "shinobi"), true);
        assertEquals(60.0F, withHat - withoutHat, 0.001F);

        // Jonin Vest -1 tick hand-seal delay (minimum 0)
        assertEquals(3, Ability.applyArmorDelayReduction(true, 4));
        assertEquals(4, Ability.applyArmorDelayReduction(false, 4));
        assertEquals(0, Ability.applyArmorDelayReduction(true, 1));
        assertEquals(0, Ability.applyArmorDelayReduction(true, 0));

        // Warring States Armor -15% incoming jutsu/explosion damage
        assertEquals(17.0F, PlayerEvents.applyWarringStatesReduction(20.0F, true, true), 0.001F);
        assertEquals(20.0F, PlayerEvents.applyWarringStatesReduction(20.0F, false, true), 0.001F);
        assertEquals(20.0F, PlayerEvents.applyWarringStatesReduction(20.0F, true, false), 0.001F);
    }
}
