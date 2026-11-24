package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.network.ServerSecurityValidator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Updates the player's 8-slot Jutsu Radial Wheel loadout or active selected slot on the server.
 */
public class ServerUpdateJutsuLoadoutPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerUpdateJutsuLoadoutPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "server_update_jutsu_loadout"));

    public static final StreamCodec<FriendlyByteBuf, ServerUpdateJutsuLoadoutPacket> STREAM_CODEC =
            StreamCodec.ofMember(ServerUpdateJutsuLoadoutPacket::encode, ServerUpdateJutsuLoadoutPacket::decode);

    public enum Action {
        SELECT_SLOT,
        ASSIGN_SLOT
    }

    private final Action action;
    private final int slotIndex;
    private final String abilityIdStr;

    public ServerUpdateJutsuLoadoutPacket(Action action, int slotIndex, String abilityIdStr) {
        this.action = action != null ? action : Action.SELECT_SLOT;
        this.slotIndex = slotIndex;
        this.abilityIdStr = abilityIdStr != null ? abilityIdStr : "";
    }

    public static ServerUpdateJutsuLoadoutPacket selectSlot(int slotIndex) {
        return new ServerUpdateJutsuLoadoutPacket(Action.SELECT_SLOT, slotIndex, "");
    }

    public static ServerUpdateJutsuLoadoutPacket assignSlot(int slotIndex, @javax.annotation.Nullable Identifier abilityId) {
        return new ServerUpdateJutsuLoadoutPacket(Action.ASSIGN_SLOT, slotIndex, abilityId != null ? abilityId.toString() : "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ServerUpdateJutsuLoadoutPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeEnum(msg.action);
        outBuffer.writeInt(msg.slotIndex);
        outBuffer.writeUtf(msg.abilityIdStr, 256);
    }

    public static ServerUpdateJutsuLoadoutPacket decode(FriendlyByteBuf inBuffer) {
        Action action = inBuffer.readEnum(Action.class);
        int slotIndex = inBuffer.readInt();
        String abilityIdStr = inBuffer.readUtf(256);
        return new ServerUpdateJutsuLoadoutPacket(action, slotIndex, abilityIdStr);
    }

    public static void handle(final ServerUpdateJutsuLoadoutPacket msg, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ServerSecurityValidator.validateBasePacket(player, "ServerUpdateJutsuLoadoutPacket")) {
                return;
            }
            if (msg.slotIndex < 0 || msg.slotIndex >= INinjaData.MAX_JUTSU_SLOTS) {
                return;
            }
            NinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (msg.action == Action.SELECT_SLOT) {
                ninjaData.setSelectedJutsuSlot(msg.slotIndex);
            } else if (msg.action == Action.ASSIGN_SLOT) {
                if (msg.abilityIdStr.isEmpty()) {
                    ninjaData.setEquippedJutsu(msg.slotIndex, null);
                    return;
                }
                Identifier abilityId = Identifier.tryParse(msg.abilityIdStr);
                if (abilityId == null) {
                    return;
                }
                Ability ability = NarutoAbilities.getAbility(abilityId);
                if (ability == null || !ability.isWheelEquippable()) {
                    return;
                }
                if (!ServerSecurityValidator.isAbilityUnlocked(ninjaData, ability)) {
                    NarutoMod.LOGGER.warn("Player {} attempted to equip locked ability {} to slot {}",
                            player.getScoreboardName(), abilityId, msg.slotIndex);
                    return;
                }
                ninjaData.setEquippedJutsu(msg.slotIndex, abilityId);
            }
        });
    }
}
