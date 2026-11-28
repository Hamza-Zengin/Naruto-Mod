package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ClientMissionsSyncPacket;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads Shinobi mission definitions from DataPacks ({@code data/narutomod/missions/*.json}),
 * seeded with legacy {@link NinjaMission} defaults and synchronized to clients.
 */
public class MissionDataManager extends SimpleJsonResourceReloadListener<MissionDefinition> {

    private static final Map<String, MissionDefinition> MISSIONS = new ConcurrentHashMap<>();

    private static final Comparator<MissionDefinition> MISSION_ORDER = Comparator
            .comparingInt((MissionDefinition m) -> rankLabelOrder(m.rankLabel()))
            .thenComparingInt(m -> m.requiredRank().getLevel())
            .thenComparing(MissionDefinition::id, String.CASE_INSENSITIVE_ORDER);

    static {
        seedDefaults();
    }

    public MissionDataManager() {
        super(MissionDefinition.CODEC, FileToIdConverter.json("missions"));
    }

    private static void seedDefaults() {
        MISSIONS.clear();
        for (NinjaMission legacy : NinjaMission.values()) {
            MissionDefinition def = MissionDefinition.fromLegacy(legacy);
            MISSIONS.put(def.id().toLowerCase(Locale.ROOT), def);
        }
    }

    @Override
    protected void apply(Map<Identifier, MissionDefinition> dataMap, ResourceManager resourceManager, ProfilerFiller profiler) {
        MISSIONS.clear();
        seedDefaults();
        if (dataMap != null) {
            for (Map.Entry<Identifier, MissionDefinition> entry : dataMap.entrySet()) {
                MissionDefinition def = entry.getValue();
                if (def == null) {
                    continue;
                }
                if (def.id().isBlank()) {
                    def = def.withId(entry.getKey().getPath());
                }
                MISSIONS.put(def.id().toLowerCase(Locale.ROOT), def);
            }
        }
        NarutoMod.LOGGER.info("Loaded {} shinobi mission definitions from data packs.", MISSIONS.size());
    }

    public static MissionDefinition get(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String key = id.trim().toLowerCase(Locale.ROOT);
        MissionDefinition def = MISSIONS.get(key);
        if (def != null) {
            return def;
        }
        NinjaMission legacy = NinjaMission.fromId(id);
        return legacy != null ? MissionDefinition.fromLegacy(legacy) : null;
    }

    public static List<MissionDefinition> getAll() {
        List<MissionDefinition> list = new ArrayList<>(MISSIONS.values());
        list.sort(MISSION_ORDER);
        return Collections.unmodifiableList(list);
    }

    public static void applySynced(List<MissionDefinition> synced) {
        MISSIONS.clear();
        if (synced == null || synced.isEmpty()) {
            seedDefaults();
        } else {
            for (MissionDefinition def : synced) {
                if (def != null && !def.id().isBlank()) {
                    MISSIONS.put(def.id().toLowerCase(Locale.ROOT), def);
                }
            }
        }
        NarutoMod.LOGGER.info("Synced {} shinobi mission definitions from server.", MISSIONS.size());
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PacketHandler.sendToPlayer(new ClientMissionsSyncPacket(getAll()), player);
    }

    public static void syncToAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        syncToAll(server.getPlayerList().getPlayers());
    }

    public static void syncToAll(Iterable<ServerPlayer> players) {
        if (players == null) {
            return;
        }
        ClientMissionsSyncPacket packet = new ClientMissionsSyncPacket(getAll());
        for (ServerPlayer player : players) {
            PacketHandler.sendToPlayer(packet, player);
        }
    }

    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "missions"), new MissionDataManager());
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer joiningPlayer = event.getPlayer();
        if (joiningPlayer != null) {
            syncToPlayer(joiningPlayer);
        } else {
            syncToAll(event.getPlayerList().getPlayers());
        }
    }

    private static int rankLabelOrder(String rankLabel) {
        if (rankLabel == null || rankLabel.isBlank()) {
            return 99;
        }
        String upper = rankLabel.trim().toUpperCase(Locale.ROOT);
        if (upper.startsWith("D")) return 0;
        if (upper.startsWith("C")) return 1;
        if (upper.startsWith("B")) return 2;
        if (upper.startsWith("A")) return 3;
        if (upper.startsWith("S")) return 4;
        return 99;
    }
}
