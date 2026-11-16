package com.sekwah.narutomod;

import com.google.gson.reflect.TypeToken;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.clans.NarutoClans;
import com.sekwah.narutomod.client.gui.NarutoInGameGUI;
import com.sekwah.narutomod.client.keybinds.NarutoKeyHandler;
import com.sekwah.narutomod.commands.NarutoCommands;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.entity.NarutoDataSerialisers;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.gamerules.NarutoGameRules;
import com.sekwah.narutomod.item.NarutoDataComponents;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.registries.NarutoRegistries;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.trackers.IdentifierIntMapSyncTracker;
import com.sekwah.narutomod.trackers.ToggleAbilityDataSyncTracker;
import com.sekwah.sekclib.capabilitysync.capabilitysync.RegisterCapabilitySyncEvent;
import com.sekwah.sekclib.capabilitysync.capabilitysync.RegisterSyncTrackerTypeEvent;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(NarutoMod.MOD_ID)
public class NarutoMod {

    public static final String MOD_ID = "narutomod";

    public static final Logger LOGGER = LogManager.getLogger("Naruto Mod");

    public NarutoMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.LOCAL, NarutoConfig.MOD_CONFIG, "naruto-mod.toml");

        modEventBus.addListener(NarutoKeyHandler::onRegisterKeyMappings);
        modEventBus.addListener(PacketHandler::register);
        modEventBus.addListener(this::registerSyncTrackers);
        modEventBus.addListener(this::registerCapabilitySync);
        modEventBus.addListener(NarutoRegistries::init);

        NarutoSounds.register(modEventBus);
        NarutoDataSerialisers.register(modEventBus);
        NarutoDataComponents.register(modEventBus);
        NarutoItems.register(modEventBus);
        NarutoBlocks.register(modEventBus);
        NarutoEntities.register(modEventBus);
        NarutoAbilities.register(modEventBus);
        NarutoClans.register(modEventBus);
        NarutoGameRules.register(modEventBus);
        NinjaCapabilityHandler.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(NarutoMod::onServerStarting);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modEventBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(NarutoInGameGUI::new));
        }
    }

    public static void onServerStarting(RegisterCommandsEvent event) {
        NarutoCommands.register(event.getDispatcher());
    }

    public void registerSyncTrackers(RegisterSyncTrackerTypeEvent event) {
        event.registerSyncTracker(ToggleAbilityData.class, new ToggleAbilityDataSyncTracker());
        IdentifierIntMapSyncTracker mapSyncTracker = new IdentifierIntMapSyncTracker();
        event.registerSyncTracker(new TypeToken<java.util.Map<Identifier, Integer>>() {}.getType(), mapSyncTracker);
        event.registerSyncTracker(new TypeToken<java.util.HashMap<Identifier, Integer>>() {}.getType(), mapSyncTracker);
        event.registerSyncTracker(java.util.Map.class, mapSyncTracker);
        event.registerSyncTracker(java.util.HashMap.class, mapSyncTracker);
    }

    public void registerCapabilitySync(RegisterCapabilitySyncEvent event) {
        event.registerPlayerCap(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "ninja_data"), NinjaCapabilityHandler.NINJA_DATA, NinjaData.class);
    }
}
