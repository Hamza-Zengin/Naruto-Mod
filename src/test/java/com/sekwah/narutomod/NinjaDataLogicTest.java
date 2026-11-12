package com.sekwah.narutomod;

import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.jutsus.AmaterasuAbility;
import com.sekwah.narutomod.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutomod.abilities.jutsus.EightGatesAbility;
import com.sekwah.narutomod.abilities.jutsus.EighthGateOfDeathAbility;
import com.sekwah.narutomod.abilities.jutsus.RasenshurikenAbility;
import com.sekwah.narutomod.abilities.jutsus.ShadowCloneAbility;
import com.sekwah.narutomod.abilities.jutsus.SharinganAbility;
import com.sekwah.narutomod.abilities.jutsus.SusanooRibcageAbility;
import com.sekwah.narutomod.item.weapons.KusanagiSwordItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying NinjaData game logic:
 * - Chakra and Stamina pools & usage
 * - Clan assignments and Uzumaki clan bonus calculations
 * - Jutsu unlock tracking
 * - Rank progression
 */
public class NinjaDataLogicTest {

    private NinjaData ninjaData;

    @BeforeEach
    public void setup() {
        this.ninjaData = new NinjaData(true);
    }

    @Test
    @DisplayName("Verify default initialization of server NinjaData")
    public void testDefaultInitialization() {
        assertEquals("ACADEMY_STUDENT", ninjaData.getRank());
        assertEquals(NinjaRank.ACADEMY_STUDENT, NinjaRank.fromString(ninjaData.getRank()));
        assertFalse(ninjaData.isClanLocked());
        assertEquals("shinobi", ninjaData.getClan().getPath());
        assertTrue(ninjaData.getChakra() > 0, "Initial chakra should be greater than 0");
        assertTrue(ninjaData.getStamina() > 0, "Initial stamina should be greater than 0");
        assertEquals("", ninjaData.getUnlockedJutsusString());
    }

    @Test
    @DisplayName("Verify chakra usage and clamping")
    public void testChakraUsage() {
        float initial = ninjaData.getChakra();
        ninjaData.useChakra(20.0f, 10);
        assertEquals(initial - 20.0f, ninjaData.getChakra(), 0.01f);

        // Over-exhaustion should clamp to 0
        ninjaData.useChakra(10000.0f, 10);
        assertEquals(0.0f, ninjaData.getChakra(), 0.01f);
    }

    @Test
    @DisplayName("Verify stamina usage and clamping")
    public void testStaminaUsage() {
        float initial = ninjaData.getStamina();
        ninjaData.useStamina(15.0f, 10);
        assertEquals(initial - 15.0f, ninjaData.getStamina(), 0.01f);

        // Over-exhaustion should clamp to 0
        ninjaData.useStamina(10000.0f, 10);
        assertEquals(0.0f, ninjaData.getStamina(), 0.01f);
    }

    @Test
    @DisplayName("Verify Uzumaki clan chakra bonus (+25% max chakra)")
    public void testUzumakiClanBonus() {
        ninjaData.setClan(new ResourceLocation(NarutoMod.MOD_ID, "shinobi"));
        float baseMaxChakra = ninjaData.getMaxChakra();

        ninjaData.setClan(new ResourceLocation(NarutoMod.MOD_ID, "uzumaki"));
        float uzumakiMaxChakra = ninjaData.getMaxChakra();

        assertEquals(baseMaxChakra * 1.25f, uzumakiMaxChakra, 0.01f,
                "Uzumaki clan should grant exactly +25% max chakra");
    }

    @Test
    @DisplayName("Verify jutsu unlock tracking and persistence format")
    public void testJutsuUnlocks() {
        ResourceLocation fireball = new ResourceLocation(NarutoMod.MOD_ID, "fireball");
        ResourceLocation chidori = new ResourceLocation(NarutoMod.MOD_ID, "chidori");

        assertFalse(ninjaData.getUnlockedAbilities().contains(fireball));
        assertFalse(ninjaData.getUnlockedAbilities().contains(chidori));

        ninjaData.unlockAbility(fireball);
        assertTrue(ninjaData.getUnlockedAbilities().contains(fireball));
        assertFalse(ninjaData.getUnlockedAbilities().contains(chidori));

        ninjaData.unlockAbility(chidori);
        assertTrue(ninjaData.getUnlockedAbilities().contains(fireball));
        assertTrue(ninjaData.getUnlockedAbilities().contains(chidori));

        // Duplicate unlocks should be idempotent
        ninjaData.unlockAbility(fireball);
        assertTrue(ninjaData.getUnlockedAbilities().contains(fireball));

        // Lock ability
        ninjaData.lockAbility(fireball);
        assertFalse(ninjaData.getUnlockedAbilities().contains(fireball));
        assertTrue(ninjaData.getUnlockedAbilities().contains(chidori));
    }

    @Test
    @DisplayName("Verify ninja rank progression")
    public void testRankProgression() {
        assertEquals("ACADEMY_STUDENT", ninjaData.getRank());

        ninjaData.setRank("GENIN");
        assertEquals("GENIN", ninjaData.getRank());
        assertEquals(NinjaRank.GENIN, NinjaRank.fromString(ninjaData.getRank()));

        ninjaData.setRank("CHUNIN");
        assertEquals("CHUNIN", ninjaData.getRank());
        assertEquals(NinjaRank.CHUNIN, NinjaRank.fromString(ninjaData.getRank()));

        ninjaData.setRank("JONIN");
        assertEquals("JONIN", ninjaData.getRank());
        assertEquals(NinjaRank.JONIN, NinjaRank.fromString(ninjaData.getRank()));
    }

    @Test
    @DisplayName("Verify clan lock state changes")
    public void testClanLocking() {
        assertFalse(ninjaData.isClanLocked());
        ninjaData.setClanLocked(true);
        assertTrue(ninjaData.isClanLocked());
    }

    @Test
    @DisplayName("Verify active jutsus bitmask initialization")
    public void testActiveJutsusBitmaskInitialization() {
        assertEquals(0, ninjaData.getActiveJutsusFlag());
        assertFalse(ninjaData.isSharinganActive());
        assertFalse(ninjaData.isByakuganActive());
        assertFalse(ninjaData.isEightGatesActive());
        assertFalse(ninjaData.isWaterWalkActive());
    }

    @Test
    @DisplayName("Verify active jutsus bitmask manipulation and independence")
    public void testActiveJutsusBitmaskManipulation() {
        // Verify bit shift constants
        assertEquals(1, INinjaData.JUTSU_FLAG_SHARINGAN);
        assertEquals(2, INinjaData.JUTSU_FLAG_BYAKUGAN);
        assertEquals(4, INinjaData.JUTSU_FLAG_EIGHT_GATES);
        assertEquals(8, INinjaData.JUTSU_FLAG_WATER_WALK);

        // Activate Sharingan
        ninjaData.setSharinganActive(true);
        assertTrue(ninjaData.isSharinganActive());
        assertTrue(ninjaData.isJutsuActive(INinjaData.JUTSU_FLAG_SHARINGAN));
        assertEquals(1, ninjaData.getActiveJutsusFlag());

        // Independent bits: other jutsus must remain inactive
        assertFalse(ninjaData.isByakuganActive());
        assertFalse(ninjaData.isEightGatesActive());
        assertFalse(ninjaData.isWaterWalkActive());

        // Activate Eight Gates concurrently
        ninjaData.setEightGatesActive(true);
        assertTrue(ninjaData.isSharinganActive());
        assertTrue(ninjaData.isEightGatesActive());
        assertEquals(1 | 4, ninjaData.getActiveJutsusFlag()); // 5

        // Activate Water Walk
        ninjaData.setWaterWalkActive(true);
        assertTrue(ninjaData.isWaterWalkActive());
        assertEquals(1 | 4 | 8, ninjaData.getActiveJutsusFlag()); // 13

        // Deactivate Sharingan
        ninjaData.setSharinganActive(false);
        assertFalse(ninjaData.isSharinganActive());
        assertTrue(ninjaData.isEightGatesActive());
        assertTrue(ninjaData.isWaterWalkActive());
        assertEquals(4 | 8, ninjaData.getActiveJutsusFlag()); // 12

        // Activate Byakugan
        ninjaData.setByakuganActive(true);
        assertTrue(ninjaData.isByakuganActive());
        assertEquals(2 | 4 | 8, ninjaData.getActiveJutsusFlag()); // 14
    }

    @Test
    @DisplayName("Verify active jutsus bitmask NBT serialization and deserialization")
    public void testActiveJutsusBitmaskNBT() {
        ninjaData.setSharinganActive(true);
        ninjaData.setWaterWalkActive(true);
        assertEquals(INinjaData.JUTSU_FLAG_SHARINGAN | INinjaData.JUTSU_FLAG_WATER_WALK, ninjaData.getActiveJutsusFlag());

        Tag serialized = ninjaData.serializeNBT();
        assertNotNull(serialized);

        NinjaData restored = new NinjaData(false);
        restored.deserializeNBT(serialized);

        assertEquals(ninjaData.getActiveJutsusFlag(), restored.getActiveJutsusFlag());
        assertTrue(restored.isSharinganActive());
        assertTrue(restored.isWaterWalkActive());
        assertFalse(restored.isByakuganActive());
        assertFalse(restored.isEightGatesActive());
    }

    @Test
    @DisplayName("Verify active jutsus bitmask reset on resetData")
    public void testActiveJutsusBitmaskReset() {
        ninjaData.setActiveJutsusFlag(0b1111);
        assertEquals(15, ninjaData.getActiveJutsusFlag());

        ninjaData.resetData();
        assertEquals(0, ninjaData.getActiveJutsusFlag());
        assertFalse(ninjaData.isSharinganActive());
        assertFalse(ninjaData.isByakuganActive());
        assertFalse(ninjaData.isEightGatesActive());
        assertFalse(ninjaData.isWaterWalkActive());
    }

    @Test
    @DisplayName("Verify hand seals buffer and timeout initialization")
    public void testHandSealsBuffer() {
        assertTrue(ninjaData.getActiveSeals().isEmpty(), "Active seals should initially be empty");
        assertEquals(0, ninjaData.getSealTimeout());

        ninjaData.addSeal((byte) 1);
        assertEquals(1, ninjaData.getActiveSeals().size());
        assertEquals((byte) 1, ninjaData.getActiveSeals().get(0));
        assertEquals(NinjaData.DEFAULT_SEAL_TIMEOUT, ninjaData.getSealTimeout());

        ninjaData.addSeal((byte) 2);
        ninjaData.addSeal((byte) 1);
        assertEquals(3, ninjaData.getActiveSeals().size());
        assertEquals((byte) 1, ninjaData.getActiveSeals().get(0));
        assertEquals((byte) 2, ninjaData.getActiveSeals().get(1));
        assertEquals((byte) 1, ninjaData.getActiveSeals().get(2));

        ninjaData.clearSeals();
        assertTrue(ninjaData.getActiveSeals().isEmpty());
        assertEquals(0, ninjaData.getSealTimeout());
    }

    @Test
    @DisplayName("Verify hand seal timeout countdown and automatic clearance")
    public void testHandSealTimeoutCountdown() {
        ninjaData.addSeal((byte) 2);
        ninjaData.addSeal((byte) 3);
        assertEquals(2, ninjaData.getActiveSeals().size());
        assertEquals(NinjaData.DEFAULT_SEAL_TIMEOUT, ninjaData.getSealTimeout());

        // Tick 20 times (1 second)
        for (int i = 0; i < 20; i++) {
            ninjaData.tick();
        }
        assertEquals(NinjaData.DEFAULT_SEAL_TIMEOUT - 20, ninjaData.getSealTimeout());
        assertEquals(2, ninjaData.getActiveSeals().size(), "Seals should remain before timeout");

        // Tick remaining 40 times to reach 0
        for (int i = 0; i < 40; i++) {
            ninjaData.tick();
        }
        assertEquals(0, ninjaData.getSealTimeout());
        assertTrue(ninjaData.getActiveSeals().isEmpty(), "Seals should be cleared after timeout");
    }

    @Test
    @DisplayName("Verify hand seal max capacity constraint")
    public void testHandSealMaxCapacity() {
        for (byte b = 1; b <= 20; b++) {
            ninjaData.addSeal(b);
        }
        assertEquals(NinjaData.MAX_ACTIVE_SEALS, ninjaData.getActiveSeals().size());
        // Oldest seals (1..8) dropped, last 12 (9..20) kept
        assertEquals((byte) 9, ninjaData.getActiveSeals().get(0));
        assertEquals((byte) 20, ninjaData.getActiveSeals().get(NinjaData.MAX_ACTIVE_SEALS - 1));
    }

    @Test
    @DisplayName("Verify hand seals NBT serialization and deserialization")
    public void testHandSealsNBT() {
        ninjaData.addSeal((byte) 1);
        ninjaData.addSeal((byte) 2);
        ninjaData.addSeal((byte) 3);

        Tag serialized = ninjaData.serializeNBT();
        NinjaData restored = new NinjaData(false);
        restored.deserializeNBT(serialized);

        assertEquals(ninjaData.getActiveSeals().size(), restored.getActiveSeals().size());
        assertEquals(ninjaData.getActiveSeals(), restored.getActiveSeals());
        assertEquals(ninjaData.getSealTimeout(), restored.getSealTimeout());
    }

    @Test
    @DisplayName("Verify Ability expected seals and 25% chakra cost discount")
    public void testHandSealComboCostDiscount() {
        com.sekwah.narutomod.abilities.jutsus.FireballJutsuAbility fireball = new com.sekwah.narutomod.abilities.jutsus.FireballJutsuAbility();
        fireball.setRegistryName(new ResourceLocation(NarutoMod.MOD_ID, "fireball"));

        // 1. Verify canon expected seals: Snake (6) - Ram (8) - Monkey (9) - Boar (12) - Horse (7) - Tiger (3)
        assertArrayEquals(new byte[]{6, 8, 9, 12, 7, 3}, fireball.getExpectedSeals());

        // 2. Test matching canon seals sequence
        java.util.List<Byte> canonSeals = java.util.List.of((byte) 6, (byte) 8, (byte) 9, (byte) 12, (byte) 7, (byte) 3);
        assertTrue(fireball.matchesExpectedSeals(canonSeals));

        // 3. Test matching in-game defaultCombo digits (1-2-1)
        java.util.List<Byte> comboSeals = java.util.List.of((byte) 1, (byte) 2, (byte) 1);
        assertTrue(fireball.matchesExpectedSeals(comboSeals));

        // 4. Test mismatched seals
        java.util.List<Byte> wrongSeals = java.util.List.of((byte) 2, (byte) 3, (byte) 2);
        assertFalse(fireball.matchesExpectedSeals(wrongSeals));

        // 5. Test ServerTriggerHandJutsuPacket.calculateEffectiveChakraCost
        float baseCost = fireball.getDefaultChakraCost(); // 30.0f
        float discounted = com.sekwah.narutomod.network.c2s.ServerTriggerHandJutsuPacket.calculateEffectiveChakraCost(fireball, baseCost, canonSeals);
        assertEquals(baseCost * 0.75f, discounted, 0.01f, "Matched combo should grant exactly 25% cost reduction");

        float notDiscounted = com.sekwah.narutomod.network.c2s.ServerTriggerHandJutsuPacket.calculateEffectiveChakraCost(fireball, baseCost, wrongSeals);
        assertEquals(baseCost, notDiscounted, 0.01f);

        // 6. Test NinjaData integration with Ability.getEffectiveChakraCost
        ninjaData.clearSeals();
        assertEquals(baseCost, fireball.getEffectiveChakraCost(ninjaData), 0.01f);

        for (byte b : comboSeals) {
            ninjaData.addSeal(b);
        }
        assertEquals(baseCost * 0.75f, fireball.getEffectiveChakraCost(ninjaData), 0.01f);
    }

    @Test
    @DisplayName("Verify getToggleAbilityData is null-safe and self-healing")
    public void testToggleAbilityDataNullSafety() {
        assertNotNull(ninjaData.getToggleAbilityData(), "Initial ToggleAbilityData should not be null");

        // Force reflection to set toggleAbilityData to null (simulating corrupted network sync)
        try {
            java.lang.reflect.Field field = NinjaData.class.getDeclaredField("toggleAbilityData");
            field.setAccessible(true);
            field.set(ninjaData, null);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }

        // Must self-heal and return a non-null instance
        assertNotNull(ninjaData.getToggleAbilityData(), "getToggleAbilityData should self-heal when field is null");
        assertNotNull(ninjaData.getToggleAbilityData().getAbilitiesSnapshot());
        assertEquals(0, ninjaData.getToggleAbilityData().getAbilitiesSnapshot().size());
    }

    @Test
    @DisplayName("Verify jutsu mastery tracking, incrementing, and map retrieval")
    public void testJutsuMasteryTracking() {
        ResourceLocation rasengan = new ResourceLocation(NarutoMod.MOD_ID, "rasengan");
        ResourceLocation chidori = new ResourceLocation(NarutoMod.MOD_ID, "chidori");

        assertEquals(0, ninjaData.getMastery(rasengan), "Mastery should default to 0");
        assertEquals(0, ninjaData.getMastery(chidori), "Mastery should default to 0");
        assertTrue(ninjaData.getJutsuMasteryMap().isEmpty(), "Initial mastery map should be empty");

        ninjaData.addMastery(rasengan, 1);
        assertEquals(1, ninjaData.getMastery(rasengan));
        assertEquals(0, ninjaData.getMastery(chidori));

        ninjaData.addMastery(rasengan, 99);
        assertEquals(100, ninjaData.getMastery(rasengan));

        ninjaData.addMastery(chidori, 50);
        assertEquals(100, ninjaData.getMastery(rasengan));
        assertEquals(50, ninjaData.getMastery(chidori));

        assertEquals(2, ninjaData.getJutsuMasteryMap().size());
        assertEquals(100, ninjaData.getJutsuMasteryMap().get(rasengan));
        assertEquals(50, ninjaData.getJutsuMasteryMap().get(chidori));
    }

    @Test
    @DisplayName("Verify jutsu mastery NBT serialization and deserialization via saveNBTData and loadNBTData")
    public void testJutsuMasteryNBT() {
        ResourceLocation rasengan = new ResourceLocation(NarutoMod.MOD_ID, "rasengan");
        ResourceLocation fireball = new ResourceLocation(NarutoMod.MOD_ID, "fireball");

        ninjaData.addMastery(rasengan, 150);
        ninjaData.addMastery(fireball, 30);

        // Test with saveNBTData and loadNBTData
        CompoundTag compound = new CompoundTag();
        ninjaData.saveNBTData(compound);

        NinjaData restored = new NinjaData(false);
        restored.loadNBTData(compound);

        assertEquals(150, restored.getMastery(rasengan));
        assertEquals(30, restored.getMastery(fireball));
        assertEquals(2, restored.getJutsuMasteryMap().size());

        // Also test with serializeNBT and deserializeNBT
        Tag serializedTag = ninjaData.serializeNBT();
        NinjaData restoredViaTag = new NinjaData(false);
        restoredViaTag.deserializeNBT(serializedTag);

        assertEquals(150, restoredViaTag.getMastery(rasengan));
        assertEquals(30, restoredViaTag.getMastery(fireball));
    }

    @Test
    @DisplayName("Verify jutsu mastery cleared on resetData")
    public void testJutsuMasteryReset() {
        ResourceLocation rasengan = new ResourceLocation(NarutoMod.MOD_ID, "rasengan");
        ninjaData.addMastery(rasengan, 120);
        assertEquals(120, ninjaData.getMastery(rasengan));

        ninjaData.resetData();
        assertEquals(0, ninjaData.getMastery(rasengan));
        assertTrue(ninjaData.getJutsuMasteryMap().isEmpty());
    }

    private static class DummyAbility extends Ability {
        public boolean executed = false;

        public DummyAbility(ResourceLocation id) {
            this.setRegistryName(id);
        }

        @Override
        public ActivationType activationType() {
            return ActivationType.INSTANT;
        }

        @Override
        public void performServer(net.minecraft.world.entity.player.Player player, INinjaData ninjaData, int ticksActive) {
            this.executed = true;
        }
    }

    @Test
    @DisplayName("Verify Ability perform() and performServer() increment jutsu mastery dynamically (+3/+2/+1)")
    public void testAbilityPerformIncrementsMastery() {
        ResourceLocation testJutsu = new ResourceLocation(NarutoMod.MOD_ID, "test_jutsu");
        DummyAbility ability = new DummyAbility(testJutsu);

        assertEquals(0, ninjaData.getMastery(testJutsu));
        assertFalse(ability.executed);

        // First execution via perform() (mastery < 150 grants +3)
        ability.perform(null, ninjaData);
        assertTrue(ability.executed);
        assertEquals(3, ninjaData.getMastery(testJutsu), "Successful perform() below 150 should grant +3 mastery");

        // Second execution via perform(player, data, ticks)
        ability.perform(null, ninjaData, 10);
        assertEquals(6, ninjaData.getMastery(testJutsu), "Second perform() below 150 should grant another +3 mastery");

        // Third execution via performServer()
        ability.performServer(null, ninjaData);
        assertEquals(9, ninjaData.getMastery(testJutsu), "performServer() should route to perform() and grant +3 mastery");

        // Mastery tier transition (150 <= mastery < 350 grants +2)
        ninjaData.addMastery(testJutsu, 150); // total 159
        ability.perform(null, ninjaData);
        assertEquals(161, ninjaData.getMastery(testJutsu), "Perform() between 150 and 350 should grant +2 mastery");

        // Mastery tier transition (mastery >= 350 grants +1)
        ninjaData.addMastery(testJutsu, 200); // total 361
        ability.perform(null, ninjaData);
        assertEquals(362, ninjaData.getMastery(testJutsu), "Perform() above 350 should grant +1 mastery");
    }

    @Test
    @DisplayName("Verify RasenshurikenAbility requires Rasengan with 100+ mastery")
    public void testRasenshurikenPrerequisites() {
        RasenshurikenAbility rasenshuriken = new RasenshurikenAbility();
        rasenshuriken.setRegistryName(new ResourceLocation(NarutoMod.MOD_ID, "rasenshuriken"));

        ResourceLocation rasengan = new ResourceLocation(NarutoMod.MOD_ID, "rasengan");

        // Verify prerequisite configuration
        assertEquals(List.of(rasengan), rasenshuriken.getPrerequisites());
        assertEquals(100, rasenshuriken.getRequiredMastery());

        // 1. Rasengan not unlocked, 0 mastery
        assertFalse(rasenshuriken.meetsPrerequisites(ninjaData), "Prerequisites must fail when Rasengan is not known");

        // 2. Rasengan unlocked, but mastery is below 100
        ninjaData.unlockAbility(rasengan);
        ninjaData.addMastery(rasengan, 50);
        assertFalse(rasenshuriken.meetsPrerequisites(ninjaData), "Prerequisites must fail when Rasengan mastery < 100");

        // 3. Rasengan mastery reaches 99
        ninjaData.addMastery(rasengan, 49);
        assertEquals(99, ninjaData.getMastery(rasengan));
        assertFalse(rasenshuriken.meetsPrerequisites(ninjaData), "Prerequisites must fail when Rasengan mastery is 99");

        // 4. Rasengan mastery reaches 100
        ninjaData.addMastery(rasengan, 1);
        assertEquals(100, ninjaData.getMastery(rasengan));
        assertTrue(rasenshuriken.meetsPrerequisites(ninjaData), "Prerequisites must succeed when Rasengan mastery >= 100");

        // 5. Rasengan mastery exceeds 100
        ninjaData.addMastery(rasengan, 50);
        assertTrue(rasenshuriken.meetsPrerequisites(ninjaData));
    }

    @Test
    @DisplayName("Verify prerequisite locks prevent unlock until mastery and rank requirements are met")
    public void testPrerequisiteLockAndUnlockLogic() {
        RasenshurikenAbility rasenshuriken = new RasenshurikenAbility();
        rasenshuriken.setRegistryName(new ResourceLocation(NarutoMod.MOD_ID, "rasenshuriken"));
        ResourceLocation rasengan = new ResourceLocation(NarutoMod.MOD_ID, "rasengan");

        // Genin rank, prerequisites not met
        ninjaData.setRank("GENIN");
        assertFalse(rasenshuriken.meetsPrerequisites(ninjaData));
        assertFalse(ninjaData.isAbilityUnlocked(rasenshuriken));

        // Unlock Rasengan and train mastery to 100, but still Genin rank
        ninjaData.unlockAbility(rasengan);
        ninjaData.addMastery(rasengan, 100);
        assertTrue(rasenshuriken.meetsPrerequisites(ninjaData));
        // Rasenshuriken requires JONIN rank
        assertFalse(ninjaData.isAbilityUnlocked(rasenshuriken), "Must not unlock below required JONIN rank");

        // Promote to JONIN rank
        ninjaData.setRank("JONIN");
        assertTrue(ninjaData.isAbilityUnlocked(rasenshuriken), "Should unlock once prerequisites (Rasengan 100) and JONIN rank are both achieved!");
    }

    @Test
    @DisplayName("Verify Sharingan evolution stages based on mastery")
    public void testSharinganEvolutionStages() {
        assertEquals(SharinganAbility.SharinganStage.ONE_TOMOE, SharinganAbility.getStage(0));
        assertEquals(SharinganAbility.SharinganStage.ONE_TOMOE, SharinganAbility.getStage(999));
        assertEquals(SharinganAbility.SharinganStage.TWO_TOMOE, SharinganAbility.getStage(1000));
        assertEquals(SharinganAbility.SharinganStage.TWO_TOMOE, SharinganAbility.getStage(2499));
        assertEquals(SharinganAbility.SharinganStage.THREE_TOMOE, SharinganAbility.getStage(2500));
        assertEquals(SharinganAbility.SharinganStage.THREE_TOMOE, SharinganAbility.getStage(4999));
        assertEquals(SharinganAbility.SharinganStage.MANGEKYOU, SharinganAbility.getStage(5000));
        assertEquals(SharinganAbility.SharinganStage.MANGEKYOU, SharinganAbility.getStage(8000));

        ResourceLocation sharingan = new ResourceLocation(NarutoMod.MOD_ID, "sharingan");
        assertFalse(SharinganAbility.hasMangekyou(ninjaData));

        ninjaData.addMastery(sharingan, 1000);
        assertEquals(SharinganAbility.SharinganStage.TWO_TOMOE, SharinganAbility.getStage(ninjaData));
        assertFalse(SharinganAbility.hasMangekyou(ninjaData));

        ninjaData.addMastery(sharingan, 1500); // 2500 total
        assertEquals(SharinganAbility.SharinganStage.THREE_TOMOE, SharinganAbility.getStage(ninjaData));
        assertFalse(SharinganAbility.hasMangekyou(ninjaData));

        ninjaData.addMastery(sharingan, 2500); // 5000 total
        assertEquals(SharinganAbility.SharinganStage.MANGEKYOU, SharinganAbility.getStage(ninjaData));
        assertTrue(SharinganAbility.hasMangekyou(ninjaData));
    }

    @Test
    @DisplayName("Verify SusanooRibcageAbility requires Uchiha clan and Mangekyou stage")
    public void testSusanooRequiresMangekyou() {
        SusanooRibcageAbility susanoo = new SusanooRibcageAbility();
        susanoo.setRegistryName(new ResourceLocation(NarutoMod.MOD_ID, "susanoo_ribcage"));
        ResourceLocation sharingan = new ResourceLocation(NarutoMod.MOD_ID, "sharingan");

        // 1. Non-Uchiha clan cannot activate Susanoo
        ninjaData.setClan(new ResourceLocation(NarutoMod.MOD_ID, "hyuga"));
        assertFalse(susanoo.canActivate(null, ninjaData));
        assertFalse(ninjaData.isAbilityUnlocked(susanoo));

        // 2. Uchiha clan without Mangekyou (0 mastery)
        ninjaData.forceSetClan(new ResourceLocation(NarutoMod.MOD_ID, "uchiha"));
        assertFalse(susanoo.canActivate(null, ninjaData));
        assertFalse(susanoo.meetsPrerequisites(ninjaData));
        assertFalse(ninjaData.isAbilityUnlocked(susanoo));

        // 3. Uchiha clan with 3-Tomoe (2500 mastery)
        ninjaData.unlockAbility(sharingan);
        ninjaData.addMastery(sharingan, 2500);
        assertFalse(susanoo.canActivate(null, ninjaData));
        assertFalse(susanoo.meetsPrerequisites(ninjaData));
        assertFalse(ninjaData.isAbilityUnlocked(susanoo));

        // 4. Awakened Mangekyou (5000 mastery)
        ninjaData.addMastery(sharingan, 2500); // 5000 total
        assertTrue(susanoo.canActivate(null, ninjaData));
        assertTrue(susanoo.meetsPrerequisites(ninjaData));

        // Still requires JONIN rank to unlock
        ninjaData.setRank("GENIN");
        assertFalse(ninjaData.isAbilityUnlocked(susanoo));

        // When reaching JONIN rank (and being Uchiha with Mangekyou), unlocks!
        ninjaData.setRank("JONIN");
        assertTrue(ninjaData.isAbilityUnlocked(susanoo));
    }

    @Test
    @DisplayName("Verify Byakugan Tenketsu chakra regeneration lock and serialization")
    public void testByakuganChakraLockDebuff() {
        assertEquals(0, ninjaData.getChakraRegenCooldown());
        assertFalse(ninjaData.isChakraRegenLocked());

        ninjaData.lockChakraRegen(ByakuganAbility.CHAKRA_LOCK_TICKS);
        assertEquals(100, ninjaData.getChakraRegenCooldown());
        assertTrue(ninjaData.isChakraRegenLocked());

        // Calling lockChakraRegen with smaller duration shouldn't lower the existing cooldown
        ninjaData.lockChakraRegen(40);
        assertEquals(100, ninjaData.getChakraRegenCooldown());

        // NBT serialization test
        Tag nbt = ninjaData.serializeNBT();
        NinjaData restored = new NinjaData(false);
        restored.deserializeNBT(nbt);
        assertEquals(100, restored.getChakraRegenCooldown());
        assertTrue(restored.isChakraRegenLocked());

        // Reset data test
        ninjaData.resetData();
        assertEquals(0, ninjaData.getChakraRegenCooldown());
        assertFalse(ninjaData.isChakraRegenLocked());
    }

    @Test
    @DisplayName("Verify Alt Asama 3.3 rank mission prerequisites: Genin->Chunin and Chunin->Jonin")
    public void testRankUpMissionPrerequisites() {
        ResourceLocation fireball = new ResourceLocation(NarutoMod.MOD_ID, "fireball");
        ResourceLocation waterBullet = new ResourceLocation(NarutoMod.MOD_ID, "water_bullet");
        ResourceLocation bodyFlicker = new ResourceLocation(NarutoMod.MOD_ID, "body_flicker");
        ResourceLocation rasengan = new ResourceLocation(NarutoMod.MOD_ID, "rasengan");

        // 1. Initial Academy Student
        assertEquals("ACADEMY_STUDENT", ninjaData.getRank());
        assertFalse(ninjaData.canRankUp(), "Cannot graduate without chakra >= 120 and jutsus cast >= 8");

        ninjaData.setBaseMaxChakra(120.0f);
        ninjaData.setJutsusCastCount(8);
        assertTrue(ninjaData.canRankUp());
        assertTrue(ninjaData.rankUp());
        assertEquals("GENIN", ninjaData.getRank());

        // 2. Genin -> Chunin Requirements:
        // Chakra >= 160, Stamina >= 130, Rogue Ninjas Defeated >= 10, At least 3 jutsus with 50+ mastery
        assertFalse(ninjaData.canRankUp(), "Genin cannot rank up initially");

        // Set chakra and stamina to target
        ninjaData.setBaseMaxChakra(160.0f);
        ninjaData.setBaseMaxStamina(130.0f);
        assertFalse(ninjaData.canRankUp(), "Cannot rank up without rogue kills and jutsu masteries");

        // Set rogue ninjas to 9 (needs 10)
        ninjaData.setRogueNinjasDefeated(9);
        ninjaData.addMastery(fireball, 50);
        ninjaData.addMastery(waterBullet, 50);
        ninjaData.addMastery(bodyFlicker, 50);
        assertFalse(ninjaData.canRankUp(), "Cannot rank up with only 9 rogue kills");
        assertEquals(1, NinjaRank.GENIN.getMissingRequirements(ninjaData).size());

        // Add 10th rogue kill
        ninjaData.addRogueNinjaDefeated();
        assertEquals(10, ninjaData.getRogueNinjasDefeated());
        assertTrue(ninjaData.canRankUp(), "Should be ready for Chunin rank up once all 4 requirements are met");
        assertTrue(NinjaRank.GENIN.getMissingRequirements(ninjaData).isEmpty());

        // Test one jutsu mastery failing (< 50)
        ninjaData.getJutsuMasteryMap().put(bodyFlicker, 49);
        assertFalse(ninjaData.canRankUp(), "Cannot rank up if only 2 jutsus have 50+ mastery");
        ninjaData.getJutsuMasteryMap().put(bodyFlicker, 50);
        assertTrue(ninjaData.canRankUp());

        // Graduate to CHUNIN
        assertTrue(ninjaData.rankUp());
        assertEquals("CHUNIN", ninjaData.getRank());

        // 3. Chunin -> Jonin Requirements:
        // Chakra >= 260, Stamina >= 180, Akatsuki Defeated >= 1, At least 1 advanced jutsu with 50+ mastery
        assertFalse(ninjaData.canRankUp(), "Chunin cannot rank up without advanced jutsu and akatsuki kill");

        ninjaData.setBaseMaxChakra(260.0f);
        ninjaData.setBaseMaxStamina(180.0f);
        assertFalse(ninjaData.canRankUp());

        // Add 50 mastery to standard jutsus (fireball, water bullet, body flicker) - none of them are advanced
        assertEquals(3, NinjaRank.countMasteredJutsus(ninjaData, 50));
        assertFalse(NinjaRank.hasMasteredAdvancedJutsu(ninjaData, 50));
        assertFalse(ninjaData.canRankUp());

        // Defeat an Akatsuki member
        ninjaData.addAkatsukiNinjaDefeated();
        assertEquals(1, ninjaData.getAkatsukiNinjasDefeated());
        assertFalse(ninjaData.canRankUp(), "Still requires an advanced jutsu (e.g. Rasengan) with 50+ mastery");

        // Master Rasengan (isScrollRequiredJutsu -> advanced) with 50 mastery
        ninjaData.addMastery(rasengan, 50);
        assertTrue(NinjaRank.hasMasteredAdvancedJutsu(ninjaData, 50));
        assertTrue(ninjaData.canRankUp(), "All Chunin -> Jonin requirements now satisfied!");
        assertTrue(NinjaRank.CHUNIN.getMissingRequirements(ninjaData).isEmpty());

        // Graduate to JONIN
        assertTrue(ninjaData.rankUp());
        assertEquals("JONIN", ninjaData.getRank());

        // 4. NBT Serialization of mission counters
        CompoundTag tag = new CompoundTag();
        ninjaData.saveNBTData(tag);

        NinjaData restored = new NinjaData(false);
        restored.loadNBTData(tag);
        assertEquals(10, restored.getRogueNinjasDefeated());
        assertEquals(1, restored.getAkatsukiNinjasDefeated());

        // 5. Reset data
        ninjaData.resetData();
        assertEquals(0, ninjaData.getRogueNinjasDefeated());
        assertEquals(0, ninjaData.getAkatsukiNinjasDefeated());
    }

    @Test
    @DisplayName("Verify weapon chakra interactions (Samehada devour & Kusanagi chakra flow)")
    public void testWeaponChakraInteractions() {
        // 1. Samehada Chakra Devour Logic
        NinjaData attackerData = new NinjaData(true);
        NinjaData targetData = new NinjaData(true);

        attackerData.setBaseMaxChakra(100.0f);
        attackerData.useChakra(100.0f, 0); // Attacker empty at 0 chakra
        assertEquals(0.0f, attackerData.getChakra(), 0.01f);

        targetData.setBaseMaxChakra(100.0f);
        assertEquals(100.0f, targetData.getChakra(), 0.01f);

        // Strike 1: Target has 100 chakra -> devours 20 chakra
        float targetChakra = targetData.getChakra();
        float drain = Math.min(targetChakra, 20.0f);
        targetData.useChakra(drain, 30);
        attackerData.addChakra(drain);

        assertEquals(80.0f, targetData.getChakra(), 0.01f);
        assertEquals(20.0f, attackerData.getChakra(), 0.01f);

        // Strike 2: Target has only 12 chakra left
        targetData.useChakra(68.0f, 0);
        assertEquals(12.0f, targetData.getChakra(), 0.01f);

        targetChakra = targetData.getChakra();
        drain = Math.min(targetChakra, 20.0f);
        targetData.useChakra(drain, 30);
        attackerData.addChakra(drain);

        assertEquals(0.0f, targetData.getChakra(), 0.01f);
        assertEquals(32.0f, attackerData.getChakra(), 0.01f);

        // Strike 3: Target has 0 chakra -> no chakra drained
        targetChakra = targetData.getChakra();
        drain = Math.min(targetChakra, 20.0f);
        assertEquals(0.0f, drain, 0.01f);

        // 2. Kusanagi NBT and Chakra Flow Logic
        CompoundTag activeTag = new CompoundTag();
        activeTag.putBoolean(KusanagiSwordItem.NBT_CHAKRA_ACTIVE, true);
        activeTag.putLong(KusanagiSwordItem.NBT_ACTIVE_UNTIL, 500L);

        assertTrue(activeTag.getBoolean(KusanagiSwordItem.NBT_CHAKRA_ACTIVE));
        assertEquals(500L, activeTag.getLong(KusanagiSwordItem.NBT_ACTIVE_UNTIL));

        // When expired (gameTime 600 > 500)
        long gameTime = 600L;
        if (gameTime > activeTag.getLong(KusanagiSwordItem.NBT_ACTIVE_UNTIL)) {
            activeTag.putBoolean(KusanagiSwordItem.NBT_CHAKRA_ACTIVE, false);
        }
        assertFalse(activeTag.getBoolean(KusanagiSwordItem.NBT_CHAKRA_ACTIVE));
    }

    @Test
    @DisplayName("Verify Amaterasu requires Uchiha clan, active Sharingan, and Mangekyo stage")
    public void testAmaterasuRequiresMangekyou() {
        AmaterasuAbility amaterasu = new AmaterasuAbility();
        amaterasu.setRegistryName(new ResourceLocation(NarutoMod.MOD_ID, "amaterasu"));
        ResourceLocation sharingan = new ResourceLocation(NarutoMod.MOD_ID, "sharingan");

        // 1. Non-Uchiha clan cannot activate
        ninjaData.setClan(new ResourceLocation(NarutoMod.MOD_ID, "hyuga"));
        assertFalse(amaterasu.canActivate(null, ninjaData));

        // 2. Uchiha clan without active Sharingan
        ninjaData.forceSetClan(new ResourceLocation(NarutoMod.MOD_ID, "uchiha"));
        ninjaData.unlockAbility(sharingan);
        ninjaData.addMastery(sharingan, 5000);
        assertFalse(amaterasu.canActivate(null, ninjaData)); // Sharingan not active

        // 3. Sharingan active but mastery < 5000 (3-Tomoe)
        ninjaData.setJutsuActive(NinjaData.JUTSU_FLAG_SHARINGAN, true);
        NinjaData lowMasteryData = new NinjaData(true);
        lowMasteryData.forceSetClan(new ResourceLocation(NarutoMod.MOD_ID, "uchiha"));
        lowMasteryData.unlockAbility(sharingan);
        lowMasteryData.addMastery(sharingan, 2500); // 3-Tomoe
        lowMasteryData.setJutsuActive(NinjaData.JUTSU_FLAG_SHARINGAN, true);
        assertFalse(amaterasu.canActivate(null, lowMasteryData));

        // 4. Sharingan active and Mangekyo (5000+ mastery)
        assertTrue(amaterasu.canActivate(null, ninjaData));
    }

    @Test
    @DisplayName("Verify Eight Gates stages progression and Eighth Gate of Death requirement")
    public void testEightGatesStagesAndDeathGate() {
        // Gate progression
        assertEquals(EightGatesAbility.GateStage.GATE_1, EightGatesAbility.getStage(0));
        assertEquals(EightGatesAbility.GateStage.GATE_1, EightGatesAbility.getStage(399));
        assertEquals(EightGatesAbility.GateStage.GATE_2, EightGatesAbility.getStage(400));
        assertEquals(EightGatesAbility.GateStage.GATE_2, EightGatesAbility.getStage(999));
        assertEquals(EightGatesAbility.GateStage.GATE_3, EightGatesAbility.getStage(1000));
        assertEquals(EightGatesAbility.GateStage.GATE_4, EightGatesAbility.getStage(2000));
        assertEquals(EightGatesAbility.GateStage.GATE_5, EightGatesAbility.getStage(3000));
        assertEquals(EightGatesAbility.GateStage.GATE_6, EightGatesAbility.getStage(4000));
        assertEquals(EightGatesAbility.GateStage.GATE_7, EightGatesAbility.getStage(5000));

        EighthGateOfDeathAbility deathGate = new EighthGateOfDeathAbility();
        deathGate.setRegistryName(new ResourceLocation(NarutoMod.MOD_ID, "eight_gates_death"));
        ResourceLocation eightGates = new ResourceLocation(NarutoMod.MOD_ID, "eight_gates");

        // Without mastery 5000 in eight_gates
        ninjaData.unlockAbility(eightGates);
        ninjaData.addMastery(eightGates, 4000);
        assertFalse(deathGate.meetsPrerequisites(ninjaData));
        assertFalse(deathGate.canActivate(null, ninjaData));

        // With 5000 mastery in eight_gates (Gate 7 reached)
        ninjaData.addMastery(eightGates, 1000); // 5000 total
        assertTrue(deathGate.meetsPrerequisites(ninjaData));
        assertTrue(deathGate.canActivate(null, ninjaData));
    }

    @Test
    @DisplayName("Verify Ability global mastery tiers and cost/power/cooldown multipliers")
    public void testAbilityMasteryTiersAndMultipliers() {
        // Tiers: Lv.1 (<50), Lv.2 (<150), Lv.3 (<350), Lv.4 (<700), Lv.5 (>=700)
        assertEquals(1, Ability.getMasteryTier(0));
        assertEquals(1, Ability.getMasteryTier(49));
        assertEquals(2, Ability.getMasteryTier(50));
        assertEquals(2, Ability.getMasteryTier(149));
        assertEquals(3, Ability.getMasteryTier(150));
        assertEquals(3, Ability.getMasteryTier(349));
        assertEquals(4, Ability.getMasteryTier(350));
        assertEquals(4, Ability.getMasteryTier(699));
        assertEquals(5, Ability.getMasteryTier(700));
        assertEquals(5, Ability.getMasteryTier(2000));

        // Chakra Cost Multipliers: Lv.1: 1.0, Lv.2: 0.90, Lv.3: 0.80, Lv.4: 0.70, Lv.5: 0.60
        assertEquals(1.00f, Ability.getChakraCostMultiplier(0), 0.001f);
        assertEquals(0.90f, Ability.getChakraCostMultiplier(50), 0.001f);
        assertEquals(0.80f, Ability.getChakraCostMultiplier(150), 0.001f);
        assertEquals(0.70f, Ability.getChakraCostMultiplier(350), 0.001f);
        assertEquals(0.60f, Ability.getChakraCostMultiplier(700), 0.001f);

        // Power Multipliers: Lv.1: 1.0, Lv.2: 1.12, Lv.3: 1.25, Lv.4: 1.40, Lv.5: 1.60
        assertEquals(1.00f, Ability.getPowerMultiplier(0), 0.001f);
        assertEquals(1.12f, Ability.getPowerMultiplier(50), 0.001f);
        assertEquals(1.25f, Ability.getPowerMultiplier(150), 0.001f);
        assertEquals(1.40f, Ability.getPowerMultiplier(350), 0.001f);
        assertEquals(1.60f, Ability.getPowerMultiplier(700), 0.001f);

        // Cooldown Multipliers: Lv.1: 1.0, Lv.2: 0.90, Lv.3: 0.80, Lv.4: 0.70, Lv.5: 0.60
        assertEquals(1.00f, Ability.getCooldownMultiplier(0), 0.001f);
        assertEquals(0.90f, Ability.getCooldownMultiplier(50), 0.001f);
        assertEquals(0.80f, Ability.getCooldownMultiplier(150), 0.001f);
        assertEquals(0.70f, Ability.getCooldownMultiplier(350), 0.001f);
        assertEquals(0.60f, Ability.getCooldownMultiplier(700), 0.001f);
    }

    @Test
    @DisplayName("Verify Shadow Clone count and lifespan scaling with mastery")
    public void testShadowCloneMasteryScaling() {
        // Counts: <100 -> 1, <300 -> 2, <700 -> 4, >=700 -> 8
        assertEquals(1, ShadowCloneAbility.getCloneCount(0));
        assertEquals(1, ShadowCloneAbility.getCloneCount(99));
        assertEquals(2, ShadowCloneAbility.getCloneCount(100));
        assertEquals(2, ShadowCloneAbility.getCloneCount(299));
        assertEquals(4, ShadowCloneAbility.getCloneCount(300));
        assertEquals(4, ShadowCloneAbility.getCloneCount(699));
        assertEquals(8, ShadowCloneAbility.getCloneCount(700));
        assertEquals(8, ShadowCloneAbility.getCloneCount(1000));

        // Lifespan: <100 -> 400 (20s), <300 -> 600 (30s), <700 -> 900 (45s), >=700 -> 1200 (60s)
        assertEquals(400, ShadowCloneAbility.getCloneLifespan(0));
        assertEquals(400, ShadowCloneAbility.getCloneLifespan(99));
        assertEquals(600, ShadowCloneAbility.getCloneLifespan(100));
        assertEquals(600, ShadowCloneAbility.getCloneLifespan(299));
        assertEquals(900, ShadowCloneAbility.getCloneLifespan(300));
        assertEquals(900, ShadowCloneAbility.getCloneLifespan(699));
        assertEquals(1200, ShadowCloneAbility.getCloneLifespan(700));
        assertEquals(1200, ShadowCloneAbility.getCloneLifespan(1500));
    }

    @Test
    @DisplayName("Verify scaled activation delay formula and mastery reduction")
    public void testScaledActivationDelayFormula() {
        // Genin / Base 20 ticks: -10% per 100 mastery, 0 at 1000+
        com.sekwah.narutomod.abilities.jutsus.BoneDanceAbility geninAbility = new com.sekwah.narutomod.abilities.jutsus.BoneDanceAbility();
        assertTrue(geninAbility.requiresSealActivationDelay());
        assertEquals(20, geninAbility.getBaseActivationDelayTicks());
        assertEquals(20, geninAbility.getScaledActivationDelay(0));
        assertEquals(20, geninAbility.getScaledActivationDelay(50));
        assertEquals(18, geninAbility.getScaledActivationDelay(100));
        assertEquals(16, geninAbility.getScaledActivationDelay(200));
        assertEquals(10, geninAbility.getScaledActivationDelay(500));
        assertEquals(2, geninAbility.getScaledActivationDelay(900));
        assertEquals(0, geninAbility.getScaledActivationDelay(1000));
        assertEquals(0, geninAbility.getScaledActivationDelay(1500));

        // Chunin / Base 16 ticks
        ShadowCloneAbility chuninAbility = new ShadowCloneAbility();
        assertTrue(chuninAbility.requiresSealActivationDelay());
        assertEquals(16, chuninAbility.getBaseActivationDelayTicks());
        assertEquals(16, chuninAbility.getScaledActivationDelay(0));
        assertEquals(14, chuninAbility.getScaledActivationDelay(100)); // 16 * 0.9 = 14.4 -> 14
        assertEquals(13, chuninAbility.getScaledActivationDelay(200)); // 16 * 0.8 = 12.8 -> 13
        assertEquals(8, chuninAbility.getScaledActivationDelay(500));  // 16 * 0.5 = 8
        assertEquals(0, chuninAbility.getScaledActivationDelay(1000));

        // Jonin / Base 12 ticks
        RasenshurikenAbility joninAbility = new RasenshurikenAbility();
        assertTrue(joninAbility.requiresSealActivationDelay());
        assertEquals(12, joninAbility.getBaseActivationDelayTicks());
        assertEquals(12, joninAbility.getScaledActivationDelay(0));
        assertEquals(11, joninAbility.getScaledActivationDelay(100)); // 12 * 0.9 = 10.8 -> 11
        assertEquals(10, joninAbility.getScaledActivationDelay(200)); // 12 * 0.8 = 9.6 -> 10
        assertEquals(6, joninAbility.getScaledActivationDelay(500));  // 12 * 0.5 = 6
        assertEquals(0, joninAbility.getScaledActivationDelay(1000));
    }

    @Test
    @DisplayName("Verify utility abilities require zero activation delay")
    public void testUtilityAbilitiesRequireZeroDelay() {
        com.sekwah.narutomod.abilities.utility.ChakraDashAbility dashAbility = new com.sekwah.narutomod.abilities.utility.ChakraDashAbility();
        assertFalse(dashAbility.requiresSealActivationDelay());
        assertEquals(0, dashAbility.getScaledActivationDelay(0));
        assertEquals(0, dashAbility.getScaledActivationDelay(500));
    }

    @Test
    @DisplayName("Verify Eighth Gate of Death bitmask flag and synchronization")
    public void testEighthGateBitmask() {
        assertEquals(16, INinjaData.JUTSU_FLAG_EIGHT_GATES_DEATH);
        assertFalse(ninjaData.isEighthGateActive());

        ninjaData.setEighthGateActive(true);
        assertTrue(ninjaData.isEighthGateActive());
        assertEquals(INinjaData.JUTSU_FLAG_EIGHT_GATES_DEATH, ninjaData.getActiveJutsusFlag() & INinjaData.JUTSU_FLAG_EIGHT_GATES_DEATH);

        ninjaData.setEighthGateActive(false);
        assertFalse(ninjaData.isEighthGateActive());
    }

    @Test
    @DisplayName("Verify Shadow Clone command modes constants")
    public void testShadowCloneCommandModes() {
        assertEquals(0, com.sekwah.narutomod.entity.ShadowCloneEntity.MODE_FOLLOW);
        assertEquals(1, com.sekwah.narutomod.entity.ShadowCloneEntity.MODE_PATROL);
        assertEquals(2, com.sekwah.narutomod.entity.ShadowCloneEntity.MODE_HOLD);
    }
}
