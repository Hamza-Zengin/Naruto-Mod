package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S packet to request shinobi rank graduation (Academy -> Genin -> Chunin -> Jonin).
 */
public class ServerRankUpPacket {

    public ServerRankUpPacket() {
    }

    public static void encode(ServerRankUpPacket msg, FriendlyByteBuf outBuffer) {
    }

    public static ServerRankUpPacket decode(FriendlyByteBuf inBuffer) {
        return new ServerRankUpPacket();
    }

    public static class Handler {
        public static void handle(ServerRankUpPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                final ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                    NinjaRank currentRank = NinjaRank.fromString(ninjaData.getRank());
                    if (currentRank.canRankUp(ninjaData)) {
                        if (ninjaData.rankUp()) {
                            NinjaRank newRank = NinjaRank.fromString(ninjaData.getRank());
                            player.sendSystemMessage(Component.translatable("naruto.rank.promoted",
                                    Component.literal(newRank.getTurkishName() + " (" + newRank.getEnglishName() + ")")
                                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                            ).withStyle(ChatFormatting.GREEN));

                            player.level().playSound(null, player.blockPosition(),
                                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                            player.level().playSound(null, player.blockPosition(),
                                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2f, 1.0f);
                        }
                    } else {
                        player.sendSystemMessage(Component.translatable("naruto.rank.not_ready").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                        java.util.List<Component> missing = currentRank.getMissingRequirements(ninjaData);
                        for (Component req : missing) {
                            player.sendSystemMessage(Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                                    .append(req.copy().withStyle(ChatFormatting.YELLOW)));
                        }
                    }
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
