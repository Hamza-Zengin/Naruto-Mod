package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Helper for jutsu mastery XP/tier calculations, unlock/prerequisite checks,
 * rank progression, and death penalty scaling.
 */
public final class NinjaMasteryHelper {

    private NinjaMasteryHelper() {}

    // --- Jutsu Mastery XP & Tier Calculations ---

    public static int getMastery(Map<Identifier, Integer> masteryMap, Identifier jutsuId) {
        if (masteryMap == null || jutsuId == null) return 0;
        return masteryMap.getOrDefault(jutsuId, 0);
    }

    public static void setMastery(Map<Identifier, Integer> masteryMap, Identifier jutsuId, int amount) {
        if (masteryMap == null || jutsuId == null) return;
        masteryMap.put(jutsuId, Math.max(0, amount));
    }

    public static void addMastery(Map<Identifier, Integer> masteryMap, Identifier jutsuId, int amount) {
        if (masteryMap == null || jutsuId == null || amount == 0) return;
        int current = getMastery(masteryMap, jutsuId);
        int updated = Math.max(0, current + amount);
        masteryMap.put(jutsuId, updated);
    }

    public static int calculateMasteryGain(int currentMastery, boolean isToggle) {
        if (currentMastery < 100) {
            return isToggle ? 2 : 5;
        } else if (currentMastery < 250) {
            return isToggle ? 2 : 3;
        } else if (currentMastery < 500) {
            return isToggle ? 1 : 2;
        }
        return 1;
    }

    public static void incrementJutsuMastery(Map<Identifier, Integer> masteryMap, Identifier jutsuId, boolean isToggle) {
        if (masteryMap == null || jutsuId == null) return;
        int current = getMastery(masteryMap, jutsuId);
        int gain = calculateMasteryGain(current, isToggle);
        addMastery(masteryMap, jutsuId, gain);
    }

    public static int getMasteryTier(int mastery) {
        return Ability.getMasteryTier(mastery);
    }

    // --- Unlock & Prerequisite Checks ---

    public static boolean isScrollRequiredJutsu(Identifier id) {
        return JutsuUnlockRules.isScrollRequiredJutsu(id);
    }

    public static boolean isClanRestrictedJutsu(Identifier id) {
        return JutsuUnlockRules.isClanRestrictedJutsu(id);
    }

    public static boolean isAbilityUnlocked(NinjaData data, Ability ability) {
        if (data == null || ability == null) return false;
        return JutsuUnlockRules.checkAbilityUnlocked(data, ability, ability.getRegistryName());
    }

    public static boolean isAbilityUnlocked(NinjaData data, Identifier abilityId) {
        if (data == null || abilityId == null) return false;
        Ability ability = null;
        try {
            ability = NarutoAbilities.getAbility(abilityId);
        } catch (Throwable ignored) {
            // NarutoAbilities or registry may not be initialized in standalone unit tests
        }
        if (ability == null) {
            return data.getUnlockedAbilities().contains(abilityId);
        }
        return JutsuUnlockRules.checkAbilityUnlocked(data, ability, abilityId);
    }

    public static String unlockAbility(String currentUnlocked, Identifier abilityId) {
        return JutsuUnlockRules.addUnlockedAbility(currentUnlocked, abilityId);
    }

    public static String lockAbility(NinjaData data, String currentUnlocked, Identifier abilityId) {
        String updated = JutsuUnlockRules.removeUnlockedAbility(currentUnlocked, abilityId);
        if (data != null && abilityId != null) {
            for (int i = 0; i < INinjaData.MAX_JUTSU_SLOTS; i++) {
                if (abilityId.equals(data.getEquippedJutsu(i))) {
                    data.setEquippedJutsu(i, null);
                }
            }
        }
        return updated;
    }

    public static Set<Identifier> parseUnlockedAbilities(String source) {
        return Collections.unmodifiableSet(JutsuUnlockRules.parseUnlockedAbilities(source));
    }

    // --- Rank Progression Helpers ---

    public static int getEnemiesDefeated(String rank, int rawEnemiesDefeated) {
        return RankProgression.getEnemiesDefeated(rank, rawEnemiesDefeated);
    }

    public static void addEnemyDefeated(NinjaData data) {
        RankProgression.addEnemyDefeated(data);
    }

    public static int getJutsusCastCount(String rank, int rawJutsusCastCount) {
        return RankProgression.getJutsusCastCount(rank, rawJutsusCastCount);
    }

    public static void addJutsuCast(NinjaData data) {
        RankProgression.addJutsuCast(data);
    }

    public static boolean canRankUp(INinjaData data) {
        return RankProgression.canRankUp(data);
    }

    public static boolean rankUp(NinjaData data) {
        return RankProgression.rankUp(data);
    }

    public static void applyDeathPenalty(INinjaData original, INinjaData future, boolean hasEighthGateToggle) {
        if (original == null || future == null) {
            return;
        }
        boolean isEighthGateDeath = original.isShimonPenaltyActive()
                || original.isEighthGateActive()
                || hasEighthGateToggle;
        float multiplier = isEighthGateDeath ? 0.75f : 0.95f;

        float newBaseMaxChakra = Math.max(100.0f, original.getBaseMaxChakra() * multiplier);
        float newBaseMaxStamina = Math.max(100.0f, original.getBaseMaxStamina() * multiplier);

        future.setBaseMaxChakra(newBaseMaxChakra);
        future.setBaseMaxStamina(newBaseMaxStamina);
        future.setChakra(future.getMaxChakra());
        future.setStamina(future.getMaxStamina());

        future.setChakraExp(original.getChakraExp() * multiplier);
        future.setStaminaExp(original.getStaminaExp() * multiplier);

        for (Map.Entry<Identifier, Integer> entry : original.getJutsuMasteryMap().entrySet()) {
            int penalized = (int) Math.floor(entry.getValue() * multiplier);
            future.setMastery(entry.getKey(), Math.max(0, penalized));
        }

        future.setShimonPenaltyActive(false);
        future.setEighthGateActive(false);
        future.setEightGatesActive(false);
        future.clearActiveHandJutsu();
        if (future.getToggleAbilityData() != null) {
            future.getToggleAbilityData().clear();
        }
    }
}
