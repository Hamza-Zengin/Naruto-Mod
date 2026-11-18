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

    @Test
    @DisplayName("Verify Phase 1 new jutsus (Phoenix Flower, Chidori Stream, Mystical Palm) metadata and hybrid unlock")
    void testPhase1JutsuExpansion() {
        var phoenix = new com.sekwah.narutomod.abilities.jutsus.PhoenixFlowerAbility();
        assertEquals(ChakraElement.FIRE, phoenix.getChakraElement());
        assertEquals(NinjaRank.CHUNIN, phoenix.getRequiredRank());
        assertEquals(Ability.ActivationType.INSTANT, phoenix.activationType());
        assertEquals(131L, phoenix.defaultCombo());

        var chidoriStream = new com.sekwah.narutomod.abilities.jutsus.ChidoriStreamAbility();
        assertEquals(ChakraElement.LIGHTNING, chidoriStream.getChakraElement());
        assertEquals(NinjaRank.JONIN, chidoriStream.getRequiredRank());
        assertEquals(Ability.ActivationType.INSTANT, chidoriStream.activationType());
        assertEquals(1313L, chidoriStream.defaultCombo());
        assertTrue(chidoriStream.getPrerequisites().contains(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "chidori")));
        assertEquals(150, chidoriStream.getRequiredMastery());

        var mysticalPalm = new com.sekwah.narutomod.abilities.jutsus.MysticalPalmAbility();
        assertEquals(ChakraElement.NONE, mysticalPalm.getChakraElement());
        assertEquals(NinjaRank.CHUNIN, mysticalPalm.getRequiredRank());
        assertEquals(Ability.ActivationType.CHANNELED, mysticalPalm.activationType());
        assertEquals(332L, mysticalPalm.defaultCombo());
        assertFalse(mysticalPalm.canActivateBelowMinCharge());

        Identifier streamId = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "chidori_stream");
        Identifier chidoriId = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "chidori");

        NinjaData joninWithChidori = new NinjaData(true);
        joninWithChidori.setRank(NinjaRank.JONIN.name());
        joninWithChidori.unlockAbility(chidoriId);
        joninWithChidori.addMastery(chidoriId, 150);
        assertTrue(JutsuUnlockRules.checkAbilityUnlocked(joninWithChidori, chidoriStream, streamId));

        NinjaData chuninWithScroll = new NinjaData(true);
        chuninWithScroll.setRank(NinjaRank.CHUNIN.name());
        chuninWithScroll.unlockAbility(streamId);
        assertTrue(JutsuUnlockRules.checkAbilityUnlocked(chuninWithScroll, chidoriStream, streamId));
    }

    @Test
    @DisplayName("Verify Phoenix Flower 90-degree equal 4-bolt spread and Chidori Stream mastery field duration scaling")
    void testPhoenixFlowerAndChidoriStreamRevisions() {
        float[] offsets = com.sekwah.narutomod.abilities.jutsus.PhoenixFlowerAbility.YAW_OFFSETS;
        assertArrayEquals(new float[]{-45.0F, -15.0F, 15.0F, 45.0F}, offsets, 0.001F);

        Vec3 baseLook = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3[] dirs = new Vec3[offsets.length];
        for (int i = 0; i < offsets.length; i++) {
            dirs[i] = com.sekwah.narutomod.abilities.jutsus.PhoenixFlowerAbility.computeRotatedDirection(baseLook, offsets[i]);
            assertEquals(1.0D, dirs[i].length(), 0.0001D);
        }

        double expectedStepCos = Math.cos(Math.toRadians(30.0D));
        for (int i = 0; i < dirs.length - 1; i++) {
            assertEquals(expectedStepCos, dirs[i].dot(dirs[i + 1]), 0.001D,
                    "Adjacent Phoenix Flower fireballs must be spaced by 30 degrees");
        }
        assertEquals(Math.cos(Math.toRadians(90.0D)), dirs[0].dot(dirs[3]), 0.001D,
                "Outer Phoenix Flower fireballs must span 90 degrees");

        // Chidori Stream persistent electric field duration: 30 ticks base + 8 ticks per Mastery Tier (Tier 1..5 -> 38..70 ticks = 1.9s..3.5s)
        assertEquals(38, com.sekwah.narutomod.abilities.jutsus.ChidoriStreamAbility.computeFieldDurationTicks(0));
        assertTrue(com.sekwah.narutomod.abilities.jutsus.ChidoriStreamAbility.computeFieldDurationTicks(0) >= 20,
                "Base electric field duration must be at least 1 second (20 ticks)");
        assertEquals(46, com.sekwah.narutomod.abilities.jutsus.ChidoriStreamAbility.computeFieldDurationTicks(50));
        assertEquals(54, com.sekwah.narutomod.abilities.jutsus.ChidoriStreamAbility.computeFieldDurationTicks(150));
        assertEquals(62, com.sekwah.narutomod.abilities.jutsus.ChidoriStreamAbility.computeFieldDurationTicks(350));
        assertEquals(70, com.sekwah.narutomod.abilities.jutsus.ChidoriStreamAbility.computeFieldDurationTicks(700));
    }

    @Test
    @DisplayName("Verify Phase 2 Kubikiribocho blood-repair, village Kage/Jonin passives, and Sage Cloak math")
    void testPhase2EquipmentExpansion() {
        // Kubikiribocho durability repair math
        assertEquals(98, com.sekwah.narutomod.item.weapons.KubikiribochoItem.computeRepairedDamageValue(100, false));
        assertEquals(85, com.sekwah.narutomod.item.weapons.KubikiribochoItem.computeRepairedDamageValue(100, true));
        assertEquals(0, com.sekwah.narutomod.item.weapons.KubikiribochoItem.computeRepairedDamageValue(10, true));

        // Kage Hats -15% chakra cost on matching element only
        assertEquals(34.0F, Ability.applyElementalHatDiscount(ChakraElement.WIND, ChakraElement.WIND, 40.0F), 0.001F);
        assertEquals(40.0F, Ability.applyElementalHatDiscount(ChakraElement.WIND, ChakraElement.FIRE, 40.0F), 0.001F);
        assertEquals(40.0F, Ability.applyElementalHatDiscount(ChakraElement.NONE, ChakraElement.LIGHTNING, 40.0F), 0.001F);

        // Sage Cloak +50% chakra regen & +12% Senjutsu damage multiplier
        assertEquals(1.50F, ChakraStaminaLogic.getChestplateChakraRegenMultiplier(true, false, false, false), 0.001F);
        assertEquals(1.20F, ChakraStaminaLogic.getChestplateChakraRegenMultiplier(false, false, true, false), 0.001F);
        assertEquals(1.25F, ChakraStaminaLogic.getChestplateStaminaRegenMultiplier(true, false), 0.001F);
        assertEquals(11.2F, PlayerEvents.applySageCloakDamageMultiplier(10.0F, true), 0.001F);
        assertEquals(10.0F, PlayerEvents.applySageCloakDamageMultiplier(10.0F, false), 0.001F);

        // Village Kage Hat & Jonin Vest defensive passives
        assertEquals(17.0F, PlayerEvents.applyVillageArmorDamageReduction(
                20.0F, ChakraElement.WIND, false, false, false, false, false, false, true), 0.001F);
        assertEquals(15.0F, PlayerEvents.applyVillageArmorDamageReduction(
                20.0F, ChakraElement.NONE, true, false, false, true, false, false, false), 0.001F);
        assertEquals(15.0F, PlayerEvents.applyVillageArmorDamageReduction(
                20.0F, ChakraElement.NONE, false, true, false, false, true, false, false), 0.001F);
        assertEquals(16.0F, PlayerEvents.applyVillageArmorDamageReduction(
                20.0F, ChakraElement.NONE, false, false, true, false, false, true, false), 0.001F);
    }

    @Test
    @DisplayName("Verify Phase 3 VillageNinjaEntity biome resolution, village presets, and Chidori Spark tactical AI")
    void testPhase3VillageNpcAndJutsuProfiles() {
        var VillageType = com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.KONOHA;
        assertEquals(com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.SUNA,
                com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.fromId("suna"));
        assertEquals(VillageType,
                com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.fromId("unknown"));

        assertEquals(com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.SUNA,
                com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.fromBiomePath("desert"));
        assertEquals(com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.KIRI,
                com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.fromBiomePath("frozen_ocean"));
        assertEquals(com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.KUMO,
                com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.fromBiomePath("jagged_peaks"));
        assertEquals(com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.IWA,
                com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.fromBiomePath("savanna"));
        assertEquals(com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.KONOHA,
                com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.fromBiomePath("forest"));

        var konoha = com.sekwah.narutomod.entity.ai.NinjaJutsuProfile.villagePreset("konoha");
        var suna = com.sekwah.narutomod.entity.ai.NinjaJutsuProfile.villagePreset("suna");
        var kiri = com.sekwah.narutomod.entity.ai.NinjaJutsuProfile.villagePreset("kiri");
        var kumo = com.sekwah.narutomod.entity.ai.NinjaJutsuProfile.villagePreset("kumo");
        var iwa = com.sekwah.narutomod.entity.ai.NinjaJutsuProfile.villagePreset("iwa");

        assertTrue(konoha.isFireballEnabled());
        assertTrue(suna.isVacuumSpheresEnabled());
        assertTrue(kiri.isWaterBulletEnabled());
        assertTrue(kumo.isChidoriSparkEnabled());
        assertTrue(kumo.isVacuumSpheresEnabled());
        assertTrue(iwa.isEarthBulletEnabled());
        assertTrue(iwa.isMudWallEnabled());

        var cooldowns = new com.sekwah.narutomod.entity.ai.NinjaJutsuCooldowns();
        assertEquals(com.sekwah.narutomod.entity.ai.NinjaJutsuType.CHIDORI_SPARK,
                com.sekwah.narutomod.entity.ai.NinjaJutsuAttackGoal.selectTacticalAction(
                        kumo, cooldowns, 5.0D, true, 0.9F, 0));
    }

    @Test
    @DisplayName("Verify ShinobiVillagerEntity per-village specialty trade definitions across all 5 villages")
    void testShinobiVillagerMerchantOffers() {
        for (var vt : com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.values()) {
            var specialties = com.sekwah.narutomod.entity.mob.ShinobiVillagerEntity.getVillageSpecialtyItemIds(vt);
            assertEquals(3, specialties.size(), "Each village merchant must define 3 unique specialty trades: " + vt);
        }
        assertTrue(com.sekwah.narutomod.entity.mob.ShinobiVillagerEntity.getVillageSpecialtyItemIds(
                com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.KONOHA).contains("ichiraku_ramen"));
        assertTrue(com.sekwah.narutomod.entity.mob.ShinobiVillagerEntity.getVillageSpecialtyItemIds(
                com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.SUNA).contains("fuma_shuriken"));
        assertTrue(com.sekwah.narutomod.entity.mob.ShinobiVillagerEntity.getVillageSpecialtyItemIds(
                com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.KIRI).contains("anbu_tanto"));
        assertTrue(com.sekwah.narutomod.entity.mob.ShinobiVillagerEntity.getVillageSpecialtyItemIds(
                com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.KUMO).contains("chakra_blades"));
        assertTrue(com.sekwah.narutomod.entity.mob.ShinobiVillagerEntity.getVillageSpecialtyItemIds(
                com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.IWA).contains("scroll_stone_spikes"));
    }
}
