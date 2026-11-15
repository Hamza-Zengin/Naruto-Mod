package com.sekwah.narutomod.clans;

import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

public class NarutoClans {

    public static final DeferredRegister<Clan> CLANS = DeferredRegister.create(NarutoRegistries.CLANS_KEY, MOD_ID);
    public static final Map<Identifier, Clan> CLAN_MAP = new HashMap<>();

    public static final DeferredHolder<Clan, Clan> SHINOBI = registerClan("shinobi",
            () -> new Clan(Component.translatable("clan.narutomod.shinobi"), Component.translatable("clan.narutomod.shinobi.desc")));

    public static final DeferredHolder<Clan, Clan> UCHIHA = registerClan("uchiha",
            () -> new Clan(Component.translatable("clan.narutomod.uchiha"), Component.translatable("clan.narutomod.uchiha.desc")));

    public static final DeferredHolder<Clan, Clan> HYUGA = registerClan("hyuga",
            () -> new Clan(Component.translatable("clan.narutomod.hyuga"), Component.translatable("clan.narutomod.hyuga.desc")));

    public static final DeferredHolder<Clan, Clan> UZUMAKI = registerClan("uzumaki",
            () -> new Clan(Component.translatable("clan.narutomod.uzumaki"), Component.translatable("clan.narutomod.uzumaki.desc")));

    public static final DeferredHolder<Clan, Clan> NARA = registerClan("nara",
            () -> new Clan(Component.translatable("clan.narutomod.nara"), Component.translatable("clan.narutomod.nara.desc")));

    public static final DeferredHolder<Clan, Clan> SENJU = registerClan("senju",
            () -> new Clan(Component.translatable("clan.narutomod.senju"), Component.translatable("clan.narutomod.senju.desc")));

    public static final DeferredHolder<Clan, Clan> KAGUYA = registerClan("kaguya",
            () -> new Clan(Component.translatable("clan.narutomod.kaguya"), Component.translatable("clan.narutomod.kaguya.desc")));

    private static <T extends Clan> DeferredHolder<Clan, T> registerClan(String name, java.util.function.Supplier<T> clanSupplier) {
        return CLANS.register(name, clanSupplier);
    }

    public static void register(IEventBus eventBus) {
        CLANS.register(eventBus);
    }

    public static Clan getClan(Identifier location) {
        if (location == null) return null;
        for (DeferredHolder<Clan, ? extends Clan> entry : CLANS.getEntries()) {
            if (entry.getId().equals(location)) {
                return entry.get();
            }
        }
        return null;
    }
}
