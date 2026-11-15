package com.sekwah.narutomod.registries;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.clans.Clan;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class NarutoRegistries {

    public static final ResourceKey<Registry<Ability>> ABILITY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "abilities"));

    public static final ResourceKey<Registry<Clan>> CLANS_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "clans"));

    public static final Registry<Ability> ABILITY = new RegistryBuilder<>(ABILITY_KEY).create();
    public static final Registry<Clan> CLANS = new RegistryBuilder<>(CLANS_KEY).create();

    public static void init(NewRegistryEvent event) {
        event.register(ABILITY);
        event.register(CLANS);
    }
}
