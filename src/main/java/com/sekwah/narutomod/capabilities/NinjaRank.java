package com.sekwah.narutomod.capabilities;

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
