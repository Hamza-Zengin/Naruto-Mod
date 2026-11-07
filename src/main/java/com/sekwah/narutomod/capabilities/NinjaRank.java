package com.sekwah.narutomod.capabilities;

public enum NinjaRank {
    ACADEMY_STUDENT(0, "Academy Student", "Akademi Öğrencisi"),
    GENIN(1, "Genin", "Genin"),
    CHUNIN(2, "Chunin", "Chūnin"),
    JONIN(3, "Jonin", "Jōnin"),
    ANBU(4, "Anbu", "Anbu"),
    KAGE(5, "Kage", "Kage");

    private final int level;
    private final String englishName;
    private final String turkishName;

    NinjaRank(int level, String englishName, String turkishName) {
        this.level = level;
        this.englishName = englishName;
        this.turkishName = turkishName;
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

    public static NinjaRank fromString(String name) {
        if (name == null) return GENIN;
        for (NinjaRank rank : values()) {
            if (rank.name().equalsIgnoreCase(name) || rank.englishName.equalsIgnoreCase(name) || rank.turkishName.equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return GENIN;
    }
}
