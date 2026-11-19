package com.sekwah.narutomod.worldgen;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom worldgen structure types for Naruto Mod.
 */
public final class NarutoStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, NarutoMod.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<AnchoredJigsawStructure>> ANCHORED_JIGSAW =
            STRUCTURE_TYPES.register("anchored_jigsaw", () -> () -> AnchoredJigsawStructure.CODEC);

    private NarutoStructures() {
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
    }
}
