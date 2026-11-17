package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.abilities.Ability;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

/**
 * Pure rules for evaluating jutsu unlock eligibility and managing the
 * comma-delimited unlocked jutsu list stored on {@link NinjaData}.
 */
public final class JutsuUnlockRules {

    private JutsuUnlockRules() {}

    public static boolean isScrollRequiredJutsu(Identifier id) {
        if (id == null) return false;
        String path = id.getPath();
        return path.equals("rasengan") || path.equals("chidori") || path.equals("shadow_clone")
                || path.equals("kirin") || path.equals("amaterasu") || path.equals("susanoo_ribcage")
                || path.equals("eight_gates") || path.equals("eight_gates_death");
    }

    public static boolean isClanRestrictedJutsu(Identifier id) {
        if (id == null) return false;
        String path = id.getPath();
        return path.equals("amaterasu")
                || path.equals("susanoo_ribcage")
                || path.equals("sharingan")
                || path.equals("tsukuyomi")
                || path.equals("byakugan")
                || path.equals("hakke_kusho")
                || path.equals("bone_dance")
                || path.equals("wood_prison")
                || path.equals("shadow_possession");
    }

    public static boolean checkAbilityUnlocked(INinjaData data, Ability ability, Identifier abilityId) {
        if (ability == null || data == null) return false;

        if (!ability.getPrerequisites().isEmpty() && !ability.meetsPrerequisites(data)) {
            return false;
        }

        if (ability.getRequiredRank() == NinjaRank.ACADEMY_STUDENT) {
            return true;
        }

        Identifier clan = data.getClan();
        String rankStr = data.getRank();
        NinjaRank playerRank = NinjaRank.fromString(rankStr);

        if (abilityId != null) {
            String path = abilityId.getPath();
            if (path.equals("sharingan")) {
                return clan != null && clan.getPath().equals("uchiha") && playerRank.getLevel() >= NinjaRank.GENIN.getLevel();
            }
            if (path.equals("byakugan")) {
                return clan != null && clan.getPath().equals("hyuga") && playerRank.getLevel() >= NinjaRank.GENIN.getLevel();
            }
            if (path.equals("shadow_possession")) {
                if (clan == null || !clan.getPath().equals("nara")) return false;
            } else if (path.equals("tsukuyomi")) {
                if (clan == null || !clan.getPath().equals("uchiha")) return false;
            } else if (path.equals("hakke_kusho")) {
                if (clan == null || !clan.getPath().equals("hyuga")) return false;
            } else if (path.equals("bone_dance")) {
                if (clan == null || !clan.getPath().equals("kaguya")) return false;
            } else if (path.equals("wood_prison")) {
                if (clan == null || !clan.getPath().equals("senju")) return false;
            }

            if (isScrollRequiredJutsu(abilityId)) {
                if (path.equals("amaterasu") || path.equals("susanoo_ribcage")) {
                    if (clan == null || !clan.getPath().equals("uchiha")) {
                        return false;
                    }
                }
                if (path.equals("eight_gates") && playerRank.getLevel() < NinjaRank.JONIN.getLevel()) {
                    return false;
                }
                if (data.getUnlockedAbilities().contains(abilityId)) {
                    return true;
                }
                if (!ability.getPrerequisites().isEmpty() && playerRank.getLevel() >= ability.getRequiredRank().getLevel()) {
                    return true;
                }
                return false;
            }
        }

        if (!ability.getPrerequisites().isEmpty()) {
            if (!ability.meetsPrerequisites(data)) {
                return false;
            }
            if (playerRank.getLevel() >= ability.getRequiredRank().getLevel()
                    || (abilityId != null && data.getUnlockedAbilities().contains(abilityId))) {
                return true;
            }
        }

        if (abilityId != null && data.getUnlockedAbilities().contains(abilityId)) {
            return true;
        }

        return playerRank.getLevel() >= ability.getRequiredRank().getLevel();
    }

    public static Set<Identifier> parseUnlockedAbilities(String unlockedJutsus) {
        HashSet<Identifier> set = new HashSet<>();
        if (unlockedJutsus != null && !unlockedJutsus.isEmpty()) {
            for (String s : unlockedJutsus.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    Identifier parsed = Identifier.tryParse(trimmed);
                    if (parsed != null) {
                        set.add(parsed);
                    }
                }
            }
        }
        return set;
    }

    public static String addUnlockedAbility(String current, Identifier abilityId) {
        if (abilityId == null) return current != null ? current : "";
        Set<Identifier> unlocked = parseUnlockedAbilities(current);
        if (unlocked.contains(abilityId)) {
            return current != null ? current : "";
        }
        if (current == null || current.isEmpty()) {
            return abilityId.toString();
        }
        return current + "," + abilityId;
    }

    public static String removeUnlockedAbility(String current, Identifier abilityId) {
        if (abilityId == null) return current != null ? current : "";
        Set<Identifier> unlocked = parseUnlockedAbilities(current);
        if (!unlocked.remove(abilityId)) {
            return current != null ? current : "";
        }
        StringBuilder sb = new StringBuilder();
        for (Identifier loc : unlocked) {
            if (sb.length() > 0) sb.append(",");
            sb.append(loc.toString());
        }
        return sb.toString();
    }
}
