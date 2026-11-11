package com.sekwah.narutomod.trackers;

import com.sekwah.sekclib.capabilitysync.capabilitysync.tracker.SyncTrackerClone;
import com.sekwah.sekclib.capabilitysync.capabilitysync.tracker.SyncTrackerSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Sync tracker serializer and cloner for Map<ResourceLocation, Integer>,
 * used by SekCLib to synchronize jutsu mastery data to the client.
 */
public class ResourceLocationIntMapSyncTracker implements SyncTrackerSerializer<Map<ResourceLocation, Integer>>, SyncTrackerClone<Map<ResourceLocation, Integer>> {

    private static final int MAX_ENTRIES = 2048;

    @Override
    public void encode(Map<ResourceLocation, Integer> objectToSend, FriendlyByteBuf outBuffer) {
        if (objectToSend == null) {
            outBuffer.writeInt(-1);
            return;
        }
        int size = Math.min(objectToSend.size(), MAX_ENTRIES);
        outBuffer.writeInt(size);
        int written = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : objectToSend.entrySet()) {
            if (written++ >= MAX_ENTRIES) {
                break;
            }
            outBuffer.writeResourceLocation(entry.getKey());
            outBuffer.writeInt(entry.getValue() != null ? entry.getValue() : 0);
        }
    }

    @Override
    public Map<ResourceLocation, Integer> decode(FriendlyByteBuf inBuffer) {
        int size = inBuffer.readInt();
        if (size <= 0 || size > MAX_ENTRIES) {
            return new HashMap<>();
        }
        Map<ResourceLocation, Integer> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation key = inBuffer.readResourceLocation();
            int val = inBuffer.readInt();
            map.put(key, val);
        }
        return map;
    }

    @Override
    public Map<ResourceLocation, Integer> clone(Map<ResourceLocation, Integer> data) {
        return data != null ? new HashMap<>(data) : new HashMap<>();
    }
}
