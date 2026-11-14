package com.sekwah.narutomod.capabilities;

/**
 * Shinobi Mission Board definitions (D-Rank through S-Rank).
 */
public enum NinjaMission {
    D_RANK_PATROL("d_rank_patrol", "D-Rank", 10, 25),
    C_RANK_ROGUE_HUNT("c_rank_rogue_hunt", "C-Rank", 5, 60),
    B_RANK_JUTSU_DRILL("b_rank_jutsu_drill", "B-Rank", 20, 100),
    A_RANK_AKATSUKI_RECON("a_rank_akatsuki_recon", "A-Rank", 2, 200),
    S_RANK_AKATSUKI_BOUNTY("s_rank_akatsuki_bounty", "S-Rank", 5, 450);

    private final String id;
    private final String rankLabel;
    private final int targetCount;
    private final int rewardXp;

    NinjaMission(String id, String rankLabel, int targetCount, int rewardXp) {
        this.id = id;
        this.rankLabel = rankLabel;
        this.targetCount = targetCount;
        this.rewardXp = rewardXp;
    }

    public String getId() { return id; }
    public String getRankLabel() { return rankLabel; }
    public int getTargetCount() { return targetCount; }
    public int getRewardXp() { return rewardXp; }
    public String getTranslationKey() { return "naruto.mission." + id; }

    public int getProgress(INinjaData data) {
        if (data == null) return 0;
        return switch (this) {
            case D_RANK_PATROL -> data.getEnemiesDefeated();
            case C_RANK_ROGUE_HUNT -> data.getRogueNinjaKills();
            case B_RANK_JUTSU_DRILL -> data.getJutsusCastCount();
            case A_RANK_AKATSUKI_RECON, S_RANK_AKATSUKI_BOUNTY -> data.getAkatsukiKills();
        };
    }

    public boolean isCompleted(INinjaData data) {
        return getProgress(data) >= targetCount;
    }

    public static NinjaMission fromId(String id) {
        for (NinjaMission m : values()) {
            if (m.id.equalsIgnoreCase(id)) return m;
        }
        return null;
    }
}
