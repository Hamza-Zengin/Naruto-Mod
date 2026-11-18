package com.sekwah.narutomod.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Immutable Data Component payload storing sealed shinobi throwable ammunition counts
 * and the currently active ammo slot inside {@link SealedAmmoScrollItem}.
 */
public record SealedAmmoContents(
        int kunai,
        int shuriken,
        int senbon,
        int explosiveKunai,
        int smokeBomb,
        int selectedIndex
) {

    public static final SealedAmmoContents EMPTY = new SealedAmmoContents(0, 0, 0, 0, 0, 0);
    public static final int MAX_PER_TYPE = 64;

    public static final Codec<SealedAmmoContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("kunai", 0).forGetter(SealedAmmoContents::kunai),
            Codec.INT.optionalFieldOf("shuriken", 0).forGetter(SealedAmmoContents::shuriken),
            Codec.INT.optionalFieldOf("senbon", 0).forGetter(SealedAmmoContents::senbon),
            Codec.INT.optionalFieldOf("explosive_kunai", 0).forGetter(SealedAmmoContents::explosiveKunai),
            Codec.INT.optionalFieldOf("smoke_bomb", 0).forGetter(SealedAmmoContents::smokeBomb),
            Codec.INT.optionalFieldOf("selected_index", 0).forGetter(SealedAmmoContents::selectedIndex)
    ).apply(instance, SealedAmmoContents::new));

    public static final StreamCodec<ByteBuf, SealedAmmoContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SealedAmmoContents::kunai,
            ByteBufCodecs.VAR_INT, SealedAmmoContents::shuriken,
            ByteBufCodecs.VAR_INT, SealedAmmoContents::senbon,
            ByteBufCodecs.VAR_INT, SealedAmmoContents::explosiveKunai,
            ByteBufCodecs.VAR_INT, SealedAmmoContents::smokeBomb,
            ByteBufCodecs.VAR_INT, SealedAmmoContents::selectedIndex,
            SealedAmmoContents::new
    );

    public enum AmmoType {
        KUNAI(0, "item.narutomod.kunai"),
        SHURIKEN(1, "item.narutomod.shuriken"),
        SENBON(2, "item.narutomod.senbon"),
        EXPLOSIVE_KUNAI(3, "item.narutomod.explosive_kunai"),
        SMOKE_BOMB(4, "item.narutomod.smoke_bomb");

        private final int index;
        private final String translationKey;

        AmmoType(int index, String translationKey) {
            this.index = index;
            this.translationKey = translationKey;
        }

        public int getIndex() {
            return this.index;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }

        public static AmmoType fromIndex(int idx) {
            AmmoType[] vals = values();
            return vals[Math.floorMod(idx, vals.length)];
        }
    }

    public AmmoType getSelectedType() {
        return AmmoType.fromIndex(this.selectedIndex);
    }

    public int getCount(AmmoType type) {
        return switch (type) {
            case KUNAI -> Math.max(0, this.kunai);
            case SHURIKEN -> Math.max(0, this.shuriken);
            case SENBON -> Math.max(0, this.senbon);
            case EXPLOSIVE_KUNAI -> Math.max(0, this.explosiveKunai);
            case SMOKE_BOMB -> Math.max(0, this.smokeBomb);
        };
    }

    public int getTotalCount() {
        return Math.max(0, this.kunai)
                + Math.max(0, this.shuriken)
                + Math.max(0, this.senbon)
                + Math.max(0, this.explosiveKunai)
                + Math.max(0, this.smokeBomb);
    }

    public SealedAmmoContents withCount(AmmoType type, int newCount) {
        int clamped = Math.max(0, Math.min(MAX_PER_TYPE, newCount));
        return switch (type) {
            case KUNAI -> new SealedAmmoContents(clamped, shuriken, senbon, explosiveKunai, smokeBomb, selectedIndex);
            case SHURIKEN -> new SealedAmmoContents(kunai, clamped, senbon, explosiveKunai, smokeBomb, selectedIndex);
            case SENBON -> new SealedAmmoContents(kunai, shuriken, clamped, explosiveKunai, smokeBomb, selectedIndex);
            case EXPLOSIVE_KUNAI -> new SealedAmmoContents(kunai, shuriken, senbon, clamped, smokeBomb, selectedIndex);
            case SMOKE_BOMB -> new SealedAmmoContents(kunai, shuriken, senbon, explosiveKunai, clamped, selectedIndex);
        };
    }

    public SealedAmmoContents withSelected(AmmoType type) {
        return new SealedAmmoContents(kunai, shuriken, senbon, explosiveKunai, smokeBomb, type.getIndex());
    }

    public SealedAmmoContents cycleSelected() {
        AmmoType[] vals = AmmoType.values();
        int total = getTotalCount();
        for (int offset = 1; offset <= vals.length; offset++) {
            AmmoType candidate = AmmoType.fromIndex(this.selectedIndex + offset);
            if (total == 0 || getCount(candidate) > 0) {
                return withSelected(candidate);
            }
        }
        return withSelected(AmmoType.fromIndex(this.selectedIndex + 1));
    }
}
