package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.MissionDefinition;
import com.sekwah.narutomod.capabilities.MissionDefinition.RewardItem;
import com.sekwah.narutomod.capabilities.NinjaMission;
import com.sekwah.narutomod.capabilities.NinjaRank;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Synchronizes DataPack-loaded {@link MissionDefinition} entries from server to client on login and /reload.
 */
public record ClientMissionsSyncPacket(List<MissionDefinition> missions) implements CustomPacketPayload {

    private static final int MAX_MISSIONS = 256;
    private static final int MAX_REWARD_ITEMS = 32;
    private static final int MAX_ID_LENGTH = 64;
    private static final int MAX_RANK_LABEL_LENGTH = 32;
    private static final int MAX_TARGET_COUNT = 1_000_000;
    private static final int MAX_REWARD_XP = 1_000_000;
    private static final int MAX_ITEM_COUNT = 6400;

    public static final CustomPacketPayload.Type<ClientMissionsSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "client_missions_sync"));

    public static final StreamCodec<FriendlyByteBuf, ClientMissionsSyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(ClientMissionsSyncPacket::encode, ClientMissionsSyncPacket::decode);

    public ClientMissionsSyncPacket {
        missions = missions == null ? List.of() : List.copyOf(missions);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientMissionsSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.missions.size());
        for (MissionDefinition m : msg.missions) {
            buf.writeUtf(m.id(), MAX_ID_LENGTH);
            buf.writeUtf(m.rankLabel(), MAX_RANK_LABEL_LENGTH);
            buf.writeEnum(m.requiredRank());
            buf.writeEnum(m.targetType());
            buf.writeVarInt(m.targetCount());
            buf.writeVarInt(m.rewardXp());
            List<RewardItem> items = m.rewardItems();
            buf.writeVarInt(items.size());
            for (RewardItem reward : items) {
                buf.writeIdentifier(reward.itemId());
                buf.writeVarInt(reward.count());
            }
        }
    }

    public static ClientMissionsSyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_MISSIONS) {
            throw new DecoderException("Invalid mission count: " + count);
        }
        List<MissionDefinition> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String id = buf.readUtf(MAX_ID_LENGTH);
            String rankLabel = buf.readUtf(MAX_RANK_LABEL_LENGTH);
            NinjaRank requiredRank = buf.readEnum(NinjaRank.class);
            NinjaMission.TargetType targetType = buf.readEnum(NinjaMission.TargetType.class);
            int targetCount = buf.readVarInt();
            if (targetCount < 1 || targetCount > MAX_TARGET_COUNT) {
                throw new DecoderException("Invalid mission targetCount: " + targetCount);
            }
            int rewardXp = buf.readVarInt();
            if (rewardXp < 0 || rewardXp > MAX_REWARD_XP) {
                throw new DecoderException("Invalid mission rewardXp: " + rewardXp);
            }
            int itemCount = buf.readVarInt();
            if (itemCount < 0 || itemCount > MAX_REWARD_ITEMS) {
                throw new DecoderException("Invalid mission reward item count: " + itemCount);
            }
            List<RewardItem> rewards = new ArrayList<>(itemCount);
            for (int j = 0; j < itemCount; j++) {
                Identifier itemId = buf.readIdentifier();
                int stackCount = buf.readVarInt();
                if (stackCount < 1 || stackCount > MAX_ITEM_COUNT) {
                    throw new DecoderException("Invalid reward item stack count: " + stackCount);
                }
                rewards.add(new RewardItem(itemId, stackCount));
            }
            list.add(new MissionDefinition(id, rankLabel, requiredRank, targetType, targetCount, rewardXp, rewards));
        }
        return new ClientMissionsSyncPacket(list);
    }
}
