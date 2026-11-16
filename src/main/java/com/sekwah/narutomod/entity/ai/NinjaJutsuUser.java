package com.sekwah.narutomod.entity.ai;

/**
 * Capability-like contract implemented by Ninja mobs that use {@link NinjaJutsuAttackGoal}
 * so the goal can manage shared and per-ability cooldowns cleanly on the entity.
 */
public interface NinjaJutsuUser {

    NinjaJutsuCooldowns getJutsuCooldowns();

    default int getGlobalJutsuCooldown() {
        return this.getJutsuCooldowns().getGlobalJutsuCooldown();
    }

    default void setGlobalJutsuCooldown(int ticks) {
        this.getJutsuCooldowns().setGlobalJutsuCooldown(ticks);
    }

    default int getJutsuCooldown(NinjaJutsuType type) {
        return this.getJutsuCooldowns().getAbilityCooldown(type);
    }

    default void setJutsuCooldown(NinjaJutsuType type, int ticks) {
        this.getJutsuCooldowns().setAbilityCooldown(type, ticks);
    }
}
