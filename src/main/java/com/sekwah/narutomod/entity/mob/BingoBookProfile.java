package com.sekwah.narutomod.entity.mob;

import com.sekwah.narutomod.entity.ai.NinjaJutsuProfile;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Generates Bingo Book wanted criminal profiles (rank tier, shinobi alias, elemental jutsu AI,
 * boosted stats, and bounty loot helpers) for {@link RogueNinjaEntity} mini-bosses.
 */
public record BingoBookProfile(
        RankTier rankTier,
        String alias,
        ElementTheme elementTheme,
        double maxHealth,
        NinjaJutsuProfile jutsuProfile
) {

    public enum RankTier {
        A("A", "A-Rank", 110.0D, 130.0D, 45, 0.25F),
        S("S", "S-Rank", 130.0D, 150.0D, 75, 0.50F);

        private final String code;
        private final String prefix;
        private final double minHealth;
        private final double maxHealth;
        private final int xpReward;
        private final float rareScrollChance;

        RankTier(String code, String prefix, double minHealth, double maxHealth, int xpReward, float rareScrollChance) {
            this.code = code;
            this.prefix = prefix;
            this.minHealth = minHealth;
            this.maxHealth = maxHealth;
            this.xpReward = xpReward;
            this.rareScrollChance = rareScrollChance;
        }

        public String getCode() {
            return this.code;
        }

        public String getPrefix() {
            return this.prefix;
        }

        public double getMinHealth() {
            return this.minHealth;
        }

        public double getMaxHealth() {
            return this.maxHealth;
        }

        public int getXpReward() {
            return this.xpReward;
        }

        public float getRareScrollChance() {
            return this.rareScrollChance;
        }

        public static RankTier fromString(@Nullable String raw) {
            if (raw != null) {
                String norm = raw.trim().toUpperCase(Locale.ROOT);
                if (norm.equals("S") || norm.startsWith("S-")) {
                    return S;
                }
            }
            return A;
        }
    }

    public enum ElementTheme {
        FIRE,
        WATER,
        WIND,
        EARTH,
        LIGHTNING;

        public static ElementTheme fromNameOrSeed(@Nullable String raw, long seed) {
            if (raw != null && !raw.isBlank()) {
                String norm = raw.trim().toUpperCase(Locale.ROOT);
                for (ElementTheme theme : values()) {
                    if (theme.name().equals(norm)) {
                        return theme;
                    }
                }
            }
            ElementTheme[] all = values();
            return all[Math.floorMod(Long.hashCode(seed), all.length)];
        }
    }

    private static final List<String> A_RANK_ALIASES = List.of(
            "Kurogane of the Mist",
            "Sajin of the Dunes",
            "Raikou the Phantom",
            "Kagerou of the Gale",
            "Iwakaze the Breaker",
            "Shiranui the Shadow",
            "Tetsuo the Iron Wall",
            "Ranmaru the Flash"
    );

    private static final List<String> S_RANK_ALIASES = List.of(
            "Homura the Scorcher",
            "Suijin of the Abyss",
            "Hayate the Tempest",
            "Akane of the Crimson Blade",
            "Kurokage the Exile",
            "Raiga of the Storm"
    );

    public static BingoBookProfile generate(RandomSource random) {
        RankTier tier = random.nextFloat() < 0.30F ? RankTier.S : RankTier.A;
        List<String> pool = tier == RankTier.S ? S_RANK_ALIASES : A_RANK_ALIASES;
        String alias = pool.get(random.nextInt(pool.size()));
        ElementTheme[] themes = ElementTheme.values();
        ElementTheme theme = themes[random.nextInt(themes.length)];
        int span = Math.max(1, (int) Math.round(tier.getMaxHealth() - tier.getMinHealth()) + 1);
        double maxHp = tier.getMinHealth() + random.nextInt(span);
        return new BingoBookProfile(tier, alias, theme, maxHp, buildJutsuProfile(tier, theme));
    }

    public static BingoBookProfile generateDeterministic(long seed, @Nullable RankTier forcedTier) {
        RandomSource seeded = RandomSource.create(seed);
        RankTier tier = forcedTier != null ? forcedTier : (seeded.nextFloat() < 0.30F ? RankTier.S : RankTier.A);
        List<String> pool = tier == RankTier.S ? S_RANK_ALIASES : A_RANK_ALIASES;
        String alias = pool.get(Math.floorMod((int) seed, pool.size()));
        ElementTheme theme = ElementTheme.fromNameOrSeed(null, seed >>> 8);
        int span = Math.max(1, (int) Math.round(tier.getMaxHealth() - tier.getMinHealth()) + 1);
        double maxHp = tier.getMinHealth() + seeded.nextInt(span);
        return new BingoBookProfile(tier, alias, theme, maxHp, buildJutsuProfile(tier, theme));
    }

    public static NinjaJutsuProfile buildJutsuProfile(RankTier tier, ElementTheme theme) {
        boolean isS = tier == RankTier.S;
        NinjaJutsuProfile.Builder builder = new NinjaJutsuProfile.Builder()
                .rangedDistances(8.0D, 24.0D)
                .closeThreatDistance(7.0D)
                .criticalHealthRatio(0.35F)
                .globalCooldownTicks(isS ? 32 : 42)
                .handSealWindupTicks(isS ? 8 : 10)
                .kawarimi(true, isS ? 140 : 170, 5.5D, 75, isS ? 1 : 0);

        if (isS) {
            builder.mudWall(true, 160, 120, 45.0F);
        }

        switch (theme) {
            case FIRE -> builder
                    .fireball(true, isS ? 70 : 85, 25)
                    .waterBullet(false, 120, 30, 10.0F, 30, 1.15D);
            case WATER -> builder
                    .waterBullet(true, isS ? 65 : 80, 25, isS ? 12.0F : 10.5F, 34, 1.25D);
            case WIND -> builder
                    .waterBullet(false, 120, 30, 10.0F, 30, 1.15D)
                    .vacuumSpheres(true, isS ? 70 : 85, 25, isS ? 6.0F : 5.2F, 32, 1.50D);
            case EARTH -> builder
                    .waterBullet(false, 120, 30, 10.0F, 30, 1.15D)
                    .earthBullet(true, isS ? 75 : 90, 25, isS ? 12.5F : 11.0F, 40, 1.15D)
                    .mudWall(true, isS ? 145 : 175, 120, 45.0F);
            case LIGHTNING -> builder
                    .chidoriSpark(true, isS ? 75 : 90, isS ? 11.5F : 10.0F)
                    .waterBullet(true, isS ? 85 : 100, 25, 9.5F, 30, 1.20D);
        }

        return builder.build();
    }

    public Component formatWantedName() {
        ChatFormatting rankColor = this.rankTier == RankTier.S ? ChatFormatting.DARK_RED : ChatFormatting.RED;
        return Component.literal("[" + this.rankTier.getPrefix() + "] ")
                .withStyle(rankColor, ChatFormatting.BOLD)
                .append(Component.literal(this.alias).withStyle(ChatFormatting.GOLD));
    }

    public static int getBonusEmeraldCount(RankTier tier, RandomSource random) {
        return tier == RankTier.S ? (4 + random.nextInt(4)) : (2 + random.nextInt(3));
    }

    public static int getBlankScrollCount(RankTier tier, RandomSource random) {
        if (tier == RankTier.S) {
            return 1 + (random.nextFloat() < 0.35F ? 1 : 0);
        }
        return 1;
    }

    public static Optional<Item> rollRareJutsuScroll(RankTier tier, RandomSource random) {
        if (random.nextFloat() >= tier.getRareScrollChance()) {
            return Optional.empty();
        }
        Item[] pool = tier == RankTier.S
                ? new Item[]{
                        NarutoItems.SCROLL_RASENGAN.get(),
                        NarutoItems.SCROLL_CHIDORI.get(),
                        NarutoItems.SCROLL_WATER_DRAGON.get(),
                        NarutoItems.SCROLL_STONE_SPIKES.get(),
                        NarutoItems.SCROLL_CHIDORI_STREAM.get(),
                        NarutoItems.SCROLL_KIRIN.get()
                }
                : new Item[]{
                        NarutoItems.SCROLL_STONE_SPIKES.get(),
                        NarutoItems.SCROLL_WATER_DRAGON.get(),
                        NarutoItems.SCROLL_CHIDORI_STREAM.get(),
                        NarutoItems.SCROLL_RASENGAN.get(),
                        NarutoItems.SCROLL_CHIDORI.get()
                };
        return Optional.of(pool[random.nextInt(pool.length)]);
    }

    public static void dropBingoBonusLoot(ServerLevel serverLevel, RogueNinjaEntity entity, RandomSource random, RankTier tier) {
        int emeralds = getBonusEmeraldCount(tier, random);
        if (emeralds > 0) {
            entity.spawnAtLocation(serverLevel, new ItemStack(Items.EMERALD, emeralds));
        }

        int blankScrolls = getBlankScrollCount(tier, random);
        for (int i = 0; i < blankScrolls; i++) {
            entity.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.BLANK_JUTSU_SCROLL.get(), 1));
        }

        rollRareJutsuScroll(tier, random).ifPresent(scroll ->
                entity.spawnAtLocation(serverLevel, new ItemStack(scroll, 1)));
    }
}
