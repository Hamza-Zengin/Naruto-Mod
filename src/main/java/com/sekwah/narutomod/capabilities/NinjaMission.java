package com.sekwah.narutomod.capabilities;

/**
 * Shinobi Mission Board definitions (D-Rank through S-Rank).
 */
public enum NinjaMission {
    D_RANK_PATROL("d_rank_patrol", "D-Rank", NinjaRank.ACADEMY_STUDENT, TargetType.KILL_ENEMY, 10, 25),
    C_RANK_ROGUE_HUNT("c_rank_rogue_hunt", "C-Rank", NinjaRank.GENIN, TargetType.KILL_ROGUE, 5, 60),
    B_RANK_JUTSU_DRILL("b_rank_jutsu_drill", "B-Rank", NinjaRank.CHUNIN, TargetType.CAST_JUTSU, 20, 100),
    A_RANK_AKATSUKI_RECON("a_rank_akatsuki_recon", "A-Rank", NinjaRank.JONIN, TargetType.KILL_AKATSUKI, 2, 200),
    S_RANK_AKATSUKI_BOUNTY("s_rank_akatsuki_bounty", "S-Rank", NinjaRank.JONIN, TargetType.KILL_AKATSUKI, 5, 450);

    public enum MissionStatus {
        NONE,
        IN_PROGRESS,
        COMPLETED;

        public static MissionStatus fromString(String value) {
            if (value == null || value.isEmpty()) return NONE;
            for (MissionStatus s : values()) {
                if (s.name().equalsIgnoreCase(value)) return s;
            }
            return NONE;
        }
    }

    public enum TargetType {
        KILL_ENEMY,
        KILL_ROGUE,
        CAST_JUTSU,
        KILL_AKATSUKI
    }

    private final String id;
    private final String rankLabel;
    private final NinjaRank requiredRank;
    private final TargetType targetType;
    private final int targetCount;
    private final int rewardXp;

    NinjaMission(String id, String rankLabel, NinjaRank requiredRank, TargetType targetType, int targetCount, int rewardXp) {
        this.id = id;
        this.rankLabel = rankLabel;
        this.requiredRank = requiredRank;
        this.targetType = targetType;
        this.targetCount = targetCount;
        this.rewardXp = rewardXp;
    }

    public String getId() { return id; }
    public String getRankLabel() { return rankLabel; }
    public NinjaRank getRequiredRank() { return requiredRank; }
    public TargetType getTargetType() { return targetType; }
    public int getTargetCount() { return targetCount; }
    public int getRewardXp() { return rewardXp; }
    public String getTranslationKey() { return "naruto.mission." + id; }

    public boolean canAccept(NinjaRank playerRank) {
        return playerRank != null && playerRank.getLevel() >= this.requiredRank.getLevel();
    }

    public int getProgress(INinjaData data) {
        if (data == null) return 0;
        if (this.id.equalsIgnoreCase(data.getActiveMissionId())) {
            return Math.min(data.getMissionProgress(), this.targetCount);
        }
        return 0;
    }

    public boolean isCompleted(INinjaData data) {
        if (data == null) return false;
        return this.id.equalsIgnoreCase(data.getActiveMissionId())
                && (MissionStatus.fromString(data.getMissionStatus()) == MissionStatus.COMPLETED
                || data.getMissionProgress() >= this.targetCount);
    }

    public static NinjaMission fromId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (NinjaMission m : values()) {
            if (m.id.equalsIgnoreCase(id)) return m;
        }
        return null;
    }
}
