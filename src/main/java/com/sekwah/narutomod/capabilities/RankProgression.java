package com.sekwah.narutomod.capabilities;

/**
 * Stateless helper for shinobi rank progression, capped rank counters, and rank-up transitions.
 */
public final class RankProgression {

    private RankProgression() {}

    public static int getEnemiesDefeated(String rank, int rawEnemiesDefeated) {
        NinjaRank current = NinjaRank.fromString(rank);
        return switch (current) {
            case GENIN -> Math.min(rawEnemiesDefeated, 25);
            case CHUNIN -> Math.min(rawEnemiesDefeated, 75);
            case JONIN -> Math.min(rawEnemiesDefeated, 150);
            case ANBU -> Math.min(rawEnemiesDefeated, 300);
            default -> rawEnemiesDefeated;
        };
    }

    public static void addEnemyDefeated(NinjaData data) {
        NinjaRank current = NinjaRank.fromString(data.getRank());
        int raw = data.getRawEnemiesDefeated();
        int cap = switch (current) {
            case GENIN -> 25;
            case CHUNIN -> 75;
            case JONIN -> 150;
            case ANBU -> 300;
            default -> Integer.MAX_VALUE;
        };
        if (raw < cap) {
            data.setEnemiesDefeated(raw + 1);
        }
    }

    public static int getJutsusCastCount(String rank, int rawJutsusCastCount) {
        NinjaRank current = NinjaRank.fromString(rank);
        if (current == NinjaRank.ACADEMY_STUDENT) {
            return Math.min(rawJutsusCastCount, 10);
        }
        return rawJutsusCastCount;
    }

    public static void addJutsuCast(NinjaData data) {
        NinjaRank current = NinjaRank.fromString(data.getRank());
        int raw = data.getRawJutsusCastCount();
        if (current == NinjaRank.ACADEMY_STUDENT) {
            if (raw < 10) {
                data.setJutsusCastCount(raw + 1);
            }
        } else {
            data.setJutsusCastCount(raw + 1);
        }
        NinjaMission active = NinjaMission.fromId(data.getActiveMissionId());
        if (active != null && active.getTargetType() == NinjaMission.TargetType.CAST_JUTSU
                && NinjaMission.MissionStatus.fromString(data.getMissionStatus()) == NinjaMission.MissionStatus.IN_PROGRESS) {
            data.incrementMissionProgress(1);
        }
    }

    public static boolean canRankUp(INinjaData data) {
        return NinjaRank.fromString(data.getRank()).canRankUp(data);
    }

    public static boolean rankUp(NinjaData data) {
        if (!canRankUp(data)) return false;
        NinjaRank current = NinjaRank.fromString(data.getRank());
        NinjaRank next = current.next();
        if (next == null) return false;

        data.setRank(next.name());
        data.setJutsusCastCount(0);
        data.setEnemiesDefeated(0);
        data.setRawChakra(data.getMaxChakra());
        data.setRawStamina(data.getMaxStamina());
        return true;
    }
}
