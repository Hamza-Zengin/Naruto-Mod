package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaMission;
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
 * Client-to-server packet for accepting, claiming rewards for, or abandoning Shinobi Missions.
 */
public class ServerMissionActionPacket {

    public enum Action {
        ACCEPT_MISSION,
        CLAIM_REWARD,
        ABANDON_MISSION
    }

    private final Action action;
    private final String missionId;

    public ServerMissionActionPacket(Action action, String missionId) {
        this.action = action;
        this.missionId = missionId != null ? missionId : "";
    }

    public static void encode(ServerMissionActionPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeEnum(msg.action);
        outBuffer.writeUtf(msg.missionId, 64);
    }

    public static ServerMissionActionPacket decode(FriendlyByteBuf inBuffer) {
        Action action = inBuffer.readEnum(Action.class);
        String missionId = inBuffer.readUtf(64);
        return new ServerMissionActionPacket(action, missionId);
    }

    public static class Handler {
        public static void handle(ServerMissionActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                    switch (msg.action) {
                        case ACCEPT_MISSION -> {
                            NinjaMission mission = NinjaMission.fromId(msg.missionId);
                            if (mission == null) return;
                            if (!ninjaData.getActiveMissionId().isEmpty()) {
                                player.sendSystemMessage(Component.translatable("naruto.mission.already_active")
                                        .withStyle(ChatFormatting.RED));
                                return;
                            }
                            NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
                            if (!mission.canAccept(rank)) {
                                player.sendSystemMessage(Component.translatable("naruto.mission.rank_too_low",
                                        mission.getRequiredRank().getEnglishName()).withStyle(ChatFormatting.RED));
                                return;
                            }
                            ninjaData.acceptMission(mission);
                            ninjaData.updateServerData(player);
                            player.sendSystemMessage(Component.translatable("naruto.mission.accepted",
                                    Component.translatable(mission.getTranslationKey()).withStyle(ChatFormatting.GOLD))
                                    .withStyle(ChatFormatting.GREEN));
                        }
                        case CLAIM_REWARD -> {
                            NinjaMission active = NinjaMission.fromId(ninjaData.getActiveMissionId());
                            if (active != null && ninjaData.claimMissionReward()) {
                                ninjaData.updateServerData(player);
                                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                                        SoundSource.PLAYERS, 1.0f, 1.2f);
                                player.sendSystemMessage(Component.translatable("naruto.mission.reward_claimed",
                                        Component.translatable(active.getTranslationKey()).withStyle(ChatFormatting.GOLD),
                                        active.getRewardXp()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                            }
                        }
                        case ABANDON_MISSION -> {
                            if (!ninjaData.getActiveMissionId().isEmpty()) {
                                ninjaData.abandonMission();
                                ninjaData.updateServerData(player);
                                player.sendSystemMessage(Component.translatable("naruto.mission.abandoned")
                                        .withStyle(ChatFormatting.YELLOW));
                            }
                        }
                    }
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
