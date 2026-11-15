package com.sekwah.narutomod.abilities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sekwah.narutomod.NarutoMod;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class JutsuDataManager extends SimpleJsonResourceReloadListener<JutsuDataManager.JutsuData> {

    private static final Map<Identifier, JutsuData> JUTSU_DATA = new ConcurrentHashMap<>();

    public record JutsuData(float chakraCost, float staminaCost, int cooldown, float damage, String requiredRank) {
        public static final Codec<JutsuData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("chakra_cost", -1f).forGetter(JutsuData::chakraCost),
                Codec.FLOAT.optionalFieldOf("stamina_cost", -1f).forGetter(JutsuData::staminaCost),
                Codec.INT.optionalFieldOf("cooldown", -1).forGetter(JutsuData::cooldown),
                Codec.FLOAT.optionalFieldOf("damage", -1f).forGetter(JutsuData::damage),
                Codec.STRING.optionalFieldOf("requiredRank").forGetter(d -> Optional.ofNullable(d.requiredRank()))
        ).apply(instance, (chakra, stamina, cooldown, damage, rank) ->
                new JutsuData(chakra, stamina, cooldown, damage, rank.orElse(null))));
    }

    public JutsuDataManager() {
        super(JutsuData.CODEC, FileToIdConverter.json("jutsus"));
    }

    @Override
    protected void apply(Map<Identifier, JutsuData> dataMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        JUTSU_DATA.clear();
        JUTSU_DATA.putAll(dataMap);
        NarutoMod.LOGGER.info("Loaded {} custom jutsu definitions from data packs.", JUTSU_DATA.size());
    }

    public static JutsuData get(Identifier id) {
        return JUTSU_DATA.get(id);
    }

    public static Map<Identifier, JutsuData> getAll() {
        return Collections.unmodifiableMap(JUTSU_DATA);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "jutsus"), new JutsuDataManager());
    }
}
