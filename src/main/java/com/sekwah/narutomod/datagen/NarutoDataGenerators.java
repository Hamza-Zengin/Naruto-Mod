package com.sekwah.narutomod.datagen;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class NarutoDataGenerators {

    @SubscribeEvent
    public static void gatherServerData(GatherDataEvent.Server event) {
        event.getDefaultPackGenerator().createReloadableRegistryObjects(
                new RegistrySetBuilder().add(RecipeProvider.asBootstrap(RecipeGen::new))
        );
        event.createProvider(NarutoGlobalLootModifierProvider::new);
    }

    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        PackOutput output = event.getGenerator().getPackOutput();
        event.addProvider(new NarutoItemModelProvider(output));
        event.addProvider(new NarutoBlockStateProvider(output));
    }
}
