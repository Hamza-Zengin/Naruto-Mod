package com.sekwah.narutomod.capabilities.toggleabilitydata;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Tracks ability references and other information for the server side.
 */
public class ToggleAbilityData {

    private HashSet<Identifier> abilities;

    public ToggleAbilityData(int size) {
        this.abilities = new HashSet<>(size);
    }

    public ToggleAbilityData() {
        this.abilities = new HashSet<>();
    }

    public boolean addAbility(Identifier ability) {
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

    public boolean removeAbility(Identifier ability) {
        return this.abilities.remove(ability);
    }

    public HashSet<Identifier> getAbilitiesHashSet() {
        return abilities;
    }

    public List<Identifier> getAbilitiesSnapshot() {
        if (this.abilities == null || this.abilities.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(this.abilities);
    }

    public boolean hasToggleAbility(Identifier ability) {
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
