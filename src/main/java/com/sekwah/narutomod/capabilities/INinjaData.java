package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
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
}

