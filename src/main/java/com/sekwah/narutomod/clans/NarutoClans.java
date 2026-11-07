package com.sekwah.narutomod.clans;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

public class NarutoClans {

    public static final DeferredRegister<Clan> CLANS = DeferredRegister.create(new ResourceLocation(MOD_ID, "clans"), MOD_ID);
    public static final Map<ResourceLocation, Clan> CLAN_MAP = new HashMap<>();

    public static final RegistryObject<Clan> SHINOBI = registerClan("shinobi",
            () -> new Clan(Component.translatable("clan.narutomod.shinobi"), Component.translatable("clan.narutomod.shinobi.desc")));

    public static final RegistryObject<Clan> UCHIHA = registerClan("uchiha",
            () -> new Clan(Component.translatable("clan.narutomod.uchiha"), Component.translatable("clan.narutomod.uchiha.desc")));

    public static final RegistryObject<Clan> HYUGA = registerClan("hyuga",
            () -> new Clan(Component.translatable("clan.narutomod.hyuga"), Component.translatable("clan.narutomod.hyuga.desc")));

    public static final RegistryObject<Clan> UZUMAKI = registerClan("uzumaki",
            () -> new Clan(Component.translatable("clan.narutomod.uzumaki"), Component.translatable("clan.narutomod.uzumaki.desc")));

    public static final RegistryObject<Clan> NARA = registerClan("nara",
            () -> new Clan(Component.translatable("clan.narutomod.nara"), Component.translatable("clan.narutomod.nara.desc")));

    private static <T extends Clan> RegistryObject<T> registerClan(String name, java.util.function.Supplier<T> clanSupplier) {
        RegistryObject<T> obj = CLANS.register(name, clanSupplier);
        return obj;
    }

    public static void register(IEventBus eventBus) {
        CLANS.register(eventBus);
    }

    public static Clan getClan(ResourceLocation location) {
        if (location == null) return null;
        for (RegistryObject<Clan> entry : CLANS.getEntries()) {
            if (entry.getId().equals(location)) {
                return entry.get();
            }
        }
        return null;
    }
}
