package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.JutsuDataManager;
import com.sekwah.narutomod.client.events.CameraShakeManager;
import com.sekwah.narutomod.client.events.ClientEffectHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = NarutoMod.MOD_ID, value = Dist.CLIENT)
public class ClientPayloadHandler {

    @SubscribeEvent
    public static void registerClientPayloads(final RegisterClientPayloadHandlersEvent event) {
        event.register(ClientJutsuEffectPacket.TYPE, ClientPayloadHandler::handleJutsuEffect);
        event.register(ClientCameraShakePacket.TYPE, ClientPayloadHandler::handleCameraShake);
        event.register(ClientJutsuStatsSyncPacket.TYPE, ClientPayloadHandler::handleJutsuStatsSync);
    }

    public static void handleJutsuEffect(final ClientJutsuEffectPacket msg, final IPayloadContext context) {
        context.enqueueWork(() -> ClientEffectHandler.handleJutsuEffect(msg));
    }

    public static void handleCameraShake(final ClientCameraShakePacket msg, final IPayloadContext context) {
        context.enqueueWork(() -> CameraShakeManager.triggerShake(msg.intensity, msg.durationTicks));
    }

    public static void handleJutsuStatsSync(final ClientJutsuStatsSyncPacket msg, final IPayloadContext context) {
        context.enqueueWork(() -> JutsuDataManager.applySynced(msg.stats()));
    }
}
