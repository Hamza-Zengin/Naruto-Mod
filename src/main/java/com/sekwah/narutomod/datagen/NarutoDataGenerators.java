package com.sekwah.narutomod.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.common.data.ExistingFileHelper;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class NarutoDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        PackOutput output = event.getGenerator().getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        if(event.includeServer()) {
            event.getGenerator().addProvider(true, new RecipeGen(output));
        }

        if(event.includeClient()) {
            event.getGenerator().addProvider(true, new NarutoItemModelProvider(output, existingFileHelper));
            event.getGenerator().addProvider(true, new NarutoBlockStateProvider(output, existingFileHelper));
        }
    }
}
