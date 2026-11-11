package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface INinjaData extends INBTSerializable<Tag> {
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

    ResourceLocation getClan();
    void setClan(ResourceLocation clan);
    void forceSetClan(ResourceLocation clan);

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
    boolean isAbilityUnlocked(ResourceLocation abilityId);
    void unlockAbility(ResourceLocation abilityId);
    void lockAbility(ResourceLocation abilityId);
    java.util.Set<ResourceLocation> getUnlockedAbilities();
    String getUnlockedJutsusString();
    void setUnlockedJutsusString(String jutsus);

    Map<ResourceLocation, Integer> getJutsuMasteryMap();
    int getMastery(ResourceLocation jutsuId);
    void addMastery(ResourceLocation jutsuId, int amount);

    void saveNBTData(CompoundTag nbt);
    void loadNBTData(CompoundTag nbt);

    HashMap<String, CooldownTickEvent> getCooldownEvents();

    ResourceLocation getCurrentlyChanneledAbility();
    int getCurrentlyChanneledTicks();
    void setCurrentlyChanneledAbility(Player player, Ability ability);

    ToggleAbilityData getToggleAbilityData();

    void updateServerData(Player player);

    void scheduleDelayedTickEvent(Consumer<Player> consumer, int tickDelay);

    String getActiveHandJutsu();
    int getActiveHandJutsuTicks();
    void setActiveHandJutsu(String jutsu, int ticks);
    void clearActiveHandJutsu();

    // Bitmask constants for synchronized active abilities
    int JUTSU_FLAG_SHARINGAN   = 1 << 0;
    int JUTSU_FLAG_BYAKUGAN    = 1 << 1;
    int JUTSU_FLAG_EIGHT_GATES = 1 << 2;
    int JUTSU_FLAG_WATER_WALK  = 1 << 3;

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

    List<Byte> getActiveSeals();
    void addSeal(byte seal);
    void clearSeals();
    int getSealTimeout();
    void tick();
}

