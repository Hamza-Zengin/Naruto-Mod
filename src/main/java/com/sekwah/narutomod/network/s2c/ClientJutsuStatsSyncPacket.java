package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.JutsuDataManager.JutsuStats;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Synchronizes DataPack-loaded JutsuStats from server to client on login and /reload.
 */
public record ClientJutsuStatsSyncPacket(Map<Identifier, JutsuStats> stats) implements CustomPacketPayload {

    private static final int MAX_JUTSU_ENTRIES = 512;
    private static final int MAX_EXTRA_PARAMS = 64;

    public static final CustomPacketPayload.Type<ClientJutsuStatsSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "client_jutsu_stats_sync"));

    public static final StreamCodec<FriendlyByteBuf, ClientJutsuStatsSyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(ClientJutsuStatsSyncPacket::encode, ClientJutsuStatsSyncPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientJutsuStatsSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.stats.size());
        for (Map.Entry<Identifier, JutsuStats> entry : msg.stats.entrySet()) {
            buf.writeIdentifier(entry.getKey());
            JutsuStats s = entry.getValue();
            buf.writeFloat(s.chakraCost());
            buf.writeFloat(s.staminaCost());
            buf.writeVarInt(s.cooldownTicks());
            buf.writeFloat(s.baseDamage());
            buf.writeBoolean(s.requiredRank() != null);
            if (s.requiredRank() != null) {
                buf.writeUtf(s.requiredRank(), 64);
            }
            Map<String, Float> extras = s.extraParams();
            buf.writeVarInt(extras.size());
            for (Map.Entry<String, Float> extra : extras.entrySet()) {
                buf.writeUtf(extra.getKey(), 64);
                buf.writeFloat(extra.getValue());
            }
        }
    }

    public static ClientJutsuStatsSyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_JUTSU_ENTRIES) {
            throw new DecoderException("Invalid jutsu stats count: " + count);
        }
        Map<Identifier, JutsuStats> map = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            Identifier id = buf.readIdentifier();
            float chakraCost = buf.readFloat();
            float staminaCost = buf.readFloat();
            int cooldownTicks = buf.readVarInt();
            float baseDamage = buf.readFloat();
            String requiredRank = buf.readBoolean() ? buf.readUtf(64) : null;
            int extraCount = buf.readVarInt();
            if (extraCount < 0 || extraCount > MAX_EXTRA_PARAMS) {
                throw new DecoderException("Invalid extra_params count: " + extraCount);
            }
            Map<String, Float> extras = new HashMap<>(extraCount);
            for (int j = 0; j < extraCount; j++) {
                String key = buf.readUtf(64);
                float val = buf.readFloat();
                extras.put(key, val);
            }
            map.put(id, new JutsuStats(chakraCost, staminaCost, cooldownTicks, baseDamage, requiredRank, extras));
        }
        return new ClientJutsuStatsSyncPacket(map);
    }
}
