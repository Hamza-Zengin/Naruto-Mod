package com.sekwah.narutomod.capabilities.toggleabilitydata;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Objects;

/**
 * Tracks ability references and other information for the server side.
 */
public class ToggleAbilityData {

    private HashSet<ResourceLocation> abilities;

    public ToggleAbilityData(int size) {
        this.abilities = new HashSet<>(size);
    }

    public ToggleAbilityData() {
        this.abilities = new HashSet<>();
    }

    public boolean addAbility(ResourceLocation ability) {
        return this.abilities.add(ability);
    }

    public boolean addAbilityStarted(Player player, INinjaData ninjaData, Ability ability) {
        if (ability.activationType() == Ability.ActivationType.TOGGLE && ability.logInChat()) {
            player.sendSystemMessage(Component.translatable("jutsu.toggle.enabled", Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
        }
        return this.addAbility(ability.getRegistryName());
    }

    public boolean removeAbilityEnded(Player player, INinjaData ninjaData, Ability ability) {
        if(ability instanceof Ability.HandleEnded endedAbility) endedAbility.handleAbilityEnded(player, ninjaData, 0);
        if (ability.activationType() == Ability.ActivationType.TOGGLE && ability.logInChat()) {
            player.sendSystemMessage(Component.translatable("jutsu.toggle.disabled", Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
        }
        return this.removeAbility(ability.getRegistryName());
    }

    public boolean removeAbility(ResourceLocation ability) {
        return this.abilities.remove(ability);
    }

    public HashSet<ResourceLocation> getAbilitiesHashSet() {
        return abilities;
    }

    public java.util.List<ResourceLocation> getAbilitiesSnapshot() {
        return this.abilities != null ? new java.util.ArrayList<>(this.abilities) : new java.util.ArrayList<>();
    }

    public boolean hasToggleAbility(ResourceLocation ability) {
        return this.abilities != null && this.abilities.contains(ability);
    }

    @Override
    public int hashCode() {
        return Objects.hash(abilities);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ToggleAbilityData)) return false;
        ToggleAbilityData that = (ToggleAbilityData) o;
        return abilities.equals(that.abilities);
    }
}
