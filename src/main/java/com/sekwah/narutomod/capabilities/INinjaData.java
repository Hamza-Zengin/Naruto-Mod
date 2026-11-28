package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface INinjaData extends ValueIOSerializable {
    float getChakra();
    float getMaxChakra();
    float getBaseMaxChakra();
    float getStamina();
    float getMaxStamina();
    float getBaseMaxStamina();
    void setChakra(float amount);
    void setStamina(float amount);
    void setMaxChakra(float amount);
    void setMaxStamina(float amount);
    void setBaseMaxChakra(float amount);
    void setBaseMaxStamina(float amount);
    void useChakra(float amount, int cooldown);
    void useStamina(float amount, int cooldown);
    void addChakra(float amount);
    void addStamina(float amount);
    void lockChakraRegen(int ticks);
    int getChakraRegenCooldown();
    boolean isChakraRegenLocked();

    Identifier getClan();
    void setClan(Identifier clan);
    void forceSetClan(Identifier clan);

    boolean isClanLocked();
    void setClanLocked(boolean locked);

    String getRank();
    void setRank(String rank);

    float getChakraExp();
    void addChakraExp(float amount);
    void setChakraExp(float amount);

    float getStaminaExp();
    void addStaminaExp(float amount);
    void setStaminaExp(float amount);

    int getEnemiesDefeated();
    void addEnemyDefeated();
    void setEnemiesDefeated(int count);

    int getRogueNinjasDefeated();
    void addRogueNinjaDefeated();
    void setRogueNinjasDefeated(int count);

    int getAkatsukiNinjasDefeated();
    void addAkatsukiNinjaDefeated();
    void setAkatsukiNinjasDefeated(int count);

    int getJutsusCastCount();
    void addJutsuCast();
    void setJutsusCastCount(int count);

    boolean isCreativeMode();
    void setCreativeMode(boolean creative);

    boolean canRankUp();
    boolean rankUp();
    void resetData();

    boolean isAbilityUnlocked(Ability ability);
    boolean isAbilityUnlocked(Identifier abilityId);
    void unlockAbility(Identifier abilityId);
    void lockAbility(Identifier abilityId);
    java.util.Set<Identifier> getUnlockedAbilities();
    String getUnlockedJutsusString();
    void setUnlockedJutsusString(String jutsus);

    Map<Identifier, Integer> getJutsuMasteryMap();
    int getMastery(Identifier jutsuId);
    void setMastery(Identifier jutsuId, int amount);
    void addMastery(Identifier jutsuId, int amount);
    void incrementJutsuMastery(Identifier jutsuId, boolean isToggle);
    default int getAbilityMastery(com.sekwah.narutomod.abilities.Ability ability) {
        if (ability == null || ability.getRegistryName() == null) return 0;
        return getMastery(ability.getRegistryName());
    }

    void saveNBTData(CompoundTag nbt);
    void loadNBTData(CompoundTag nbt);

    HashMap<String, CooldownTickEvent> getCooldownEvents();

    Identifier getCurrentlyChanneledAbility();
    int getCurrentlyChanneledTicks();
    void setCurrentlyChanneledAbility(Player player, Ability ability);

    ToggleAbilityData getToggleAbilityData();

    void updateServerData(Player player);

    void scheduleDelayedTickEvent(Consumer<Player> consumer, int tickDelay);

    String getActiveHandJutsu();
    int getActiveHandJutsuTicks();
    void setActiveHandJutsu(String jutsu, int ticks);
    void clearActiveHandJutsu();

    boolean isShimonPenaltyActive();
    void setShimonPenaltyActive(boolean active);

    // Bitmask constants for synchronized active abilities
    int JUTSU_FLAG_SHARINGAN           = 1 << 0;
    int JUTSU_FLAG_BYAKUGAN            = 1 << 1;
    int JUTSU_FLAG_EIGHT_GATES         = 1 << 2;
    int JUTSU_FLAG_WATER_WALK          = 1 << 3;
    int JUTSU_FLAG_EIGHT_GATES_DEATH   = 1 << 4;
    int JUTSU_FLAG_SAGE_MODE           = 1 << 5;

    int getActiveJutsusFlag();
    void setActiveJutsusFlag(int flag);
    boolean isJutsuActive(int flag);
    void setJutsuActive(int flag, boolean active);

    boolean isSharinganActive();
    void setSharinganActive(boolean active);
    boolean isByakuganActive();
    void setByakuganActive(boolean active);
    boolean isEightGatesActive();
    void setEightGatesActive(boolean active);
    boolean isWaterWalkActive();
    void setWaterWalkActive(boolean active);
    boolean isEighthGateActive();
    void setEighthGateActive(boolean active);
    boolean isSageModeActive();
    void setSageModeActive(boolean active);

    int MAX_JUTSU_SLOTS = 8;

    @javax.annotation.Nullable
    Identifier getEquippedJutsu(int slot);
    void setEquippedJutsu(int slot, @javax.annotation.Nullable Identifier abilityId);
    List<Identifier> getEquippedJutsusList();
    String getEquippedJutsusString();
    void setEquippedJutsusString(String serialized);
    int getSelectedJutsuSlot();
    void setSelectedJutsuSlot(int slot);
    @javax.annotation.Nullable
    Identifier getSelectedJutsuId();

    String getActiveMissionId();
    void setActiveMissionId(String missionId);
    int getMissionProgress();
    void setMissionProgress(int progress);
    int getCompletedMissionsCount();
    void setCompletedMissionsCount(int count);
    String getMissionStatus();
    void setMissionStatus(String status);
    boolean acceptMission(NinjaMission mission);
    boolean incrementMissionProgress(int amount);
    void abandonMission();
    boolean claimMissionReward();

    default boolean isKageHatEquipped() { return false; }
    default void setKageHatEquipped(boolean equipped) {}
}

