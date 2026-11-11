package com.sekwah.narutomod.trackers;

import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.sekclib.capabilitysync.capabilitysync.tracker.SyncTrackerClone;
import com.sekwah.sekclib.capabilitysync.capabilitysync.tracker.SyncTrackerSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class ToggleAbilityDataSyncTracker implements SyncTrackerSerializer<ToggleAbilityData>, SyncTrackerClone<ToggleAbilityData> {

    @Override
    public void encode(ToggleAbilityData objectToSend, FriendlyByteBuf outBuffer) {
        if (objectToSend == null || objectToSend.getAbilitiesHashSet() == null) {
            outBuffer.writeInt(0);
            return;
        }
        outBuffer.writeInt(objectToSend.getAbilitiesHashSet().size());
        for (ResourceLocation ability : objectToSend.getAbilitiesHashSet()) {
            outBuffer.writeUtf(ability.toString());
        }
    }

    @Override
    public ToggleAbilityData decode(FriendlyByteBuf inBuffer) {
        final int size = inBuffer.readInt();
        if (size <= 0 || size > 256) {
            return new ToggleAbilityData(0);
        }
        ToggleAbilityData data = new ToggleAbilityData(size);
        for(int i = 0; i < size; i++) {
            data.getAbilitiesHashSet().add(new ResourceLocation(inBuffer.readUtf(256)));
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
