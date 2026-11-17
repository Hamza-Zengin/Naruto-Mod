package com.sekwah.narutomod;

import com.sekwah.narutomod.capabilities.NinjaData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip NBT serialization test pinning NinjaData save/load format
 * before refactoring NinjaData into smaller components.
 */
public class NinjaDataSerializationTest {

    @Test
    @DisplayName("NinjaData saveNBTData/loadNBTData preserves all persisted fields")
    void testSaveAndLoadNbtRoundTrip() {
        NinjaData original = new NinjaData(true);
        original.setBaseMaxChakra(250.0f);
        original.setBaseMaxStamina(180.0f);
        original.setClan(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "uchiha"));
        original.setClanLocked(true);
        original.setRank("CHUNIN");
        original.setChakra(123.5f);
        original.setStamina(87.25f);
        original.setChakraExp(45.5f);
        original.setStaminaExp(30.25f);
        original.setEnemiesDefeated(42);
        original.setRogueNinjasDefeated(12);
        original.setAkatsukiNinjasDefeated(3);
        original.setJutsusCastCount(10);
        original.setUnlockedJutsusString("narutomod:fireball,narutomod:sharingan");
        original.setMastery(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "fireball"), 150);
        original.setActiveHandJutsu("chidori", 40);
        original.setSharinganActive(true);
        original.addSeal((byte) 1);
        original.addSeal((byte) 2);
        original.setActiveMissionId("genin_patrol");
        original.setMissionProgress(2);
        original.setCompletedMissionsCount(5);
        original.setMissionStatus("IN_PROGRESS");
        original.lockChakraRegen(15);
        original.setShimonPenaltyActive(true);

        CompoundTag tag = new CompoundTag();
        original.saveNBTData(tag);

        NinjaData restored = new NinjaData(true);
        restored.loadNBTData(tag);

        assertEquals(original.getMaxChakra(), restored.getMaxChakra(), 0.01f);
        assertEquals(original.getMaxStamina(), restored.getMaxStamina(), 0.01f);
        assertEquals(original.getChakra(), restored.getChakra(), 0.01f);
        assertEquals(original.getStamina(), restored.getStamina(), 0.01f);
        assertEquals(original.getClan(), restored.getClan());
        assertTrue(restored.isClanLocked());
        assertEquals("CHUNIN", restored.getRank());
        assertEquals(45.5f, restored.getChakraExp(), 0.01f);
        assertEquals(30.25f, restored.getStaminaExp(), 0.01f);
        assertEquals(42, restored.getEnemiesDefeated());
        assertEquals(12, restored.getRogueNinjasDefeated());
        assertEquals(3, restored.getAkatsukiNinjasDefeated());
        assertEquals(10, restored.getJutsusCastCount());
        assertEquals("narutomod:fireball,narutomod:sharingan", restored.getUnlockedJutsusString());
        assertEquals(150, restored.getMastery(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "fireball")));
        assertEquals("chidori", restored.getActiveHandJutsu());
        assertEquals(40, restored.getActiveHandJutsuTicks());
        assertTrue(restored.isSharinganActive());
        assertEquals(2, restored.getActiveSeals().size());
        assertEquals((byte) 1, restored.getActiveSeals().get(0));
        assertEquals((byte) 2, restored.getActiveSeals().get(1));
        assertEquals("genin_patrol", restored.getActiveMissionId());
        assertEquals(2, restored.getMissionProgress());
        assertEquals(5, restored.getCompletedMissionsCount());
        assertEquals("IN_PROGRESS", restored.getMissionStatus());
        assertEquals(15, restored.getChakraRegenCooldown());
        assertTrue(restored.isShimonPenaltyActive());
    }
}
