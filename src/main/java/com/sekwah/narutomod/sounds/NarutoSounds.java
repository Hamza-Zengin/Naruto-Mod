package com.sekwah.narutomod.sounds;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

public class NarutoSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);

    public static final RegistryObject<SoundEvent> KUNAI_THUD = register("kunai_thud");

    public static final RegistryObject<SoundEvent> NEEDLE_HIT = register("needle_hit");

    public static final RegistryObject<SoundEvent> SIZZLE = register("sizzle");

    public static final RegistryObject<SoundEvent> SEAL_A = register("seal_a");
    public static final RegistryObject<SoundEvent> SEAL_B = register("seal_b");
    public static final RegistryObject<SoundEvent> SEAL_C = register("seal_c");

    public static final RegistryObject<SoundEvent> JUTSU_CAST = register("jutsu_cast");

    public static final RegistryObject<SoundEvent> LEAP = register("leap");

    public static final RegistryObject<SoundEvent> LONELY_MARCH = register("lonely_march");

    public static final RegistryObject<SoundEvent> CHIDORI = register("chidori");
    public static final RegistryObject<SoundEvent> RASENGAN = register("rasengan");
    public static final RegistryObject<SoundEvent> CLONE_POOF = register("clone_poof");

    public static final RegistryObject<SoundEvent> CHIDORI_LOOP = register("jutsu.chidori.loop");
    public static final RegistryObject<SoundEvent> RASENGAN_LOOP = register("jutsu.rasengan.loop");
    public static final RegistryObject<SoundEvent> FIREBALL_CAST = register("jutsu.fireball.cast");
    public static final RegistryObject<SoundEvent> KIRIN_STRIKE = register("jutsu.kirin.strike");
    public static final RegistryObject<SoundEvent> SHARINGAN_ACTIVATE = register("jutsu.sharingan.activate");

    private static RegistryObject<SoundEvent> register(String key) {
        return SOUNDS.register(key, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, key)));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
