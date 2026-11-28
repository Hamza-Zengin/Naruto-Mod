package com.sekwah.narutomod.capabilities;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Stateless helper for ninja mission lifecycle (accept, progress, abandon, claim reward).
 */
public final class MissionLogic {

    private MissionLogic() {}

    public static void setMissionProgress(NinjaData data, int progress) {
        int clamped = Math.max(0, progress);
        MissionDefinition active = MissionDataManager.get(data.getActiveMissionId());
        if (active != null && clamped >= active.targetCount()) {
            data.setRawMissionProgress(active.targetCount());
            data.setMissionStatus(NinjaMission.MissionStatus.COMPLETED.name());
        } else {
            data.setRawMissionProgress(clamped);
        }
    }

    public static boolean acceptMission(NinjaData data, NinjaMission mission) {
        if (mission == null) return false;
        MissionDefinition def = MissionDataManager.get(mission.getId());
        return acceptMission(data, def != null ? def : MissionDefinition.fromLegacy(mission));
    }

    public static boolean acceptMission(NinjaData data, MissionDefinition mission) {
        if (data == null || mission == null) return false;
        data.setActiveMissionId(mission.id());
        data.setRawMissionProgress(0);
        data.setMissionStatus(NinjaMission.MissionStatus.IN_PROGRESS.name());
        return true;
    }

    public static boolean incrementMissionProgress(NinjaData data, int amount) {
        MissionDefinition active = MissionDataManager.get(data.getActiveMissionId());
        if (active == null || NinjaMission.MissionStatus.fromString(data.getMissionStatus()) != NinjaMission.MissionStatus.IN_PROGRESS) {
            return false;
        }
        int next = Math.min(active.targetCount(), data.getMissionProgress() + Math.max(0, amount));
        data.setRawMissionProgress(next);
        if (next >= active.targetCount()) {
            data.setMissionStatus(NinjaMission.MissionStatus.COMPLETED.name());
            return true;
        }
        return false;
    }

    public static void abandonMission(NinjaData data) {
        data.setActiveMissionId("");
        data.setRawMissionProgress(0);
        data.setMissionStatus(NinjaMission.MissionStatus.NONE.name());
    }

    public static boolean claimMissionReward(NinjaData data) {
        return claimMissionReward(data, null);
    }

    public static boolean claimMissionReward(NinjaData data, @Nullable Player player) {
        MissionDefinition active = MissionDataManager.get(data.getActiveMissionId());
        if (active == null) return false;
        if (NinjaMission.MissionStatus.fromString(data.getMissionStatus()) != NinjaMission.MissionStatus.COMPLETED
                && data.getMissionProgress() < active.targetCount()) {
            return false;
        }
        float reward = active.rewardXp();
        data.addChakraExp(reward * 0.5f);
        data.addStaminaExp(reward * 0.5f);
        data.setCompletedMissionsCount(data.getCompletedMissionsCount() + 1);

        if (player != null && !player.level().isClientSide()) {
            for (MissionDefinition.RewardItem rewardItem : active.rewardItems()) {
                BuiltInRegistries.ITEM.getOptional(rewardItem.itemId()).ifPresent(item -> {
                    int remaining = rewardItem.count();
                    int maxStack = Math.max(1, item.getDefaultMaxStackSize());
                    while (remaining > 0) {
                        int give = Math.min(remaining, maxStack);
                        player.getInventory().placeItemBackInInventory(new ItemStack(item, give), Prediction.SERVER_ONLY);
                        remaining -= give;
                    }
                });
            }
        }

        abandonMission(data);
        return true;
    }
}
