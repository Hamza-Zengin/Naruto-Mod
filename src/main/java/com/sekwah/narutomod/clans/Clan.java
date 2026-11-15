package com.sekwah.narutomod.clans;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class Clan {
    private Identifier registryName;
    private final Component name;
    private final Component description;

    public Clan(Component name, Component description) {
        this.name = name;
        this.description = description;
    }

    public Identifier getRegistryName() {
        return registryName;
    }

    public Clan setRegistryName(Identifier registryName) {
        this.registryName = registryName;
        return this;
    }

    public Component getName() {
        return name;
    }

    public Component getDescription() {
        return description;
    }
}
