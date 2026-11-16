package com.sekwah.narutomod.abilities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ClientJutsuStatsSyncPacket;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class JutsuDataManager extends SimpleJsonResourceReloadListener<JutsuDataManager.JutsuStats> {

    private static final Map<Identifier, JutsuStats> JUTSU_DATA = new ConcurrentHashMap<>();

    public record JutsuStats(
            float chakraCost,
            float staminaCost,
            int cooldownTicks,
            float baseDamage,
            String requiredRank,
            Map<String, Float> extraParams
    ) {
        public JutsuStats {
            extraParams = extraParams == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(extraParams));
        }

        public float getExtraParam(String key, float defaultValue) {
            Float val = extraParams.get(key);
            return val != null ? val : defaultValue;
        }

        public static final Codec<JutsuStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("chakra_cost", -1f).forGetter(JutsuStats::chakraCost),
                Codec.FLOAT.optionalFieldOf("stamina_cost", -1f).forGetter(JutsuStats::staminaCost),
                Codec.INT.optionalFieldOf("cooldown_ticks", -1).forGetter(JutsuStats::cooldownTicks),
                Codec.FLOAT.optionalFieldOf("base_damage", -1f).forGetter(JutsuStats::baseDamage),
                Codec.STRING.optionalFieldOf("requiredRank").forGetter(d -> Optional.ofNullable(d.requiredRank())),
                Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("extra_params", Map.of()).forGetter(JutsuStats::extraParams)
        ).apply(instance, (chakra, stamina, cooldownTicks, baseDamage, rank, extraParams) ->
                new JutsuStats(chakra, stamina, cooldownTicks, baseDamage, rank.orElse(null), extraParams)));
    }

    public JutsuDataManager() {
        super(JutsuStats.CODEC, FileToIdConverter.json("jutsus"));
    }

    @Override
    protected void apply(Map<Identifier, JutsuStats> dataMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        JUTSU_DATA.clear();
        JUTSU_DATA.putAll(dataMap);
        NarutoMod.LOGGER.info("Loaded {} custom jutsu definitions from data packs.", JUTSU_DATA.size());
    }

    public static void applySynced(Map<Identifier, JutsuStats> synced) {
        JUTSU_DATA.clear();
        if (synced != null) {
            JUTSU_DATA.putAll(synced);
        }
        NarutoMod.LOGGER.info("Synced {} jutsu definitions from server.", JUTSU_DATA.size());
    }

    public static JutsuStats get(Identifier id) {
        return JUTSU_DATA.get(id);
    }

    public static Map<Identifier, JutsuStats> getAll() {
        return Collections.unmodifiableMap(JUTSU_DATA);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "jutsus"), new JutsuDataManager());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ClientJutsuStatsSyncPacket packet = new ClientJutsuStatsSyncPacket(new HashMap<>(JUTSU_DATA));
        ServerPlayer joiningPlayer = event.getPlayer();
        if (joiningPlayer != null) {
            PacketHandler.sendToPlayer(packet, joiningPlayer);
        } else {
            for (ServerPlayer player : event.getPlayerList().getPlayers()) {
                PacketHandler.sendToPlayer(packet, player);
            }
        }
    }
}
