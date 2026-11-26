package com.sekwah.narutomod.capabilities;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Manages delayed player tick events and ability cooldown tick events
 * (scheduling, zero-allocation tick advancement, and cleanup).
 */
public class NinjaTickScheduler {

    private final ArrayList<DelayedPlayerTickEvent> delayedTickEvents = new ArrayList<>();
    private final HashMap<String, CooldownTickEvent> cooldownTickEvents = new HashMap<>();

    public void scheduleDelayedTickEvent(Consumer<Player> consumer, int tickDelay) {
        if (consumer == null) {
            return;
        }
        this.delayedTickEvents.add(new DelayedPlayerTickEvent(consumer, tickDelay));
    }

    public HashMap<String, CooldownTickEvent> getCooldownEvents() {
        return this.cooldownTickEvents;
    }

    public void setCooldown(String key, int ticks) {
        if (key == null) {
            return;
        }
        if (ticks <= 0) {
            this.cooldownTickEvents.remove(key);
        } else {
            this.cooldownTickEvents.put(key, new CooldownTickEvent(ticks));
        }
    }

    public int getCooldownTicks(String key) {
        if (key == null) {
            return 0;
        }
        CooldownTickEvent event = this.cooldownTickEvents.get(key);
        return (event != null && !event.isComplete()) ? event.ticks : 0;
    }

    public boolean hasCooldown(String key) {
        return getCooldownTicks(key) > 0;
    }

    public void tick(Player player) {
        tickDelayedEvents(player);
        tickCooldowns();
    }

    public void tickDelayedEvents(Player player) {
        // Zero-allocation delayed tick execution: iterate backwards to safely remove and run
        if (!this.delayedTickEvents.isEmpty()) {
            for (int i = this.delayedTickEvents.size() - 1; i >= 0; i--) {
                DelayedPlayerTickEvent event = this.delayedTickEvents.get(i);
                event.tick();
                if (event.shouldRun()) {
                    this.delayedTickEvents.remove(i);
                    event.run(player);
                }
            }
        }
    }

    public void tickCooldowns() {
        // Zero-allocation cooldown tick execution: remove directly without allocating keySet copy
        if (!this.cooldownTickEvents.isEmpty()) {
            this.cooldownTickEvents.values().removeIf(CooldownTickEvent::tickAndCheckComplete);
        }
    }

    public void clearDelayedEvents() {
        this.delayedTickEvents.clear();
    }

    public void clearCooldowns() {
        this.cooldownTickEvents.clear();
    }

    public void clear() {
        this.delayedTickEvents.clear();
        this.cooldownTickEvents.clear();
    }

    public int getPendingDelayedEventCount() {
        return this.delayedTickEvents.size();
    }
}
