package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.network.ServerSecurityValidator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S packet to lock in a player's permanent clan choice.
 * STRICT ENFORCEMENT: If player is already locked, request is completely rejected!
 */
public class ServerSelectClanPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerSelectClanPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "server_select_clan"));

    public static final StreamCodec<FriendlyByteBuf, ServerSelectClanPacket> STREAM_CODEC =
            StreamCodec.ofMember(ServerSelectClanPacket::encode, ServerSelectClanPacket::decode);

    private final Identifier clanId;

    public ServerSelectClanPacket(Identifier clanId) {
        this.clanId = clanId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ServerSelectClanPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeIdentifier(msg.clanId);
    }

    public static ServerSelectClanPacket decode(FriendlyByteBuf inBuffer) {
        Identifier clanId = inBuffer.readIdentifier();
        return new ServerSelectClanPacket(clanId);
    }

    public static void handle(final ServerSelectClanPacket msg, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ServerSecurityValidator.validateBasePacket(player, "ServerSelectClanPacket")) {
                return;
            }

            NinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (!ServerSecurityValidator.validateClanSelection(player, ninjaData, msg.clanId)) {
                return;
            }

            ninjaData.setClan(msg.clanId);
            ninjaData.setClanLocked(true);

            player.sendSystemMessage(Component.translatable("clan.narutomod.chosen",
                    Component.translatable("clan." + msg.clanId.getNamespace() + "." + msg.clanId.getPath())
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            ).withStyle(ChatFormatting.GREEN));

            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        });
    }
}
