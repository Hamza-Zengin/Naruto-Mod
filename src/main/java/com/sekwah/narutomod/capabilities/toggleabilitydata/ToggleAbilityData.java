package com.sekwah.narutomod.capabilities.toggleabilitydata;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tracks ability references and other information for the server side.
 */
public class ToggleAbilityData {

    private HashSet<Identifier> abilities;
    private transient Map<Identifier, Integer> activeTicks;

    public ToggleAbilityData(int size) {
        this.abilities = new HashSet<>(size);
    }

    public ToggleAbilityData() {
        this.abilities = new HashSet<>();
    }

    private Map<Identifier, Integer> activeTicksMap() {
        if (this.activeTicks == null) {
            this.activeTicks = new HashMap<>();
        }
        return this.activeTicks;
    }

    public boolean addAbility(Identifier ability) {
        if (ability != null && this.abilities.add(ability)) {
            this.activeTicksMap().putIfAbsent(ability, 0);
            return true;
        }
        return false;
    }

    public boolean addAbilityStarted(Player player, INinjaData ninjaData, Ability ability) {
        if (ability == null) {
            return false;
        }
        if (player != null && ability.activationType() == Ability.ActivationType.TOGGLE && ability.logInChat()) {
            player.sendSystemMessage(Component.translatable("jutsu.toggle.enabled", Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
        }
        Identifier id = ability.getRegistryName();
        boolean added = this.addAbility(id);
        if (id != null) {
            this.activeTicksMap().put(id, 0);
        }
        return added;
    }

    public int incrementAbilityTicks(Identifier ability) {
        if (ability == null) {
            return 0;
        }
        Map<Identifier, Integer> ticksMap = this.activeTicksMap();
        int next = ticksMap.getOrDefault(ability, 0) + 1;
        ticksMap.put(ability, next);
        return next;
    }

    public int getActiveTicks(Identifier ability) {
        if (ability == null || this.activeTicks == null) {
            return 0;
        }
        return this.activeTicks.getOrDefault(ability, 0);
    }

    private int getAndRemoveActiveTicks(Identifier ability) {
        if (ability == null || this.activeTicks == null) {
            return 0;
        }
        Integer removed = this.activeTicks.remove(ability);
        return removed != null ? removed : 0;
    }

    public boolean removeAbilityEnded(Player player, INinjaData ninjaData, Ability ability) {
        if (ability == null) {
            return false;
        }
        Identifier id = ability.getRegistryName();
        int finalTicks = this.getAndRemoveActiveTicks(id);
        if (ability instanceof Ability.HandleEnded endedAbility) {
            endedAbility.handleAbilityEnded(player, ninjaData, finalTicks);
        }
        if (player != null && ability.activationType() == Ability.ActivationType.TOGGLE && ability.logInChat()) {
            player.sendSystemMessage(Component.translatable("jutsu.toggle.disabled", Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
        }
        return this.removeAbility(id);
    }

    public boolean removeAbility(Identifier ability) {
        if (this.activeTicks != null && ability != null) {
            this.activeTicks.remove(ability);
        }
        return this.abilities.remove(ability);
    }

    public void clearActiveTicks() {
        if (this.activeTicks != null) {
            this.activeTicks.clear();
        }
    }

    public void clear() {
        if (this.abilities != null) {
            this.abilities.clear();
        }
        this.clearActiveTicks();
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
