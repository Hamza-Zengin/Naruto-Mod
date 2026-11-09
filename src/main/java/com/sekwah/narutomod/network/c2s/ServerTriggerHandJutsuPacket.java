package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.abilities.jutsus.ChidoriAbility;
import com.sekwah.narutomod.abilities.jutsus.RasenganAbility;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client when the player left-clicks to strike with an active hand jutsu (Rasengan / Chidori).
 */
public class ServerTriggerHandJutsuPacket {

    private final int targetEntityId;

    public ServerTriggerHandJutsuPacket(int targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public static void encode(ServerTriggerHandJutsuPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeInt(msg.targetEntityId);
    }

    public static ServerTriggerHandJutsuPacket decode(FriendlyByteBuf inBuffer) {
        int id = inBuffer.readInt();
        return new ServerTriggerHandJutsuPacket(id);
    }

    public static class Handler {
        public static void handle(ServerTriggerHandJutsuPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                final ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                    String activeJutsu = ninjaData.getActiveHandJutsu();
                    if (ninjaData.getActiveHandJutsuTicks() <= 0 || activeJutsu.isEmpty()) {
                        return;
                    }

                    LivingEntity target = null;
                    if (msg.targetEntityId >= 0) {
                        Entity entity = player.level().getEntity(msg.targetEntityId);
                        if (entity instanceof LivingEntity living && living.isAlive() && living != player) {
                            target = living;
                        }
                    }

                    if ("rasengan".equalsIgnoreCase(activeJutsu)) {
                        RasenganAbility.executeStrike(player, ninjaData, target);
                    } else if ("chidori".equalsIgnoreCase(activeJutsu)) {
                        ChidoriAbility.executeStrike(player, ninjaData, target);
                    }
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
