package com.sekwah.narutomod.clans;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Clan {
    private ResourceLocation registryName;
    private final Component name;
    private final Component description;

    public Clan(Component name, Component description) {
        this.name = name;
        this.description = description;
    }

    public ResourceLocation getRegistryName() {
        return registryName;
    }

    public Clan setRegistryName(ResourceLocation registryName) {
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
