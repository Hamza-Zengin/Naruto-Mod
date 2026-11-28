package com.sekwah.narutomod;

import com.sekwah.narutomod.capabilities.MissionDataManager;
import com.sekwah.narutomod.capabilities.MissionDefinition;
import com.sekwah.narutomod.capabilities.MissionLogic;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaMission;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.mob.BingoBookProfile;
import com.sekwah.narutomod.network.s2c.ClientMissionsSyncPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MissionV2AndBingoBookTest {

    @Test
    public void testMissionDefinitionsAndSyncRoundTrip() throws Exception {
        Path missionsDir = Paths.get("src/main/resources/data/narutomod/missions");
        assertTrue(Files.isDirectory(missionsDir));
        try (var stream = Files.list(missionsDir)) {
            long jsonCount = stream.filter(p -> p.toString().endsWith(".json")).count();
            assertTrue(jsonCount >= 9, "Expected at least 9 mission JSON files, found " + jsonCount);
        }

        List<MissionDefinition> all = MissionDataManager.getAll();
        assertFalse(all.isEmpty());
        assertNotNull(MissionDataManager.get("d_rank_patrol"));

        MissionDefinition custom = new MissionDefinition(
                "test_combo_mission",
                "C-Rank",
                NinjaRank.GENIN,
                NinjaMission.TargetType.ELEMENTAL_COMBO,
                3,
                120,
                List.of(new MissionDefinition.RewardItem(Identifier.withDefaultNamespace("emerald"), 4))
        );
        MissionDataManager.applySynced(List.of(custom));
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            ClientMissionsSyncPacket packet = new ClientMissionsSyncPacket(List.of(custom));
            ClientMissionsSyncPacket.encode(packet, buf);
            ClientMissionsSyncPacket decoded = ClientMissionsSyncPacket.decode(buf);
            assertEquals(1, decoded.missions().size());
            MissionDefinition roundTrip = decoded.missions().get(0);
            assertEquals("test_combo_mission", roundTrip.id());
            assertEquals(NinjaMission.TargetType.ELEMENTAL_COMBO, roundTrip.targetType());
            assertEquals(1, roundTrip.rewardItems().size());
            assertEquals(4, roundTrip.rewardItems().get(0).count());

            NinjaData data = new NinjaData(true);
            assertTrue(MissionLogic.acceptMission(data, roundTrip));
            assertFalse(MissionLogic.incrementMissionProgress(data, 1));
            assertFalse(MissionLogic.incrementMissionProgress(data, 1));
            assertTrue(MissionLogic.incrementMissionProgress(data, 1));
            assertTrue(MissionLogic.claimMissionReward(data, null));
            assertEquals(1, data.getCompletedMissionsCount());
        } finally {
            MissionDataManager.applySynced(all);
        }
    }

    @Test
    public void testMasterJutsuProgressOnTierAdvance() {
        NinjaData data = new NinjaData(true);
        MissionDefinition masterMission = new MissionDefinition(
                "test_master_jutsu",
                "B-Rank",
                NinjaRank.CHUNIN,
                NinjaMission.TargetType.MASTER_JUTSU,
                1,
                140,
                List.of()
        );
        List<MissionDefinition> snapshot = MissionDataManager.getAll();
        MissionDataManager.applySynced(List.of(masterMission));
        try {
            MissionLogic.acceptMission(data, masterMission);
            Identifier fireball = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "fireball");
            data.addMastery(fireball, 50); // crosses Tier 1 -> Tier 2 threshold (20)
            assertEquals(NinjaMission.MissionStatus.COMPLETED.name(), data.getMissionStatus());
        } finally {
            MissionDataManager.applySynced(snapshot);
        }
    }

    @Test
    public void testBingoBookProfileGeneration() {
        BingoBookProfile aRank = BingoBookProfile.generateDeterministic(12345L, BingoBookProfile.RankTier.A);
        BingoBookProfile sRank = BingoBookProfile.generateDeterministic(67890L, BingoBookProfile.RankTier.S);

        assertEquals(BingoBookProfile.RankTier.A, aRank.rankTier());
        assertEquals(BingoBookProfile.RankTier.S, sRank.rankTier());
        assertTrue(aRank.formatWantedName().getString().contains("[A-Rank]"));
        assertTrue(sRank.formatWantedName().getString().contains("[S-Rank]"));
        assertNotNull(aRank.jutsuProfile());
        assertNotNull(sRank.jutsuProfile());
        assertTrue(BingoBookProfile.getBonusEmeraldCount(BingoBookProfile.RankTier.S, RandomSource.create(1L))
                >= BingoBookProfile.getBonusEmeraldCount(BingoBookProfile.RankTier.A, RandomSource.create(1L)));
        assertTrue(BingoBookProfile.getBlankScrollCount(BingoBookProfile.RankTier.S, RandomSource.create(1L))
                >= BingoBookProfile.getBlankScrollCount(BingoBookProfile.RankTier.A, RandomSource.create(1L)));
    }
}
