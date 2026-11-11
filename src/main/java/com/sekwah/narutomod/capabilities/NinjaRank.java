package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum NinjaRank {
    ACADEMY_STUDENT(0, "Academy Student", "Akademi Öğrencisi", 0.0f, 0.0f),
    GENIN(1, "Genin", "Genin", 25.0f, 20.0f),
    CHUNIN(2, "Chunin", "Chūnin", 60.0f, 45.0f),
    JONIN(3, "Jonin", "Jōnin", 110.0f, 80.0f),
    ANBU(4, "Anbu", "Anbu", 160.0f, 110.0f),
    KAGE(5, "Kage", "Kage", 250.0f, 160.0f);

    private final int level;
    private final String englishName;
    private final String turkishName;
    private final float chakraBonus;
    private final float staminaBonus;

    NinjaRank(int level, String englishName, String turkishName, float chakraBonus, float staminaBonus) {
        this.level = level;
        this.englishName = englishName;
        this.turkishName = turkishName;
        this.chakraBonus = chakraBonus;
        this.staminaBonus = staminaBonus;
    }

    public int getLevel() {
        return level;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getTurkishName() {
        return turkishName;
    }

    public String getDisplayName() {
        return englishName;
    }

    public float getChakraBonus() {
        return chakraBonus;
    }

    public float getStaminaBonus() {
        return staminaBonus;
    }

    public NinjaRank next() {
        int nextLevel = this.level + 1;
        for (NinjaRank rank : values()) {
            if (rank.level == nextLevel) {
                return rank;
            }
        }
        return null;
    }

    public String getTranslationKey() {
        return "rank.narutomod." + this.name().toLowerCase(java.util.Locale.ROOT);
    }

    public float getRequiredChakraToAdvance() {
        return switch (this) {
            case ACADEMY_STUDENT -> 120.0f;
            case GENIN -> 160.0f;
            case CHUNIN -> 260.0f;
            case JONIN -> 380.0f;
            case ANBU -> 500.0f;
            case KAGE -> 0.0f;
        };
    }

    public float getRequiredStaminaToAdvance() {
        return switch (this) {
            case ACADEMY_STUDENT -> 100.0f;
            case GENIN -> 130.0f;
            case CHUNIN -> 180.0f;
            case JONIN -> 260.0f;
            case ANBU -> 350.0f;
            case KAGE -> 0.0f;
        };
    }

    public int getRequiredEnemiesToAdvance() {
        return switch (this) {
            case ACADEMY_STUDENT -> 0;
            case GENIN -> 15;
            case CHUNIN -> 60;
            case JONIN -> 120;
            case ANBU -> 250;
            case KAGE -> 0;
        };
    }

    public int getRequiredJutsusToAdvance() {
        return switch (this) {
            case ACADEMY_STUDENT -> 8; // jutsus cast count
            case GENIN -> 0;
            case CHUNIN -> 3; // unlocked jutsus count
            case JONIN -> 4;
            case ANBU -> 5;
            case KAGE -> 0;
        };
    }

    public boolean canRankUp(INinjaData data) {
        if (data == null) return false;
        if (this.next() == null) return false;

        boolean chakraOk = data.getBaseMaxChakra() >= this.getRequiredChakraToAdvance();
        boolean staminaOk = data.getBaseMaxStamina() >= this.getRequiredStaminaToAdvance();

        return switch (this) {
            case ACADEMY_STUDENT -> chakraOk && data.getJutsusCastCount() >= this.getRequiredJutsusToAdvance();
            case GENIN -> chakraOk && staminaOk && data.getRogueNinjasDefeated() >= 10 && countMasteredJutsus(data, 50) >= 3;
            case CHUNIN -> chakraOk && staminaOk && data.getAkatsukiNinjasDefeated() >= 1 && hasMasteredAdvancedJutsu(data, 50);
            case JONIN, ANBU -> chakraOk && staminaOk && data.getEnemiesDefeated() >= this.getRequiredEnemiesToAdvance() && data.getUnlockedAbilities().size() >= this.getRequiredJutsusToAdvance();
            case KAGE -> false;
        };
    }

    public static int countMasteredJutsus(INinjaData data, int minMastery) {
        if (data == null || data.getJutsuMasteryMap() == null) return 0;
        int count = 0;
        for (int val : data.getJutsuMasteryMap().values()) {
            if (val >= minMastery) {
                count++;
            }
        }
        return count;
    }

    public static boolean hasMasteredAdvancedJutsu(INinjaData data, int minMastery) {
        if (data == null || data.getJutsuMasteryMap() == null) return false;
        for (Map.Entry<ResourceLocation, Integer> entry : data.getJutsuMasteryMap().entrySet()) {
            if (entry.getValue() != null && entry.getValue() >= minMastery && isAdvancedJutsu(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdvancedJutsu(ResourceLocation id) {
        if (id == null) return false;
        if (NinjaData.isScrollRequiredJutsu(id)) return true;
        String path = id.getPath();
        if (path.equals("rasenshuriken") || path.equals("kirin") || path.equals("amaterasu") || path.equals("susanoo_ribcage")) {
            return true;
        }
        try {
            Ability ability = NarutoAbilities.getAbility(id);
            if (ability != null && ability.getRequiredRank().getLevel() >= JONIN.getLevel()) {
                return true;
            }
        } catch (Throwable ignored) {
            // Standalone unit tests without registry
        }
        return false;
    }

    public List<Component> getMissingRequirements(INinjaData data) {
        List<Component> missing = new ArrayList<>();
        if (data == null) return missing;
        if (this.next() == null) {
            missing.add(Component.translatable("naruto.gui.ninja_menu.kage_congrats"));
            return missing;
        }

        if (data.getBaseMaxChakra() < this.getRequiredChakraToAdvance()) {
            missing.add(Component.translatable("naruto.rank.req.chakra", (int) data.getBaseMaxChakra(), (int) this.getRequiredChakraToAdvance()));
        }
        if (data.getBaseMaxStamina() < this.getRequiredStaminaToAdvance()) {
            missing.add(Component.translatable("naruto.rank.req.stamina", (int) data.getBaseMaxStamina(), (int) this.getRequiredStaminaToAdvance()));
        }

        switch (this) {
            case ACADEMY_STUDENT -> {
                if (data.getJutsusCastCount() < this.getRequiredJutsusToAdvance()) {
                    missing.add(Component.translatable("naruto.rank.req.jutsu_casts", data.getJutsusCastCount(), this.getRequiredJutsusToAdvance()));
                }
            }
            case GENIN -> {
                int mastered = countMasteredJutsus(data, 50);
                if (mastered < 3) {
                    missing.add(Component.translatable("naruto.rank.req.jutsu_mastery", mastered, 3));
                }
                int rogue = data.getRogueNinjasDefeated();
                if (rogue < 10) {
                    missing.add(Component.translatable("naruto.rank.req.rogue_kills", rogue, 10));
                }
            }
            case CHUNIN -> {
                if (!hasMasteredAdvancedJutsu(data, 50)) {
                    missing.add(Component.translatable("naruto.rank.req.advanced_jutsu"));
                }
                int akatsuki = data.getAkatsukiNinjasDefeated();
                if (akatsuki < 1) {
                    missing.add(Component.translatable("naruto.rank.req.akatsuki_kills", akatsuki, 1));
                }
            }
            case JONIN, ANBU -> {
                if (data.getEnemiesDefeated() < this.getRequiredEnemiesToAdvance()) {
                    missing.add(Component.translatable("naruto.rank.req.enemies", data.getEnemiesDefeated(), this.getRequiredEnemiesToAdvance()));
                }
                if (data.getUnlockedAbilities().size() < this.getRequiredJutsusToAdvance()) {
                    missing.add(Component.translatable("naruto.rank.req.scrolls", data.getUnlockedAbilities().size(), this.getRequiredJutsusToAdvance()));
                }
            }
            case KAGE -> {
            }
        }

        return missing;
    }

    public static NinjaRank parseExact(String name) {
        if (name == null) return null;
        String clean = name.trim().replace('ı', 'i').replace('İ', 'I').toLowerCase(java.util.Locale.ROOT);
        for (NinjaRank rank : values()) {
            if (rank.name().toLowerCase(java.util.Locale.ROOT).equals(clean)
                    || rank.englishName.toLowerCase(java.util.Locale.ROOT).replace(" ", "_").equals(clean)
                    || rank.englishName.toLowerCase(java.util.Locale.ROOT).equals(clean)
                    || rank.turkishName.toLowerCase(java.util.Locale.ROOT).replace('ı', 'i').replace('İ', 'I').replace(" ", "_").equals(clean)
                    || rank.turkishName.toLowerCase(java.util.Locale.ROOT).replace('ı', 'i').replace('İ', 'I').equals(clean)) {
                return rank;
            }
        }
        return null;
    }

    public static NinjaRank fromString(String name) {
        NinjaRank exact = parseExact(name);
        return exact != null ? exact : ACADEMY_STUDENT;
    }
}
