package com.sekwah.narutomod.abilities;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.jutsus.*;
import com.sekwah.narutomod.abilities.utility.*;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.c2s.ServerAbilityActivatePacket;
import com.sekwah.narutomod.network.c2s.ServerAbilityChannelPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NarutoAbilities {

    public static final DeferredRegister<Ability> ABILITY = DeferredRegister.create(new ResourceLocation(MOD_ID, "abilities"), MOD_ID);

    public static final Map<Long, ResourceLocation> COMBO_MAP = new HashMap<>();

    // Utility
    public static final RegistryObject<LeapAbility> LEAP = ABILITY.register("leap", LeapAbility::new);
    public static final RegistryObject<ChakraDashAbility> CHAKRA_DASH = ABILITY.register("chakra_dash", ChakraDashAbility::new);
    public static final RegistryObject<WaterWalkAbility> WATER_WALK = ABILITY.register("water_walk", WaterWalkAbility::new);
    public static final RegistryObject<ChakraChargeAbility> CHAKRA_CHARGE = ABILITY.register("chakra_charge", ChakraChargeAbility::new);

    // Ninjutsu / Taijutsu
    public static final RegistryObject<FireballJutsuAbility> FIREBALL = ABILITY.register("fireball", FireballJutsuAbility::new);
    public static final RegistryObject<KawarimiJutsuAbility> KAWARIMI = ABILITY.register("kawarimi", KawarimiJutsuAbility::new);
    public static final RegistryObject<RasenganAbility> RASENGAN = ABILITY.register("rasengan", RasenganAbility::new);
    public static final RegistryObject<ChidoriAbility> CHIDORI = ABILITY.register("chidori", ChidoriAbility::new);
    public static final RegistryObject<ShadowCloneAbility> SHADOW_CLONE = ABILITY.register("shadow_clone", ShadowCloneAbility::new);

    // Elemental
    public static final RegistryObject<WaterBulletAbility> WATER_BULLET = ABILITY.register("water_bullet", WaterBulletAbility::new);
    public static final RegistryObject<WindBreakerAbility> WIND_BREAKER = ABILITY.register("wind_breaker", WindBreakerAbility::new);
    public static final RegistryObject<MudWallAbility> MUD_WALL = ABILITY.register("mud_wall", MudWallAbility::new);

    // Dōjutsu
    public static final RegistryObject<SharinganAbility> SHARINGAN = ABILITY.register("sharingan", SharinganAbility::new);
    public static final RegistryObject<ByakuganAbility> BYAKUGAN = ABILITY.register("byakugan", ByakuganAbility::new);

    // Advanced & Forbidden Jutsus
    public static final RegistryObject<AmaterasuAbility> AMATERASU = ABILITY.register("amaterasu", AmaterasuAbility::new);
    public static final RegistryObject<SusanooRibcageAbility> SUSANOO_RIBCAGE = ABILITY.register("susanoo_ribcage", SusanooRibcageAbility::new);
    public static final RegistryObject<EightGatesAbility> EIGHT_GATES = ABILITY.register("eight_gates", EightGatesAbility::new);
    public static final RegistryObject<KirinAbility> KIRIN = ABILITY.register("kirin", KirinAbility::new);

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

    public static Ability getAbility(ResourceLocation id) {
        if (id == null) return null;
        for (RegistryObject<Ability> entry : ABILITY.getEntries()) {
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

    public static void triggerAbility(ResourceLocation ability) {
        PacketHandler.sendToServer(new ServerAbilityActivatePacket(ability));
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
            ResourceLocation abilityResource = COMBO_MAP.get(combo);
            PacketHandler.sendToServer(new ServerAbilityChannelPacket(abilityResource, channelStatus));
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(NarutoAbilities::registerKeyCombos);
    }
}
