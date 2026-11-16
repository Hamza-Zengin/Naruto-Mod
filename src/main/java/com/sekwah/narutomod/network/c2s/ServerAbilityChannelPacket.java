package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
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
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Tell the server that the user wants to cast a specific ability.
 */
public class ServerAbilityChannelPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerAbilityChannelPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "server_ability_channel"));

    public static final StreamCodec<FriendlyByteBuf, ServerAbilityChannelPacket> STREAM_CODEC =
            StreamCodec.ofMember(ServerAbilityChannelPacket::encode, ServerAbilityChannelPacket::decode);

    private final Identifier abilityId;
    private final ChannelStatus status;

    public enum ChannelStatus {
        START,
        STOP,
        MIN_ACTIVATE
    }

    public ServerAbilityChannelPacket(Identifier ability, ChannelStatus status) {
        this.abilityId = ability;
        this.status = status;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ServerAbilityChannelPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeIdentifier(msg.abilityId);
        outBuffer.writeInt(msg.status.ordinal());
    }

    public static ServerAbilityChannelPacket decode(FriendlyByteBuf inBuffer) {
        Identifier abilityId = inBuffer.readIdentifier();
        int status = inBuffer.readInt();
        ChannelStatus[] statuses = ChannelStatus.values();
        if (status < 0 || status >= statuses.length) {
            status = 0;
        }
        return new ServerAbilityChannelPacket(abilityId, statuses[status]);
    }

    public static void handle(final ServerAbilityChannelPacket msg, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ServerSecurityValidator.validateBasePacket(player, "ServerAbilityChannelPacket")) {
                return;
            }
            NinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);

            Ability ability = NarutoAbilities.getAbility(msg.abilityId);
            if (ability == null) return;

            if (ability.activationType() == Ability.ActivationType.CHANNELED) {
                if (msg.status == ChannelStatus.START) {
                    if (!ServerSecurityValidator.validateAbilityActivation(player, ninjaData, ability, true)) {
                        return;
                    }
                    if (!ability.canActivate(player, ninjaData)) {
                        return;
                    }
                    ninjaData.setCurrentlyChanneledAbility(player, ability);
                } else if (msg.status == ChannelStatus.STOP) {
                    if (!ServerSecurityValidator.validateAbilityActivation(player, ninjaData, ability, false)) {
                        return;
                    }
                    if (ninjaData.getCurrentlyChanneledAbility() != null && ninjaData.getCurrentlyChanneledAbility().equals(ability.getRegistryName())) {
                        ability.performServer(player, ninjaData, ninjaData.getCurrentlyChanneledTicks());
                        ninjaData.incrementJutsuMastery(ability.getRegistryName(), false);
                        ninjaData.addJutsuCast();
                        ninjaData.setCurrentlyChanneledAbility(player, null);
                    }
                } else if (msg.status == ChannelStatus.MIN_ACTIVATE) {
                    if (!ServerSecurityValidator.validateAbilityActivation(player, ninjaData, ability, false)) {
                        return;
                    }
                    if (ability instanceof Ability.Channeled channeled && channeled.canActivateBelowMinCharge()) {
                        ability.handleCost(player, ninjaData, 0);
                    } else {
                        player.sendSystemMessage(Component.translatable("jutsu.channel.needed", Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
                    }
                }
            }
        });
    }
}
