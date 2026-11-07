package com.sekwah.sekclib.capabilitysync.capabilitysync.tracker.implemented;

import com.sekwah.sekclib.SekCLib;
import com.sekwah.sekclib.capabilitysync.capabilitysync.RegisterSyncTrackerTypeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SekCLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BaseSyncTrackers {
    @SubscribeEvent
    public static void registerSyncTrackerEvent(RegisterSyncTrackerTypeEvent event) {
        IntSyncTracker intTracker = new IntSyncTracker();
        event.registerSyncTracker(int.class, intTracker);
        event.registerSyncTracker(Integer.class, intTracker);

        FloatSyncTracker floatTracker = new FloatSyncTracker();
        event.registerSyncTracker(float.class, floatTracker);
        event.registerSyncTracker(Float.class, floatTracker);

        event.registerSyncTracker(ResourceLocation.class, new ResourceLocationSyncTracker());
        event.registerSyncTracker(String.class, new StringSyncTracker());

        BoolSyncTracker boolTracker = new BoolSyncTracker();
        event.registerSyncTracker(boolean.class, boolTracker);
        event.registerSyncTracker(Boolean.class, boolTracker);

        ByteSyncTracker byteTracker = new ByteSyncTracker();
        event.registerSyncTracker(byte.class, byteTracker);
        event.registerSyncTracker(Byte.class, byteTracker);

        event.registerSyncTracker(Vec3.class, new Vec3SyncTracker());
    }
}
