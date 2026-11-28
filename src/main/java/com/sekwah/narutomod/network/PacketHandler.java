package com.sekwah.narutomod.network;

import com.sekwah.narutomod.network.c2s.ServerAbilityActivatePacket;
import com.sekwah.narutomod.network.c2s.ServerAbilityChannelPacket;
import com.sekwah.narutomod.network.c2s.ServerMissionActionPacket;
import com.sekwah.narutomod.network.c2s.ServerRankUpPacket;
import com.sekwah.narutomod.network.c2s.ServerSelectClanPacket;
import com.sekwah.narutomod.network.c2s.ServerTriggerHandJutsuPacket;
import com.sekwah.narutomod.network.c2s.ServerUpdateJutsuLoadoutPacket;
import com.sekwah.narutomod.network.s2c.ClientCameraShakePacket;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import com.sekwah.narutomod.network.s2c.ClientJutsuStatsSyncPacket;
import com.sekwah.narutomod.network.s2c.ClientMissionsSyncPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {

    public static final String PROTOCOL_VERSION = "1";

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // Client to server packets
        registrar.playToServer(ServerUpdateJutsuLoadoutPacket.TYPE, ServerUpdateJutsuLoadoutPacket.STREAM_CODEC, ServerUpdateJutsuLoadoutPacket::handle);
        registrar.playToServer(ServerAbilityActivatePacket.TYPE, ServerAbilityActivatePacket.STREAM_CODEC, ServerAbilityActivatePacket::handle);
        registrar.playToServer(ServerAbilityChannelPacket.TYPE, ServerAbilityChannelPacket.STREAM_CODEC, ServerAbilityChannelPacket::handle);
        registrar.playToServer(ServerSelectClanPacket.TYPE, ServerSelectClanPacket.STREAM_CODEC, ServerSelectClanPacket::handle);
        registrar.playToServer(ServerRankUpPacket.TYPE, ServerRankUpPacket.STREAM_CODEC, ServerRankUpPacket::handle);
        registrar.playToServer(ServerTriggerHandJutsuPacket.TYPE, ServerTriggerHandJutsuPacket.STREAM_CODEC, ServerTriggerHandJutsuPacket::handle);
        registrar.playToServer(ServerMissionActionPacket.TYPE, ServerMissionActionPacket.STREAM_CODEC, ServerMissionActionPacket::handle);

        // Server to client packets
        registrar.playToClient(ClientJutsuEffectPacket.TYPE, ClientJutsuEffectPacket.STREAM_CODEC);
        registrar.playToClient(ClientCameraShakePacket.TYPE, ClientCameraShakePacket.STREAM_CODEC);
        registrar.playToClient(ClientJutsuStatsSyncPacket.TYPE, ClientJutsuStatsSyncPacket.STREAM_CODEC);
        registrar.playToClient(ClientMissionsSyncPacket.TYPE, ClientMissionsSyncPacket.STREAM_CODEC);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    // These are for handling from server side to players.
    public static void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        if (player == null || player.connection == null || !player.connection.hasChannel(payload)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToTrackingAndSelf(CustomPacketPayload msg, Entity target) {
        try {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, msg);
        } catch (UnsupportedOperationException ignored) {
            // Mock test players in GameTestServer lack registered client payload channels.
        }
    }

    public static void sentToTrackingAndSelf(CustomPacketPayload obj, ServerPlayer player) {
        try {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, obj);
        } catch (UnsupportedOperationException ignored) {
            // Mock test players in GameTestServer lack registered client payload channels.
        }
    }

    public static void sentToTracking(CustomPacketPayload obj, Entity entity) {
        try {
            PacketDistributor.sendToPlayersTrackingEntity(entity, obj);
        } catch (UnsupportedOperationException ignored) {
            // Mock test players in GameTestServer lack registered client payload channels.
        }
    }

}
