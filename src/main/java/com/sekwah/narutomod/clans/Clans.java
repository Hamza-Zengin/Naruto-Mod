package com.sekwah.narutomod.clans;

import net.minecraft.network.chat.Component;

/**
 * Kept for backward compatibility with initial registry setups
 */
public class Clans extends Clan {

    public Clans(Component name, Component description) {
        super(name, description);
    }
}
