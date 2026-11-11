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

    public static float calculateEffectiveChakraCost(com.sekwah.narutomod.abilities.Ability ability, float baseCost, java.util.List<Byte> activeSeals) {
        if (ability != null && ability.matchesExpectedSeals(activeSeals)) {
            return baseCost * 0.75f;
        }
        return baseCost;
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
                            // Validate maximum melee reach distance (6.0 blocks / 36.0 sq blocks) to prevent packet reach exploits
                            if (player.distanceToSqr(living) <= 36.0D) {
                                target = living;
                            }
                        }
                    }

                    com.sekwah.narutomod.abilities.Ability struckAbility = "rasengan".equalsIgnoreCase(activeJutsu)
                            ? com.sekwah.narutomod.abilities.NarutoAbilities.RASENGAN.get()
                            : "chidori".equalsIgnoreCase(activeJutsu)
                            ? com.sekwah.narutomod.abilities.NarutoAbilities.CHIDORI.get()
                            : null;

                    if (struckAbility != null && struckAbility.matchesExpectedSeals(ninjaData.getActiveSeals())) {
                        float discountChakra = struckAbility.getChakraCost() * 0.25f;
                        if (discountChakra > 0) {
                            ninjaData.addChakra(discountChakra);
                            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                    "jutsu.combo.perfect", net.minecraft.network.chat.Component.translatable(struckAbility.getTranslationKey()))
                                    .withStyle(net.minecraft.ChatFormatting.GOLD));
                        }
                    }

                    if ("rasengan".equalsIgnoreCase(activeJutsu)) {
                        RasenganAbility.executeStrike(player, ninjaData, target);
                    } else if ("chidori".equalsIgnoreCase(activeJutsu)) {
                        ChidoriAbility.executeStrike(player, ninjaData, target);
                    }
                    ninjaData.clearSeals();
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
