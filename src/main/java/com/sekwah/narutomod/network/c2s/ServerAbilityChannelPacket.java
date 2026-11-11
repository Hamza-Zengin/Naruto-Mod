package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tell the server that the user wants to cast a specific ability.
 */
public class ServerAbilityChannelPacket {

    private final ResourceLocation abilityId;
    private ChannelStatus status;

    public enum ChannelStatus {
        START,
        STOP,
        MIN_ACTIVATE
    }

    public ServerAbilityChannelPacket(ResourceLocation ability, ChannelStatus status) {
        this.abilityId = ability;
        this.status = status;
    }

    public static void encode(ServerAbilityChannelPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeResourceLocation(msg.abilityId);
        outBuffer.writeInt(msg.status.ordinal());
    }

    public static ServerAbilityChannelPacket decode(FriendlyByteBuf inBuffer) {
        ResourceLocation abilityId = inBuffer.readResourceLocation();
        int status = inBuffer.readInt();
        ChannelStatus[] statuses = ChannelStatus.values();
        if (status < 0 || status >= statuses.length) {
            status = 0;
        }
        return new ServerAbilityChannelPacket(abilityId, statuses[status]);
    }

    public static class Handler {
        public static void handle(ServerAbilityChannelPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                final ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {

                    Ability ability = NarutoAbilities.getAbility(msg.abilityId);
                    if (ability == null) return;

                    // Silently ignore if player does not have the ability unlocked (no chat spam)
                    if (!ninjaData.isAbilityUnlocked(ability)) {
                        return;
                    }

                    if (ability.activationType() == Ability.ActivationType.CHANNELED) {
                        if (msg.status == ChannelStatus.START) {
                            if (!ability.canActivate(player, ninjaData)) {
                                return;
                            }
                            ninjaData.setCurrentlyChanneledAbility(player, ability);
                        } else if (msg.status == ChannelStatus.STOP) {
                            if(ninjaData.getCurrentlyChanneledAbility() != null && ninjaData.getCurrentlyChanneledAbility().equals(ability.getRegistryName())) {
                                ability.performServer(player, ninjaData, ninjaData.getCurrentlyChanneledTicks());
                                ninjaData.addJutsuCast();
                                ninjaData.setCurrentlyChanneledAbility(player, null);
                            }
                        } else if(msg.status == ChannelStatus.MIN_ACTIVATE) {
                            if (ability instanceof Ability.Channeled channeled && channeled.canActivateBelowMinCharge()) {
                                ability.handleCost(player, ninjaData, 0);
                            } else {
                                player.sendSystemMessage(Component.translatable("jutsu.channel.needed", Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
                            }
                        }
                    }
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
