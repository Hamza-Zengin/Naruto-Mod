package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.util.StateUtils;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

public class NarutoDataSerialisers {

    public static final EntityDataSerializer<AttachFace> ATTACH_FACE = EntityDataSerializer.forValueType(
            StreamCodec.of(
                    (RegistryFriendlyByteBuf buf, AttachFace value) -> buf.writeByte(StateUtils.faceToByte(value)),
                    (RegistryFriendlyByteBuf buf) -> StateUtils.byteToFace(buf.readByte())
            )
    );

    public static final EntityDataSerializer<Direction> BLOCK_DIRECTION = EntityDataSerializer.forValueType(
            StreamCodec.of(
                    (RegistryFriendlyByteBuf buf, Direction value) -> buf.writeByte(value.get2DDataValue()),
                    (RegistryFriendlyByteBuf buf) -> Direction.from2DDataValue(buf.readByte())
            )
    );

    private static final DeferredRegister<EntityDataSerializer<?>> DATA_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, MOD_ID);

    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<AttachFace>> ATTACH_FACE_ENTRY =
            DATA_SERIALIZERS.register("attach_face", () -> ATTACH_FACE);

    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<Direction>> BLOCK_DIRECTION_ENTRY =
            DATA_SERIALIZERS.register("block_direction", () -> BLOCK_DIRECTION);

    public static void register(IEventBus eventBus) {
        DATA_SERIALIZERS.register(eventBus);
    }
}
