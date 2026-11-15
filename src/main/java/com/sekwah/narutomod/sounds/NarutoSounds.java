package com.sekwah.narutomod.sounds;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

public class NarutoSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> KUNAI_THUD = register("kunai_thud");

    public static final DeferredHolder<SoundEvent, SoundEvent> NEEDLE_HIT = register("needle_hit");

    public static final DeferredHolder<SoundEvent, SoundEvent> SIZZLE = register("sizzle");

    public static final DeferredHolder<SoundEvent, SoundEvent> SEAL_A = register("seal_a");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEAL_B = register("seal_b");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEAL_C = register("seal_c");

    public static final DeferredHolder<SoundEvent, SoundEvent> JUTSU_CAST = register("jutsu_cast");

    public static final DeferredHolder<SoundEvent, SoundEvent> LEAP = register("leap");

    public static final DeferredHolder<SoundEvent, SoundEvent> LONELY_MARCH = register("lonely_march");

    public static final DeferredHolder<SoundEvent, SoundEvent> CHIDORI = register("chidori");
    public static final DeferredHolder<SoundEvent, SoundEvent> RASENGAN = register("rasengan");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLONE_POOF = register("clone_poof");

    public static final DeferredHolder<SoundEvent, SoundEvent> CHIDORI_LOOP = register("jutsu.chidori.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> RASENGAN_LOOP = register("jutsu.rasengan.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIREBALL_CAST = register("jutsu.fireball.cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> KIRIN_STRIKE = register("jutsu.kirin.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHARINGAN_ACTIVATE = register("jutsu.sharingan.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMATERASU_IGNITE = register("jutsu.amaterasu.ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUSANOO_ACTIVATE = register("jutsu.susanoo.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOOD_PRISON_ERUPT = register("jutsu.wood_prison.erupt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BONE_DANCE_ERUPT = register("jutsu.bone_dance.erupt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUD_WALL_RISE = register("jutsu.mud_wall.rise");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String key) {
        return SOUNDS.register(key, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MOD_ID, key)));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
