package com.sekwah.narutomod.capabilities;

import net.minecraft.nbt.CompoundTag;

/**
 * Legacy package-private alias forwarding to {@link NinjaDataSerializer}.
 */
final class NinjaDataNbtCodec {

    private NinjaDataNbtCodec() {}

    static void save(NinjaData data, CompoundTag nbt) {
        NinjaDataSerializer.serializeNBT(data, nbt);
    }

    static void load(NinjaData data, CompoundTag compoundTag) {
        NinjaDataSerializer.deserializeNBT(data, compoundTag);
    }
}
