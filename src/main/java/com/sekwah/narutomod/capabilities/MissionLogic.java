package com.sekwah.narutomod.capabilities;

/**
 * Stateless helper for ninja mission lifecycle (accept, progress, abandon, claim reward).
 */
public final class MissionLogic {

    private MissionLogic() {}

    public static void setMissionProgress(NinjaData data, int progress) {
        int clamped = Math.max(0, progress);
        NinjaMission active = NinjaMission.fromId(data.getActiveMissionId());
        if (active != null && clamped >= active.getTargetCount()) {
            data.setRawMissionProgress(active.getTargetCount());
            data.setMissionStatus(NinjaMission.MissionStatus.COMPLETED.name());
        } else {
            data.setRawMissionProgress(clamped);
        }
    }

    public static boolean acceptMission(NinjaData data, NinjaMission mission) {
        if (mission == null) return false;
        data.setActiveMissionId(mission.getId());
        data.setRawMissionProgress(0);
        data.setMissionStatus(NinjaMission.MissionStatus.IN_PROGRESS.name());
        return true;
    }

    public static boolean incrementMissionProgress(NinjaData data, int amount) {
        NinjaMission active = NinjaMission.fromId(data.getActiveMissionId());
        if (active == null || NinjaMission.MissionStatus.fromString(data.getMissionStatus()) != NinjaMission.MissionStatus.IN_PROGRESS) {
            return false;
        }
        int next = Math.min(active.getTargetCount(), data.getMissionProgress() + Math.max(0, amount));
        data.setRawMissionProgress(next);
        if (next >= active.getTargetCount()) {
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
        NinjaMission active = NinjaMission.fromId(data.getActiveMissionId());
        if (active == null) return false;
        if (NinjaMission.MissionStatus.fromString(data.getMissionStatus()) != NinjaMission.MissionStatus.COMPLETED
                && data.getMissionProgress() < active.getTargetCount()) {
            return false;
        }
        float reward = active.getRewardXp();
        data.addChakraExp(reward * 0.5f);
        data.addStaminaExp(reward * 0.5f);
        data.setCompletedMissionsCount(data.getCompletedMissionsCount() + 1);
        abandonMission(data);
        return true;
    }
}
