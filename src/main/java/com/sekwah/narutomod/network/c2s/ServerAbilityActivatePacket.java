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
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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

    public static final int[] HAND_SEAL_DELAY_TICKS = {0, 2, 4};
    public static final float[] HAND_SEAL_PITCHES = {0.95f, 1.05f, 1.15f};
    public static final float HAND_SEAL_VOLUME = 0.85f;

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

    /**
     * Plays an anime-style rapid 3-seal hand-seal sequence (SEAL_A -> SEAL_B -> SEAL_C at 0/2/4 ticks)
     * around the casting player.
     */
    public static void playQuickHandSealSequence(ServerPlayer player, NinjaData ninjaData) {
        if (player == null || ninjaData == null || !ServerSecurityValidator.validatePlayerState(player)) {
            return;
        }
        SoundEvent[] seals = {
                NarutoSounds.SEAL_A.get(),
                NarutoSounds.SEAL_B.get(),
                NarutoSounds.SEAL_C.get()
        };
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                seals[0], SoundSource.PLAYERS, HAND_SEAL_VOLUME, HAND_SEAL_PITCHES[0]);
        for (int i = 1; i < seals.length; i++) {
            final SoundEvent sealSound = seals[i];
            final float pitch = HAND_SEAL_PITCHES[i];
            ninjaData.scheduleDelayedTickEvent(p -> {
                if (p instanceof ServerPlayer sp && !ServerSecurityValidator.validatePlayerState(sp)) {
                    return;
                }
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                        sealSound, SoundSource.PLAYERS, HAND_SEAL_VOLUME, pitch);
            }, HAND_SEAL_DELAY_TICKS[i]);
        }
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

                if (ability.handleCost(player, ninjaData)) {
                    if (ability.logInChat()) {
                        player.sendSystemMessage(Component.translatable("jutsu.cast", Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
                    }
                    if (ability.playsHandSealSounds()) {
                        playQuickHandSealSequence(player, ninjaData);
                    }
                    int rawDelay = ability.requiresActivationDelay() ? ability.getActivationDelayTicks(ninjaData) : 0;
                    int delay = Ability.applyArmorDelayReduction(player, rawDelay);
                    if (delay > 0) {
                        ninjaData.scheduleDelayedTickEvent(p -> {
                            if (p instanceof ServerPlayer sp && !ServerSecurityValidator.validatePlayerState(sp)) {
                                return;
                            }
                            if (ability.castingSound() != null) {
                                p.level().playSound(null,
                                        p.getX(), p.getY(), p.getZ(), ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                            }
                            ability.perform(p, ninjaData);
                            if (p instanceof ServerPlayer sp) {
                                sendHandSealSpark(sp, ability);
                            }
                        }, delay);
                    } else {
                        if (ability.castingSound() != null) {
                            player.level().playSound(null,
                                    player.getX(), player.getY(), player.getZ(), ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                        }
                        ability.perform(player, ninjaData);
                        sendHandSealSpark(player, ability);
                    }
                    ninjaData.addJutsuCast();
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
                    if (ability.playsHandSealSounds()) {
                        playQuickHandSealSequence(player, ninjaData);
                    }
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
