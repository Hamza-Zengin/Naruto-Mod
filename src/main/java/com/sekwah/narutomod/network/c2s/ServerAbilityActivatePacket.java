package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.function.Supplier;

/**
 * Tell the server that the user wants to cast a specific ability.
 */
public class ServerAbilityActivatePacket {

    private final ResourceLocation abilityId;

    public ServerAbilityActivatePacket(ResourceLocation ability) {
        this.abilityId = ability;
    }

    public static void encode(ServerAbilityActivatePacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeResourceLocation(msg.abilityId);
    }

    public static ServerAbilityActivatePacket decode(FriendlyByteBuf inBuffer) {
        ResourceLocation abilityId = inBuffer.readResourceLocation();
        return new ServerAbilityActivatePacket(abilityId);
    }

    public static class Handler {
        public static void handle(ServerAbilityActivatePacket msg, Supplier<NetworkEvent.Context> ctx) {
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

                    if (ability.activationType() == Ability.ActivationType.INSTANT) {
                        if (!ability.canActivate(player, ninjaData)) {
                            return;
                        }

                        boolean canTriggerJutsu = true;
                        if (ability instanceof Ability.Cooldown cooldownAbility) {
                            canTriggerJutsu = !cooldownAbility.checkCooldown(player, ninjaData, ability.getTranslationKey());
                        }

                        boolean comboMatched = ability.matchesExpectedSeals(ninjaData.getActiveSeals());
                        if(canTriggerJutsu && ability.handleCost(player, ninjaData)) {
                            if (ability.logInChat()) {
                                player.sendSystemMessage(Component.translatable("jutsu.cast", Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));
                                if (comboMatched) {
                                    player.sendSystemMessage(Component.translatable("jutsu.combo.perfect", Component.translatable(ability.getTranslationKey())).withStyle(ChatFormatting.GOLD));
                                }
                            }
                            int delay = (ability.requiresSealActivationDelay() && comboMatched) ? ability.getActivationDelayTicks(ninjaData) : 0;
                            if (delay > 0) {
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4f, 1.2f);
                                ninjaData.scheduleDelayedTickEvent(p -> {
                                    if(ability.castingSound() != null) {
                                        p.level().playSound(null,
                                                p.getX(), p.getY(), p.getZ(), ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                                    }
                                    ability.performServer(p, ninjaData);
                                }, delay);
                            } else {
                                if(ability.castingSound() != null) {
                                    player.level().playSound(null,
                                            player.getX(), player.getY(), player.getZ(), ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                                }
                                ability.performServer(player, ninjaData);
                            }
                            ninjaData.addJutsuCast();
                            ninjaData.clearSeals();

                            if (ability instanceof Ability.Cooldown cooldownAbility) {
                               cooldownAbility.registerCooldown(ninjaData, ability.getTranslationKey());
                            }
                        }
                    } else if(ability.activationType() == Ability.ActivationType.TOGGLE) {
                        ToggleAbilityData abilityTracker = ninjaData.getToggleAbilityData();
                        HashSet<ResourceLocation> abilities = abilityTracker.getAbilitiesHashSet();
                        if(abilities.contains(ability.getRegistryName())) {
                            if (NarutoAbilities.EIGHT_GATES_DEATH.getId().equals(ability.getRegistryName())) {
                                player.sendSystemMessage(Component.translatable("jutsu.eight_gates_death.cannot_close")
                                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                                return;
                            }
                            // Toggle ability off
                            abilityTracker.removeAbilityEnded(player, ninjaData, ability);
                        } else {
                            if (!ability.canActivate(player, ninjaData)) {
                                return;
                            }
                            // Toggle ability on
                            abilityTracker.addAbilityStarted(player, ninjaData, ability);
                            ninjaData.addJutsuCast();
                        }
                    }
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
