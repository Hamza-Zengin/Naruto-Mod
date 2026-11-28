package com.sekwah.narutomod.capabilities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sekwah.narutomod.NarutoMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Data-driven Shinobi Mission definition loaded from DataPacks ({@code data/narutomod/missions/*.json})
 * or converted from legacy {@link NinjaMission} defaults.
 */
public record MissionDefinition(
        String id,
        String rankLabel,
        NinjaRank requiredRank,
        NinjaMission.TargetType targetType,
        int targetCount,
        int rewardXp,
        List<RewardItem> rewardItems
) {

    public record RewardItem(Identifier itemId, int count) {
        public static final Codec<RewardItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("item").forGetter(RewardItem::itemId),
                Codec.INT.optionalFieldOf("count", 1).forGetter(RewardItem::count)
        ).apply(instance, RewardItem::new));

        public RewardItem {
            Objects.requireNonNull(itemId, "itemId");
            count = Math.max(1, count);
        }
    }

    public static final Codec<NinjaRank> RANK_CODEC = Codec.STRING.comapFlatMap(
            str -> {
                NinjaRank parsed = NinjaRank.parseExact(str);
                return parsed != null
                        ? DataResult.success(parsed)
                        : DataResult.error(() -> "Unknown NinjaRank: " + str);
            },
            NinjaRank::name
    );

    public static final Codec<NinjaMission.TargetType> TARGET_TYPE_CODEC = Codec.STRING.comapFlatMap(
            str -> {
                if (str != null) {
                    String trimmed = str.trim();
                    for (NinjaMission.TargetType t : NinjaMission.TargetType.values()) {
                        if (t.name().equalsIgnoreCase(trimmed)) {
                            return DataResult.success(t);
                        }
                    }
                }
                return DataResult.error(() -> "Unknown NinjaMission.TargetType: " + str);
            },
            Enum::name
    );

    public static final Codec<MissionDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("id", "").forGetter(MissionDefinition::id),
            Codec.STRING.fieldOf("rank_label").forGetter(MissionDefinition::rankLabel),
            RANK_CODEC.fieldOf("required_rank").forGetter(MissionDefinition::requiredRank),
            TARGET_TYPE_CODEC.fieldOf("target_type").forGetter(MissionDefinition::targetType),
            Codec.INT.fieldOf("target_count").forGetter(MissionDefinition::targetCount),
            Codec.INT.fieldOf("reward_xp").forGetter(MissionDefinition::rewardXp),
            RewardItem.CODEC.listOf().optionalFieldOf("reward_items", List.of()).forGetter(MissionDefinition::rewardItems)
    ).apply(instance, MissionDefinition::new));

    public MissionDefinition {
        id = id != null ? id.trim() : "";
        rankLabel = (rankLabel != null && !rankLabel.isBlank()) ? rankLabel.trim() : "D-Rank";
        requiredRank = requiredRank != null ? requiredRank : NinjaRank.ACADEMY_STUDENT;
        targetType = targetType != null ? targetType : NinjaMission.TargetType.KILL_ENEMY;
        targetCount = Math.max(1, targetCount);
        rewardXp = Math.max(0, rewardXp);
        rewardItems = rewardItems == null ? List.of() : List.copyOf(rewardItems);
    }

    public MissionDefinition withId(String resolvedId) {
        return new MissionDefinition(resolvedId, this.rankLabel, this.requiredRank,
                this.targetType, this.targetCount, this.rewardXp, this.rewardItems);
    }

    public String getTitleKey() {
        return "mission.narutomod." + this.id + ".title";
    }

    public String getDescriptionKey() {
        return "mission.narutomod." + this.id + ".desc";
    }

    public String getObjectiveKey() {
        return "mission.narutomod.objective." + this.targetType.name().toLowerCase(Locale.ROOT);
    }

    public Component getTitleComponent() {
        return Component.translatable(getTitleKey());
    }

    public Component getDescriptionComponent() {
        return Component.translatable(getDescriptionKey());
    }

    public Component getObjectiveComponent(int progress) {
        return Component.translatable(getObjectiveKey(), progress, this.targetCount);
    }

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
                && (NinjaMission.MissionStatus.fromString(data.getMissionStatus()) == NinjaMission.MissionStatus.COMPLETED
                || data.getMissionProgress() >= this.targetCount);
    }

    public static MissionDefinition fromLegacy(NinjaMission mission) {
        Objects.requireNonNull(mission, "mission");
        Identifier emerald = Identifier.withDefaultNamespace("emerald");
        Identifier blankScroll = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "blank_jutsu_scroll");
        List<RewardItem> defaults = switch (mission) {
            case D_RANK_PATROL -> List.of(new RewardItem(emerald, 2));
            case C_RANK_ROGUE_HUNT -> List.of(new RewardItem(emerald, 4));
            case B_RANK_JUTSU_DRILL -> List.of(new RewardItem(blankScroll, 1));
            case A_RANK_AKATSUKI_RECON -> List.of(new RewardItem(blankScroll, 1), new RewardItem(emerald, 8));
            case S_RANK_AKATSUKI_BOUNTY -> List.of(new RewardItem(blankScroll, 2), new RewardItem(emerald, 16));
        };
        return new MissionDefinition(
                mission.getId(),
                mission.getRankLabel(),
                mission.getRequiredRank(),
                mission.getTargetType(),
                mission.getTargetCount(),
                mission.getRewardXp(),
                defaults
        );
    }
}
