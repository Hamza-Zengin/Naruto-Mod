package com.sekwah.narutomod.registries;

import com.mojang.serialization.MapCodec;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.advancements.NinjaProgressTrigger;
import com.sekwah.narutomod.clans.Clan;
import com.sekwah.narutomod.loot.AddNarutoLootModifier;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public class NarutoRegistries {

    public static final ResourceKey<Registry<Ability>> ABILITY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "abilities"));

    public static final ResourceKey<Registry<Clan>> CLANS_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "clans"));

    public static final Registry<Ability> ABILITY = new RegistryBuilder<>(ABILITY_KEY).create();
    public static final Registry<Clan> CLANS = new RegistryBuilder<>(CLANS_KEY).create();

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NarutoMod.MOD_ID);

    public static final Supplier<MapCodec<AddNarutoLootModifier>> ADD_NARUTO_LOOT =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("add_naruto_loot", () -> AddNarutoLootModifier.CODEC);

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
            DeferredRegister.create(Registries.TRIGGER_TYPE, NarutoMod.MOD_ID);

    public static final Supplier<NinjaProgressTrigger> NINJA_PROGRESS_TRIGGER =
            TRIGGER_TYPES.register("ninja_progress", NinjaProgressTrigger::new);

    public static void init(NewRegistryEvent event) {
        event.register(ABILITY);
        event.register(CLANS);
    }

    public static void register(IEventBus eventBus) {
        GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(eventBus);
        TRIGGER_TYPES.register(eventBus);
    }
}
