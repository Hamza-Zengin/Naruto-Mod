package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.advancements.NinjaProgressTrigger;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.network.ServerSecurityValidator;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import com.sekwah.narutomod.registries.NarutoRegistries;
import com.sekwah.sekclib.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;

/**
 * Tell the server that the user wants to cast a specific ability.
 */
public class ServerAbilityActivatePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerAbilityActivatePacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "server_ability_activate"));

    public static final StreamCodec<FriendlyByteBuf, ServerAbilityActivatePacket> STREAM_CODEC =
            StreamCodec.ofMember(ServerAbilityActivatePacket::encode, ServerAbilityActivatePacket::decode);

    private final Identifier abilityId;

    public ServerAbilityActivatePacket(Identifier ability) {
        this.abilityId = ability;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ServerAbilityActivatePacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeIdentifier(msg.abilityId);
    }

    public static ServerAbilityActivatePacket decode(FriendlyByteBuf inBuffer) {
        Identifier abilityId = inBuffer.readIdentifier();
        return new ServerAbilityActivatePacket(abilityId);
    }

    public static void handle(final ServerAbilityActivatePacket msg, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!ServerSecurityValidator.validateBasePacket(player, "ServerAbilityActivatePacket")) {
                return;
            }
            NinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);

            Ability ability = NarutoAbilities.getAbility(msg.abilityId);
            if (ability == null) return;

            if (ability.activationType() == Ability.ActivationType.INSTANT) {
                if (!ServerSecurityValidator.validateAbilityActivation(player, ninjaData, ability, true)) {
                    return;
                }
                if (!ability.canActivate(player, ninjaData)) {
                    return;
                }

                int discountPercent = ability.getPerfectComboDiscountPercent(ninjaData);
                if (ability.handleCost(player, ninjaData)) {
                    if (ability.logInChat()) {
                        player.sendSystemMessage(Component.translatable("jutsu.cast", Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
                        if (discountPercent > 0) {
                            player.sendSystemMessage(Component.translatable("jutsu.combo.perfect",
                                    Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW),
                                    discountPercent).withStyle(ChatFormatting.GOLD));
                        }
                    }
                    int rawDelay = (ability.requiresSealActivationDelay() && discountPercent > 0) ? ability.getActivationDelayTicks(ninjaData) : 0;
                    int delay = Ability.applyArmorDelayReduction(player, rawDelay);
                    if (delay > 0) {
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4f, 1.2f);
                        ninjaData.scheduleDelayedTickEvent(p -> {
                            if (p instanceof ServerPlayer sp && !ServerSecurityValidator.validatePlayerState(sp)) {
                                return;
                            }
                            if (ability.castingSound() != null) {
                                p.level().playSound(null,
                                        p.getX(), p.getY(), p.getZ(), ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                            }
                            ability.performServer(p, ninjaData);
                            if (p instanceof ServerPlayer sp) {
                                sendHandSealSpark(sp, ability);
                            }
                        }, delay);
                    } else {
                        if (ability.castingSound() != null) {
                            player.level().playSound(null,
                                    player.getX(), player.getY(), player.getZ(), ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                        }
                        ability.performServer(player, ninjaData);
                        sendHandSealSpark(player, ability);
                    }
                    ninjaData.addJutsuCast();
                    ninjaData.clearSeals();
                    NarutoRegistries.NINJA_PROGRESS_TRIGGER.get()
                            .trigger(player, NinjaProgressTrigger.Action.USE_JUTSU, msg.abilityId.getPath());

                    if (ability instanceof Ability.Cooldown cooldownAbility) {
                        cooldownAbility.registerCooldown(ninjaData, ability.getTranslationKey());
                    }
                }
            } else if (ability.activationType() == Ability.ActivationType.TOGGLE) {
                ToggleAbilityData abilityTracker = ninjaData.getToggleAbilityData();
                HashSet<Identifier> abilities = abilityTracker.getAbilitiesHashSet();
                if (abilities.contains(ability.getRegistryName())) {
                    if (!ServerSecurityValidator.validateAbilityActivation(player, ninjaData, ability, false)) {
                        return;
                    }
                    if (NarutoAbilities.EIGHT_GATES_DEATH.getId().equals(ability.getRegistryName())) {
                        player.sendSystemMessage(Component.translatable("jutsu.eight_gates_death.cannot_close")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                        return;
                    }
                    // Toggle ability off
                    abilityTracker.removeAbilityEnded(player, ninjaData, ability);
                } else {
                    if (!ServerSecurityValidator.validateAbilityActivation(player, ninjaData, ability, true)) {
                        return;
                    }
                    if (!ability.canActivate(player, ninjaData)) {
                        return;
                    }
                    // Toggle ability on
                    abilityTracker.addAbilityStarted(player, ninjaData, ability);
                    if (ServerSecurityValidator.shouldCountToggleCast(player, ability.getRegistryName())) {
                        ninjaData.addJutsuCast();
                        NarutoRegistries.NINJA_PROGRESS_TRIGGER.get()
                                .trigger(player, NinjaProgressTrigger.Action.USE_JUTSU, msg.abilityId.getPath());
                    }
                }
            }
        });
    }

    private static void sendHandSealSpark(ServerPlayer player, Ability ability) {
        ChakraElement element = ability.getChakraElement();
        int colorHex = switch (element) {
            case FIRE -> 0xFF5511;
            case WATER -> 0x2299FF;
            case EARTH -> 0xAA7733;
            case WIND -> 0x66EEAA;
            case LIGHTNING -> 0x55CCFF;
            default -> 0x77CCFF;
        };
        var handPos = player.getEyePosition().add(player.getLookAngle().scale(0.55)).subtract(0, 0.25, 0);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ClientJutsuEffectPacket(
                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "hand_seal_spark"),
                handPos,
                player.getLookAngle(),
                1.0f,
                colorHex
        ));
    }
}
