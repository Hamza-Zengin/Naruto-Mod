package com.sekwah.narutomod.item;

import com.mojang.serialization.Codec;
import com.sekwah.narutomod.NarutoMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NarutoDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NarutoMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> JUTSU_SCROLL_ABILITY =
            DATA_COMPONENTS.registerComponentType("jutsu_scroll_ability", builder -> builder
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> KUSANAGI_CHAKRA_ACTIVE =
            DATA_COMPONENTS.registerComponentType("kusanagi_chakra_active", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> KUSANAGI_ACTIVE_UNTIL =
            DATA_COMPONENTS.registerComponentType("kusanagi_active_until", builder -> builder
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
