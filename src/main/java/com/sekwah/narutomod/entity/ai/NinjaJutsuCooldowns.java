package com.sekwah.narutomod.entity.ai;

import java.util.EnumMap;

/**
 * Tracks both a shared global jutsu cooldown and per-ability cooldowns on an entity
 * to prevent infinite same-tick jutsu spamming.
 */
public class NinjaJutsuCooldowns {

    private int globalJutsuCooldown;
    private final EnumMap<NinjaJutsuType, Integer> abilityCooldowns = new EnumMap<>(NinjaJutsuType.class);

    public NinjaJutsuCooldowns() {
        this(0);
    }

    public NinjaJutsuCooldowns(int initialGlobalCooldown) {
        this.globalJutsuCooldown = Math.max(0, initialGlobalCooldown);
        for (NinjaJutsuType type : NinjaJutsuType.values()) {
            this.abilityCooldowns.put(type, 0);
        }
    }

    /**
     * Decrements global and all per-ability cooldown counters once per server tick.
     */
    public void tick() {
        if (this.globalJutsuCooldown > 0) {
            this.globalJutsuCooldown--;
        }
        for (NinjaJutsuType type : NinjaJutsuType.values()) {
            int current = this.abilityCooldowns.getOrDefault(type, 0);
            if (current > 0) {
                this.abilityCooldowns.put(type, current - 1);
            }
        }
    }

    public int getGlobalJutsuCooldown() {
        return this.globalJutsuCooldown;
    }

    public void setGlobalJutsuCooldown(int ticks) {
        this.globalJutsuCooldown = Math.max(0, ticks);
    }

    public int getAbilityCooldown(NinjaJutsuType type) {
        return this.abilityCooldowns.getOrDefault(type, 0);
    }

    public void setAbilityCooldown(NinjaJutsuType type, int ticks) {
        this.abilityCooldowns.put(type, Math.max(0, ticks));
    }

    public boolean isReady(NinjaJutsuType type) {
        return this.globalJutsuCooldown <= 0 && this.getAbilityCooldown(type) <= 0;
    }

    /**
     * Locks both the shared global cooldown and the specific ability cooldown atomically.
     */
    public void trigger(NinjaJutsuType type, int abilityCooldownTicks, int globalCooldownTicks) {
        this.setAbilityCooldown(type, abilityCooldownTicks);
        this.setGlobalJutsuCooldown(Math.max(this.globalJutsuCooldown, globalCooldownTicks));
    }
}
