package com.sekwah.narutomod.abilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sekwah.narutomod.NarutoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class JutsuDataManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<ResourceLocation, JutsuData> JUTSU_DATA = new ConcurrentHashMap<>();

    public record JutsuData(float chakraCost, float staminaCost, int cooldown, float damage) {}

    public JutsuDataManager() {
        super(GSON, "jutsus");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JutsuData> newMap = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                if (entry.getValue().isJsonObject()) {
                    JsonObject obj = entry.getValue().getAsJsonObject();
                    float chakra = obj.has("chakra_cost") ? obj.get("chakra_cost").getAsFloat() : -1f;
                    float stamina = obj.has("stamina_cost") ? obj.get("stamina_cost").getAsFloat() : -1f;
                    int cooldown = obj.has("cooldown") ? obj.get("cooldown").getAsInt() : -1;
                    float damage = obj.has("damage") ? obj.get("damage").getAsFloat() : -1f;
                    newMap.put(id, new JutsuData(chakra, stamina, cooldown, damage));
                }
            } catch (Exception e) {
                NarutoMod.LOGGER.error("Failed to parse jutsu data for {}", id, e);
            }
        }
        JUTSU_DATA.clear();
        JUTSU_DATA.putAll(newMap);
        NarutoMod.LOGGER.info("Loaded {} custom jutsu definitions from data packs.", JUTSU_DATA.size());
    }

    public static JutsuData get(ResourceLocation id) {
        return JUTSU_DATA.get(id);
    }

    public static Map<ResourceLocation, JutsuData> getAll() {
        return Collections.unmodifiableMap(JUTSU_DATA);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new JutsuDataManager());
    }
}
