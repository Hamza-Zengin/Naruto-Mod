package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.clans.NarutoClans;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S packet to lock in a player's permanent clan choice.
 * STRICT ENFORCEMENT: If player is already locked, request is completely rejected!
 */
public class ServerSelectClanPacket {

    private final ResourceLocation clanId;

    public ServerSelectClanPacket(ResourceLocation clanId) {
        this.clanId = clanId;
    }

    public static void encode(ServerSelectClanPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeResourceLocation(msg.clanId);
    }

    public static ServerSelectClanPacket decode(FriendlyByteBuf inBuffer) {
        ResourceLocation clanId = inBuffer.readResourceLocation();
        return new ServerSelectClanPacket(clanId);
    }

    public static class Handler {
        public static void handle(ServerSelectClanPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                final ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                    if (ninjaData.isClanLocked()) {
                        player.sendSystemMessage(Component.translatable("clan.narutomod.already_locked")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                        return;
                    }

                    // Validate clan exists
                    if (NarutoClans.getClan(msg.clanId) == null) {
                        player.sendSystemMessage(Component.literal("Geçersiz klan!").withStyle(ChatFormatting.RED));
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
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
