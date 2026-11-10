package com.sekwah.narutomod;

import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
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
}
