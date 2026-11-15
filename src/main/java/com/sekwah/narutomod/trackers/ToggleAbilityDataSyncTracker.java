package com.sekwah.narutomod.trackers;

import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.sekclib.capabilitysync.capabilitysync.tracker.SyncTrackerClone;
import com.sekwah.sekclib.capabilitysync.capabilitysync.tracker.SyncTrackerSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public class ToggleAbilityDataSyncTracker implements SyncTrackerSerializer<ToggleAbilityData>, SyncTrackerClone<ToggleAbilityData> {

    private static final int MAX_ELEMENTS = 256;

    @Override
    public void encode(ToggleAbilityData objectToSend, FriendlyByteBuf outBuffer) {
        if (objectToSend == null || objectToSend.getAbilitiesHashSet() == null) {
            outBuffer.writeInt(0);
            return;
        }
        int count = Math.min(objectToSend.getAbilitiesHashSet().size(), MAX_ELEMENTS);
        outBuffer.writeInt(count);
        int written = 0;
        for (Identifier ability : objectToSend.getAbilitiesHashSet()) {
            if (written++ >= MAX_ELEMENTS) {
                break;
            }
            outBuffer.writeIdentifier(ability);
        }
    }

    @Override
    public ToggleAbilityData decode(FriendlyByteBuf inBuffer) {
        final int size = inBuffer.readInt();
        if (size <= 0 || size > MAX_ELEMENTS) {
            return new ToggleAbilityData(0);
        }
        ToggleAbilityData data = new ToggleAbilityData(size);
        for (int i = 0; i < size; i++) {
            data.getAbilitiesHashSet().add(inBuffer.readIdentifier());
        }
        return data;
    }

    @Override
    public ToggleAbilityData clone(ToggleAbilityData data) {
        if (data == null || data.getAbilitiesHashSet() == null) {
            return new ToggleAbilityData();
        }
        ToggleAbilityData cloned = new ToggleAbilityData(data.getAbilitiesHashSet().size());
        cloned.getAbilitiesHashSet().addAll(data.getAbilitiesHashSet());
        return cloned;
    }
}
