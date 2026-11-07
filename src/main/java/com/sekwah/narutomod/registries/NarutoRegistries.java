package com.sekwah.narutomod.registries;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.clans.Clans;
import com.sekwah.narutomod.abilities.Ability;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public class NarutoRegistries {

    public static Supplier<IForgeRegistry<Ability>> ABILITY;
    public static Supplier<IForgeRegistry<Clans>> CLANS;

    public static void init(NewRegistryEvent event) {
        RegistryBuilder<Ability> abilities = new RegistryBuilder<>();
        abilities.setName(new ResourceLocation(NarutoMod.MOD_ID, "abilities"));
        ABILITY = event.create(abilities);

        RegistryBuilder<Clans> clans = new RegistryBuilder<>();
        clans.setName(new ResourceLocation(NarutoMod.MOD_ID, "clans"));
        CLANS = event.create(clans);
    }
}
