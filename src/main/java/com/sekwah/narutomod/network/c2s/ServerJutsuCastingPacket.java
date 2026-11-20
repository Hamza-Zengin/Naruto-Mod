package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.ServerSecurityValidator;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tells the server that the user is entering jutsu keys
 */
public class ServerJutsuCastingPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerJutsuCastingPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "server_jutsu_casting"));

    public static final StreamCodec<FriendlyByteBuf, ServerJutsuCastingPacket> STREAM_CODEC =
            StreamCodec.ofMember(ServerJutsuCastingPacket::encode, ServerJutsuCastingPacket::decode);

    private static final Map<UUID, Long> LAST_SEAL_TIME = new ConcurrentHashMap<>();

    private final int jutsuKey;

    public ServerJutsuCastingPacket(int jutsuKey) {
        this.jutsuKey = jutsuKey;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ServerJutsuCastingPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeInt(msg.jutsuKey);
    }

    public static ServerJutsuCastingPacket decode(FriendlyByteBuf inBuffer) {
        int jutsuKey = inBuffer.readInt();
        return new ServerJutsuCastingPacket(jutsuKey);
    }

    public static void handle(final ServerJutsuCastingPacket msg, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ServerSecurityValidator.validateBasePacket(player, "ServerJutsuCastingPacket")) {
                return;
            }
            if (msg.jutsuKey < 1 || msg.jutsuKey > 3) {
                NarutoMod.LOGGER.warn("Player {} sent invalid hand seal key {}", player.getScoreboardName(), msg.jutsuKey);
                return;
            }

            long now = System.currentTimeMillis();
            Long last = LAST_SEAL_TIME.get(player.getUUID());
            if (last != null && (now - last) < 60) {
                return; // Rate limit: drop packet spam to prevent audio flooding
            }
            LAST_SEAL_TIME.put(player.getUUID(), now);
            if (LAST_SEAL_TIME.size() > 500) {
                LAST_SEAL_TIME.entrySet().removeIf(e -> (now - e.getValue()) > 60000);
            }

            SoundEvent playSound = switch (msg.jutsuKey) {
                case 1 -> NarutoSounds.SEAL_A.get();
                case 2 -> NarutoSounds.SEAL_B.get();
                case 3 -> NarutoSounds.SEAL_C.get();
                default -> null;
            };
            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            int sealCount = ninjaData.getActiveSeals().size();
            float pitch = Math.min(1.4f, 0.9f + (sealCount * 0.08f));

            if (playSound != null) {
                player.level().playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        playSound, SoundSource.PLAYERS, 1.0f, pitch);
            }

            ninjaData.addSeal((byte) msg.jutsuKey);
        });
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            LAST_SEAL_TIME.remove(playerId);
        }
    }
}
