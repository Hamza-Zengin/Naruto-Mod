package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
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

import java.util.List;

/**
 * C2S packet to request shinobi rank graduation (Academy -> Genin -> Chunin -> Jonin).
 */
public class ServerRankUpPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerRankUpPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "server_rank_up"));

    public static final StreamCodec<FriendlyByteBuf, ServerRankUpPacket> STREAM_CODEC =
            StreamCodec.ofMember(ServerRankUpPacket::encode, ServerRankUpPacket::decode);

    public ServerRankUpPacket() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ServerRankUpPacket msg, FriendlyByteBuf outBuffer) {
    }

    public static ServerRankUpPacket decode(FriendlyByteBuf inBuffer) {
        return new ServerRankUpPacket();
    }

    public static void handle(final ServerRankUpPacket msg, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ServerSecurityValidator.validateBasePacket(player, "ServerRankUpPacket")) {
                return;
            }

            NinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            NinjaRank currentRank = NinjaRank.fromString(ninjaData.getRank());
            if (ServerSecurityValidator.validateRankUp(player, ninjaData)) {
                if (ninjaData.rankUp()) {
                    NinjaRank newRank = NinjaRank.fromString(ninjaData.getRank());
                    com.sekwah.narutomod.registries.NarutoRegistries.NINJA_PROGRESS_TRIGGER.get()
                            .trigger(player, com.sekwah.narutomod.advancements.NinjaProgressTrigger.Action.RANK_UP, newRank.name());
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
                List<Component> missing = currentRank.getMissingRequirements(ninjaData);
                for (Component req : missing) {
                    player.sendSystemMessage(Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                            .append(req.copy().withStyle(ChatFormatting.YELLOW)));
                }
            }
        });
    }
}
