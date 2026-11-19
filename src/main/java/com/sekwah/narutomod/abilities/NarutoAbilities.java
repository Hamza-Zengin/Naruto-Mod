package com.sekwah.narutomod.abilities;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.jutsus.*;
import com.sekwah.narutomod.abilities.utility.*;
import com.sekwah.narutomod.network.c2s.ServerAbilityActivatePacket;
import com.sekwah.narutomod.network.c2s.ServerAbilityChannelPacket;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

@EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class NarutoAbilities {

    public static final DeferredRegister<Ability> ABILITY = DeferredRegister.create(NarutoRegistries.ABILITY_KEY, MOD_ID);

    public static final Map<Long, Identifier> COMBO_MAP = new HashMap<>();

    // Utility
    public static final DeferredHolder<Ability, LeapAbility> LEAP = ABILITY.register("leap", LeapAbility::new);
    public static final DeferredHolder<Ability, WaterWalkAbility> WATER_WALK = ABILITY.register("water_walk", WaterWalkAbility::new);
    public static final DeferredHolder<Ability, ChakraChargeAbility> CHAKRA_CHARGE = ABILITY.register("chakra_charge", ChakraChargeAbility::new);

    // Ninjutsu / Taijutsu
    public static final DeferredHolder<Ability, FireballJutsuAbility> FIREBALL = ABILITY.register("fireball", FireballJutsuAbility::new);
    public static final DeferredHolder<Ability, KawarimiJutsuAbility> KAWARIMI = ABILITY.register("kawarimi", KawarimiJutsuAbility::new);
    public static final DeferredHolder<Ability, RasenganAbility> RASENGAN = ABILITY.register("rasengan", RasenganAbility::new);
    public static final DeferredHolder<Ability, ChidoriAbility> CHIDORI = ABILITY.register("chidori", ChidoriAbility::new);
    public static final DeferredHolder<Ability, ShadowCloneAbility> SHADOW_CLONE = ABILITY.register("shadow_clone", ShadowCloneAbility::new);

    // Elemental
    public static final DeferredHolder<Ability, WaterBulletAbility> WATER_BULLET = ABILITY.register("water_bullet", WaterBulletAbility::new);
    public static final DeferredHolder<Ability, WindBreakerAbility> WIND_BREAKER = ABILITY.register("wind_breaker", WindBreakerAbility::new);
    public static final DeferredHolder<Ability, MudWallAbility> MUD_WALL = ABILITY.register("mud_wall", MudWallAbility::new);
    public static final DeferredHolder<Ability, EarthDragonBulletAbility> EARTH_DRAGON_BULLET = ABILITY.register("earth_dragon_bullet", EarthDragonBulletAbility::new);
    public static final DeferredHolder<Ability, VacuumSpheresAbility> VACUUM_SPHERES = ABILITY.register("vacuum_spheres", VacuumSpheresAbility::new);
    public static final DeferredHolder<Ability, StoneSpikesAbility> STONE_SPIKES = ABILITY.register("stone_spikes", StoneSpikesAbility::new);
    public static final DeferredHolder<Ability, WaterDragonAbility> WATER_DRAGON = ABILITY.register("water_dragon", WaterDragonAbility::new);
    public static final DeferredHolder<Ability, PhoenixFlowerAbility> PHOENIX_FLOWER = ABILITY.register("phoenix_flower", PhoenixFlowerAbility::new);
    public static final DeferredHolder<Ability, ChidoriStreamAbility> CHIDORI_STREAM = ABILITY.register("chidori_stream", ChidoriStreamAbility::new);

    // Medical Ninjutsu
    public static final DeferredHolder<Ability, MysticalPalmAbility> MYSTICAL_PALM = ABILITY.register("mystical_palm", MysticalPalmAbility::new);

    // Dōjutsu
    public static final DeferredHolder<Ability, SharinganAbility> SHARINGAN = ABILITY.register("sharingan", SharinganAbility::new);
    public static final DeferredHolder<Ability, ByakuganAbility> BYAKUGAN = ABILITY.register("byakugan", ByakuganAbility::new);

    // Advanced & Forbidden Jutsus
    public static final DeferredHolder<Ability, AmaterasuAbility> AMATERASU = ABILITY.register("amaterasu", AmaterasuAbility::new);
    public static final DeferredHolder<Ability, SusanooRibcageAbility> SUSANOO_RIBCAGE = ABILITY.register("susanoo_ribcage", SusanooRibcageAbility::new);
    public static final DeferredHolder<Ability, EightGatesAbility> EIGHT_GATES = ABILITY.register("eight_gates", EightGatesAbility::new);
    public static final DeferredHolder<Ability, EighthGateOfDeathAbility> EIGHT_GATES_DEATH = ABILITY.register("eight_gates_death", EighthGateOfDeathAbility::new);
    public static final DeferredHolder<Ability, KirinAbility> KIRIN = ABILITY.register("kirin", KirinAbility::new);
    public static final DeferredHolder<Ability, WoodPrisonAbility> WOOD_PRISON = ABILITY.register("wood_prison", WoodPrisonAbility::new);
    public static final DeferredHolder<Ability, BoneDanceAbility> BONE_DANCE = ABILITY.register("bone_dance", BoneDanceAbility::new);
    public static final DeferredHolder<Ability, ShadowPossessionAbility> SHADOW_POSSESSION = ABILITY.register("shadow_possession", ShadowPossessionAbility::new);
    public static final DeferredHolder<Ability, RasenshurikenAbility> RASENSHURIKEN = ABILITY.register("rasenshuriken", RasenshurikenAbility::new);
    public static final DeferredHolder<Ability, TsukuyomiAbility> TSUKUYOMI = ABILITY.register("tsukuyomi", TsukuyomiAbility::new);
    public static final DeferredHolder<Ability, HakkeKushoAbility> HAKKE_KUSHO = ABILITY.register("hakke_kusho", HakkeKushoAbility::new);
    public static final DeferredHolder<Ability, AdamantineChainsAbility> ADAMANTINE_CHAINS = ABILITY.register("adamantine_chains", AdamantineChainsAbility::new);
    public static final DeferredHolder<Ability, SummoningAbility> SUMMONING = ABILITY.register("summoning", SummoningAbility::new);

    public static void register(IEventBus eventBus) {
        ABILITY.register(eventBus);
    }

    public static void registerKeyCombos() {
        ABILITY.getEntries().forEach(abilityEntry -> {
            Ability ability = abilityEntry.get();
            ability.setRegistryName(abilityEntry.getId());
            long combo = ability.defaultCombo();
            if (combo > 0) {
                if (COMBO_MAP.containsKey(combo)) {
                    NarutoMod.LOGGER.error("Ability already registered with combo {}", combo);
                } else {
                    COMBO_MAP.put(combo, abilityEntry.getId());
                }
            }
        });
    }

    public static Ability getAbility(Identifier id) {
        if (id == null) return null;
        for (DeferredHolder<Ability, ? extends Ability> entry : ABILITY.getEntries()) {
            if (entry.getId().equals(id)) {
                return entry.get();
            }
        }
        return null;
    }

    public static Ability getAbilityFromCombo(long combo) {
        if (COMBO_MAP.containsKey(combo)) {
            return getAbility(COMBO_MAP.get(combo));
        }
        return null;
    }

    public static void triggerAbility(Identifier ability) {
        ClientPacketDistributor.sendToServer(new ServerAbilityActivatePacket(ability));
    }

    public static boolean triggerAbility(long combo) {
        if (COMBO_MAP.containsKey(combo)) {
            triggerAbility(COMBO_MAP.get(combo));
            return true;
        }
        return false;
    }

    public static boolean handleCharging(long combo, ServerAbilityChannelPacket.ChannelStatus channelStatus) {
        if (COMBO_MAP.containsKey(combo)) {
            Identifier abilityResource = COMBO_MAP.get(combo);
            ClientPacketDistributor.sendToServer(new ServerAbilityChannelPacket(abilityResource, channelStatus));
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(NarutoAbilities::registerKeyCombos);
    }
}
