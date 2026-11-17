package com.sekwah.narutomod;

import com.sekwah.narutomod.capabilities.ChakraStaminaLogic;
import com.sekwah.narutomod.capabilities.MissionLogic;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaMission;
import com.sekwah.narutomod.capabilities.RankProgression;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests targeting the extracted ChakraStaminaLogic, RankProgression, and MissionLogic helpers.
 */
public class NinjaDataRefactorTest {

    @Test
    @DisplayName("ChakraStaminaLogic computes Uzumaki 1.25x bonus and RegenInfo cooldown")
    void testChakraStaminaLogic() {
        Identifier uzumaki = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "uzumaki");
        Identifier uchiha = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "uchiha");

        float uchihaMax = ChakraStaminaLogic.computeMaxChakra(100f, "ACADEMY_STUDENT", uchiha);
        float uzumakiMax = ChakraStaminaLogic.computeMaxChakra(100f, "ACADEMY_STUDENT", uzumaki);
        assertEquals(uzumakiMax, uchihaMax * 1.25f, 0.001f);

        ChakraStaminaLogic.RegenInfo regen = new ChakraStaminaLogic.RegenInfo(0.1f);
        regen.cooldown = 2;
        assertFalse(regen.canRegen());
        assertFalse(regen.canRegen());
        assertTrue(regen.canRegen());

        NinjaData data = new NinjaData(true);
        float before = data.getChakra();
        data.useChakra(20f, 10);
        assertEquals(before - 20f, data.getChakra(), 0.01f);
        assertEquals(10, data.getChakraRegenCooldown());
        assertEquals(2.0f, data.getChakraExp(), 0.01f);
    }

    @Test
    @DisplayName("RankProgression caps academy jutsusCastCount at 10 and genin enemiesDefeated at 25")
    void testRankProgressionCaps() {
        assertEquals(10, RankProgression.getJutsusCastCount("ACADEMY_STUDENT", 99));
        assertEquals(99, RankProgression.getJutsusCastCount("GENIN", 99));
        assertEquals(25, RankProgression.getEnemiesDefeated("GENIN", 50));
        assertEquals(75, RankProgression.getEnemiesDefeated("CHUNIN", 100));
    }

    @Test
    @DisplayName("MissionLogic accept, progress, complete, and claim reward lifecycle")
    void testMissionLifecycle() {
        NinjaData data = new NinjaData(true);
        assertTrue(MissionLogic.acceptMission(data, NinjaMission.D_RANK_PATROL));
        assertEquals("d_rank_patrol", data.getActiveMissionId());
        assertEquals("IN_PROGRESS", data.getMissionStatus());

        assertFalse(MissionLogic.incrementMissionProgress(data, 5));
        assertEquals(5, data.getMissionProgress());
        assertTrue(MissionLogic.incrementMissionProgress(data, 10));
        assertEquals(10, data.getMissionProgress());
        assertEquals("COMPLETED", data.getMissionStatus());

        assertTrue(MissionLogic.claimMissionReward(data));
        assertEquals(1, data.getCompletedMissionsCount());
        assertEquals("", data.getActiveMissionId());
        assertEquals("NONE", data.getMissionStatus());
    }
}
