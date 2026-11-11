package com.sekwah.narutomod;

import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
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
}
