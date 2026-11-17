package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.abilities.jutsus.ChidoriAbility;
import com.sekwah.narutomod.abilities.jutsus.RasenganAbility;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.network.ServerSecurityValidator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from client when the player left-clicks to strike with an active hand jutsu (Rasengan / Chidori).
 */
public class ServerTriggerHandJutsuPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerTriggerHandJutsuPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "server_trigger_hand_jutsu"));

    public static final StreamCodec<FriendlyByteBuf, ServerTriggerHandJutsuPacket> STREAM_CODEC =
            StreamCodec.ofMember(ServerTriggerHandJutsuPacket::encode, ServerTriggerHandJutsuPacket::decode);

    private final int targetEntityId;

    public ServerTriggerHandJutsuPacket(int targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ServerTriggerHandJutsuPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeInt(msg.targetEntityId);
    }

    public static ServerTriggerHandJutsuPacket decode(FriendlyByteBuf inBuffer) {
        int id = inBuffer.readInt();
        return new ServerTriggerHandJutsuPacket(id);
    }

    public static void handle(final ServerTriggerHandJutsuPacket msg, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ServerSecurityValidator.validateBasePacket(player, "ServerTriggerHandJutsuPacket")) {
                return;
            }
            NinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            String activeJutsu = ninjaData.getActiveHandJutsu();
            if (ninjaData.getActiveHandJutsuTicks() <= 0 || activeJutsu.isEmpty()) {
                return;
            }

            Ability struckAbility = "rasengan".equalsIgnoreCase(activeJutsu)
                    ? NarutoAbilities.RASENGAN.get()
                    : "chidori".equalsIgnoreCase(activeJutsu)
                    ? NarutoAbilities.CHIDORI.get()
                    : null;
            if (struckAbility == null || !ServerSecurityValidator.isAbilityUnlocked(ninjaData, struckAbility)) {
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

            if ("rasengan".equalsIgnoreCase(activeJutsu)) {
                RasenganAbility.executeStrike(player, ninjaData, target);
            } else if ("chidori".equalsIgnoreCase(activeJutsu)) {
                ChidoriAbility.executeStrike(player, ninjaData, target);
            }
            ninjaData.clearSeals();
        });
    }
}
