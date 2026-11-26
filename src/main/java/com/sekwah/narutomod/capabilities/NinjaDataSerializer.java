package com.sekwah.narutomod.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Map;

/**
 * Handles NBT and ValueIO serialization and deserialization for {@link NinjaData},
 * preserving the exact persisted tag keys, types, and rank-cap clamping semantics.
 */
public final class NinjaDataSerializer {

    private static final String SHIMON_PENALTY_ACTIVE_TAG = "shimonPenaltyActive";
    private static final String CHAKRA_TAG = "chakra";
    private static final String STAMINA_TAG = "stamina";
    private static final String MAX_CHAKRA_TAG = "maxChakra";
    private static final String MAX_STAMINA_TAG = "maxStamina";
    private static final String CLAN_TAG = "clan";
    private static final String CLAN_LOCKED_TAG = "clanLocked";
    private static final String RANK_TAG = "rank";
    private static final String CHAKRA_EXP_TAG = "chakraExp";
    private static final String STAMINA_EXP_TAG = "staminaExp";
    private static final String ENEMIES_DEFEATED_TAG = "enemiesDefeated";
    private static final String ROGUE_NINJAS_DEFEATED_TAG = "rogueNinjasDefeated";
    private static final String AKATSUKI_NINJAS_DEFEATED_TAG = "akatsukiNinjasDefeated";
    private static final String JUTSUS_CAST_TAG = "jutsusCast";
    private static final String UNLOCKED_JUTSUS_TAG = "unlockedJutsus";
    private static final String EQUIPPED_JUTSUS_TAG = "equippedJutsus";
    private static final String SELECTED_JUTSU_SLOT_TAG = "selectedJutsuSlot";
    private static final String ACTIVE_HAND_JUTSU_TAG = "activeHandJutsu";
    private static final String ACTIVE_HAND_JUTSU_TICKS_TAG = "activeHandJutsuTicks";
    private static final String ACTIVE_JUTSUS_FLAG_TAG = "activeJutsusFlag";
    private static final String JUTSU_MASTERY_TAG = "jutsuMastery";
    private static final String CHAKRA_REGEN_COOLDOWN_TAG = "chakraRegenCooldown";
    private static final String ACTIVE_MISSION_ID_TAG = "activeMissionId";
    private static final String MISSION_PROGRESS_TAG = "missionProgress";
    private static final String COMPLETED_MISSIONS_COUNT_TAG = "completedMissionsCount";
    private static final String MISSION_STATUS_TAG = "missionStatus";
    private static final String NINJA_DATA_WRAPPER_TAG = "ninjaData";

    private NinjaDataSerializer() {}

    public static void serializeNBT(NinjaData data, CompoundTag nbt) {
        if (data == null || nbt == null) return;
        nbt.putBoolean(SHIMON_PENALTY_ACTIVE_TAG, data.isShimonPenaltyActive());
        nbt.putFloat(CHAKRA_TAG, data.getChakra());
        nbt.putFloat(STAMINA_TAG, data.getStamina());
        nbt.putFloat(MAX_CHAKRA_TAG, data.getRawMaxChakra());
        nbt.putFloat(MAX_STAMINA_TAG, data.getRawMaxStamina());
        if (data.getClan() != null) {
            nbt.putString(CLAN_TAG, data.getClan().toString());
        }
        nbt.putBoolean(CLAN_LOCKED_TAG, data.isClanLocked());
        nbt.putString(RANK_TAG, data.getRank() != null ? data.getRank() : "ACADEMY_STUDENT");
        nbt.putFloat(CHAKRA_EXP_TAG, data.getChakraExp());
        nbt.putFloat(STAMINA_EXP_TAG, data.getStaminaExp());
        nbt.putInt(ENEMIES_DEFEATED_TAG, data.getEnemiesDefeated());
        nbt.putInt(ROGUE_NINJAS_DEFEATED_TAG, data.getRogueNinjasDefeated());
        nbt.putInt(AKATSUKI_NINJAS_DEFEATED_TAG, data.getAkatsukiNinjasDefeated());
        nbt.putInt(JUTSUS_CAST_TAG, data.getJutsusCastCount());
        nbt.putString(UNLOCKED_JUTSUS_TAG, data.getUnlockedJutsusString());
        nbt.putString(EQUIPPED_JUTSUS_TAG, data.getEquippedJutsusString());
        nbt.putInt(SELECTED_JUTSU_SLOT_TAG, data.getSelectedJutsuSlot());
        nbt.putString(ACTIVE_HAND_JUTSU_TAG, data.getActiveHandJutsu());
        nbt.putInt(ACTIVE_HAND_JUTSU_TICKS_TAG, data.getActiveHandJutsuTicks());
        nbt.putInt(ACTIVE_JUTSUS_FLAG_TAG, data.getActiveJutsusFlag());

        nbt.putInt(CHAKRA_REGEN_COOLDOWN_TAG, data.getChakraRegenCooldown());
        nbt.putString(ACTIVE_MISSION_ID_TAG, data.getActiveMissionId());
        nbt.putInt(MISSION_PROGRESS_TAG, data.getMissionProgress());
        nbt.putInt(COMPLETED_MISSIONS_COUNT_TAG, data.getCompletedMissionsCount());
        nbt.putString(MISSION_STATUS_TAG, data.getMissionStatus());

        CompoundTag masteryTag = new CompoundTag();
        for (Map.Entry<Identifier, Integer> entry : data.getJutsuMasteryMap().entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                masteryTag.putInt(entry.getKey().toString(), entry.getValue());
            }
        }
        nbt.put(JUTSU_MASTERY_TAG, masteryTag);
    }

    public static void deserializeNBT(NinjaData data, CompoundTag compoundTag) {
        if (data == null || compoundTag == null) return;
        data.setShimonPenaltyActive(compoundTag.getBooleanOr(SHIMON_PENALTY_ACTIVE_TAG, false));
        data.setRawChakra(compoundTag.getFloatOr(CHAKRA_TAG, 0.0f));
        data.setRawStamina(compoundTag.getFloatOr(STAMINA_TAG, 0.0f));
        if (compoundTag.contains(MAX_CHAKRA_TAG)) {
            data.setRawMaxChakra(compoundTag.getFloatOr(MAX_CHAKRA_TAG, data.getRawMaxChakra()));
        }
        if (compoundTag.contains(MAX_STAMINA_TAG)) {
            data.setRawMaxStamina(compoundTag.getFloatOr(MAX_STAMINA_TAG, data.getRawMaxStamina()));
        }
        compoundTag.getString(CLAN_TAG).ifPresent(clanStr -> {
            Identifier parsed = Identifier.tryParse(clanStr);
            if (parsed != null) {
                data.setRawClan(parsed);
            }
        });
        if (compoundTag.contains(CLAN_LOCKED_TAG)) {
            data.setClanLocked(compoundTag.getBooleanOr(CLAN_LOCKED_TAG, false));
        }
        if (compoundTag.contains(RANK_TAG)) {
            data.setRawRank(compoundTag.getStringOr(RANK_TAG, "ACADEMY_STUDENT"));
        } else {
            data.setRawRank("ACADEMY_STUDENT");
        }
        if (compoundTag.contains(CHAKRA_EXP_TAG)) {
            data.setChakraExp(compoundTag.getFloatOr(CHAKRA_EXP_TAG, 0.0f));
        }
        if (compoundTag.contains(STAMINA_EXP_TAG)) {
            data.setStaminaExp(compoundTag.getFloatOr(STAMINA_EXP_TAG, 0.0f));
        }
        if (compoundTag.contains(ENEMIES_DEFEATED_TAG)) {
            data.setEnemiesDefeated(compoundTag.getIntOr(ENEMIES_DEFEATED_TAG, 0));
        }
        if (compoundTag.contains(ROGUE_NINJAS_DEFEATED_TAG)) {
            data.setRogueNinjasDefeated(compoundTag.getIntOr(ROGUE_NINJAS_DEFEATED_TAG, 0));
        }
        if (compoundTag.contains(AKATSUKI_NINJAS_DEFEATED_TAG)) {
            data.setAkatsukiNinjasDefeated(compoundTag.getIntOr(AKATSUKI_NINJAS_DEFEATED_TAG, 0));
        }
        if (compoundTag.contains(JUTSUS_CAST_TAG)) {
            data.setJutsusCastCount(compoundTag.getIntOr(JUTSUS_CAST_TAG, 0));
        }
        NinjaRank current = NinjaRank.fromString(data.getRank());
        if (current == NinjaRank.ACADEMY_STUDENT) {
            if (data.getJutsusCastCount() > 10) data.setJutsusCastCount(10);
        } else if (current == NinjaRank.GENIN) {
            if (data.getEnemiesDefeated() > 25) data.setEnemiesDefeated(25);
        } else if (current == NinjaRank.CHUNIN) {
            if (data.getEnemiesDefeated() > 75) data.setEnemiesDefeated(75);
        } else if (current == NinjaRank.JONIN) {
            if (data.getEnemiesDefeated() > 150) data.setEnemiesDefeated(150);
        } else if (current == NinjaRank.ANBU) {
            if (data.getEnemiesDefeated() > 300) data.setEnemiesDefeated(300);
        }
        if (compoundTag.contains(UNLOCKED_JUTSUS_TAG)) {
            data.setUnlockedJutsusString(compoundTag.getStringOr(UNLOCKED_JUTSUS_TAG, ""));
        }
        if (compoundTag.contains(EQUIPPED_JUTSUS_TAG)) {
            data.setEquippedJutsusString(compoundTag.getStringOr(EQUIPPED_JUTSUS_TAG, ";;;;;;;"));
        }
        if (compoundTag.contains(SELECTED_JUTSU_SLOT_TAG)) {
            data.setSelectedJutsuSlot(compoundTag.getIntOr(SELECTED_JUTSU_SLOT_TAG, 0));
        }
        if (compoundTag.contains(ACTIVE_HAND_JUTSU_TAG) || compoundTag.contains(ACTIVE_HAND_JUTSU_TICKS_TAG)) {
            String handJutsu = compoundTag.contains(ACTIVE_HAND_JUTSU_TAG)
                    ? compoundTag.getStringOr(ACTIVE_HAND_JUTSU_TAG, "")
                    : data.getActiveHandJutsu();
            int handTicks = compoundTag.contains(ACTIVE_HAND_JUTSU_TICKS_TAG)
                    ? compoundTag.getIntOr(ACTIVE_HAND_JUTSU_TICKS_TAG, 0)
                    : data.getActiveHandJutsuTicks();
            data.setActiveHandJutsu(handJutsu, handTicks);
        }
        if (compoundTag.contains(ACTIVE_JUTSUS_FLAG_TAG)) {
            data.setActiveJutsusFlag(compoundTag.getIntOr(ACTIVE_JUTSUS_FLAG_TAG, 0));
        }
        if (compoundTag.contains(ACTIVE_MISSION_ID_TAG)) {
            data.setActiveMissionId(compoundTag.getStringOr(ACTIVE_MISSION_ID_TAG, ""));
        }
        if (compoundTag.contains(MISSION_PROGRESS_TAG)) {
            data.setRawMissionProgress(Math.max(0, compoundTag.getIntOr(MISSION_PROGRESS_TAG, 0)));
        }
        if (compoundTag.contains(COMPLETED_MISSIONS_COUNT_TAG)) {
            data.setCompletedMissionsCount(Math.max(0, compoundTag.getIntOr(COMPLETED_MISSIONS_COUNT_TAG, 0)));
        }
        if (compoundTag.contains(MISSION_STATUS_TAG)) {
            data.setMissionStatus(compoundTag.getStringOr(MISSION_STATUS_TAG, "NONE"));
        }
        if (compoundTag.contains(CHAKRA_REGEN_COOLDOWN_TAG)) {
            data.setRawChakraRegenCooldown(compoundTag.getIntOr(CHAKRA_REGEN_COOLDOWN_TAG, 0));
        }
        compoundTag.getCompound(JUTSU_MASTERY_TAG).ifPresent(masteryTag -> {
            Map<Identifier, Integer> map = data.getJutsuMasteryMap();
            map.clear();
            for (String key : masteryTag.keySet()) {
                Identifier id = Identifier.tryParse(key);
                if (id != null) {
                    map.put(id, masteryTag.getIntOr(key, 0));
                }
            }
        });
    }

    public static void serialize(NinjaData data, ValueOutput output) {
        if (data == null || output == null) return;
        final CompoundTag nbt = new CompoundTag();
        serializeNBT(data, nbt);
        output.store(NINJA_DATA_WRAPPER_TAG, CompoundTag.CODEC, nbt);
    }

    public static void deserialize(NinjaData data, ValueInput input) {
        if (data == null || input == null) return;
        input.read(NINJA_DATA_WRAPPER_TAG, CompoundTag.CODEC).ifPresent(tag -> deserializeNBT(data, tag));
    }
}
