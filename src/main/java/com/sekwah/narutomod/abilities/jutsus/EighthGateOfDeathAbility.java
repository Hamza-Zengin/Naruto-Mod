package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Eighth Gate of Death (Hachimon Tonkō: Shinomon - 死門)
 * The ultimate forbidden taijutsu technique.
 * Puncturing the heart releases all limiters, granting transcendent physical power (surpassing the Five Kage).
 * 
 * IRREVERSIBLE FATE:
 * Once unsealed, the Gate of Death CANNOT be toggled off!
 * Stamina depletes relentlessly; when stamina reaches 0, the body incinerates from crimson blood steam,
 * inflicting lethal direct damage until death claims the user (except in Creative or Spectator mode).
 */
public class EighthGateOfDeathAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    public static final int REQUIRED_MASTERY = 5000; // Requires mastery of Gate 7 (Kyōmon)

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public boolean requiresScroll() {
        return true;
    }

    @Override
    public List<Identifier> getPrerequisites() {
        return List.of(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "eight_gates"));
    }

    @Override
    public int getRequiredMastery() {
        return REQUIRED_MASTERY;
    }

    @Override
    public boolean canActivate(Player player, INinjaData ninjaData) {
        if (!super.canActivate(player, ninjaData)) return false;
        if (player != null && player.isSpectator()) {
            return false;
        }
        if (ninjaData == null) return false;
        if (!meetsPrerequisites(ninjaData)) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("jutsu.eight_gates_death.requires_gate7")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (player.isSpectator()) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }

        // Heavy stamina consumption: 0.30f per tick (6 stamina/second)
        if (ninjaData.getStamina() > 0.30f) {
            ninjaData.useStamina(0.30f, 30);
        } else {
            ninjaData.setStamina(0);
        }
        return true; // Once open, it remains open until death
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (player.isSpectator()) {
            return;
        }

        // Transcendent Taijutsu Buffs
        ensureSustainedEffect(player, MobEffects.SPEED, 40, 4); // Speed V
        ensureSustainedEffect(player, MobEffects.STRENGTH, 40, 7); // Strength VIII
        ensureSustainedEffect(player, MobEffects.RESISTANCE, 40, 2); // Resistance III
        ensureSustainedEffect(player, MobEffects.JUMP_BOOST, 40, 3); // Jump IV
        ensureSustainedEffect(player, MobEffects.HASTE, 40, 3);

        // Anti-AFK mastery progression for 8th gate
        if (player.tickCount % 200 == 0) {
            boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
            boolean inCombat = player.getLastHurtByMob() != null && (player.tickCount - player.getLastHurtByMobTimestamp() < 100);
            if (isMoving || inCombat) {
                Identifier reg = this.getRegistryName();
                if (reg != null) {
                    ninjaData.addMastery(reg, 1);
                }
            }
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            // Heart ignition on tick 1
            if (ticksActive == 1) {
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 0.6f);
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.8f);
                player.sendSystemMessage(Component.translatable("jutsu.eight_gates_death.activated")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }

            // Crimson Blood Steam: pulsing crimson spores and flames
            if (ticksActive % 10 == 0) {
                serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                        player.getX(), player.getY() + 0.9, player.getZ(),
                        3, 0.25, 0.35, 0.25, 0.02, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            }

            // Stamina exhausted: fatal thermal incineration begins
            if (ninjaData.getStamina() <= 0.01f && !player.isCreative()) {
                ninjaData.setShimonPenaltyActive(true);
                if (ticksActive % 20 == 0) {
                    player.sendOverlayMessage(Component.translatable("jutsu.eight_gates_death.hp_drain")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    player.hurtServer(serverLevel, serverLevel.damageSources().magic(), 7.0f);
                    serverLevel.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.2f, 0.7f);
                    serverLevel.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.5f);
                }
            }
        }
    }

    private static void ensureSustainedEffect(Player player, Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing == null || existing.getAmplifier() < amplifier
                || existing.getDuration() <= 20 || player.tickCount % 10 == 0) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, false));
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        if (!player.level().isClientSide()) {
            player.removeEffect(MobEffects.SPEED);
            player.removeEffect(MobEffects.STRENGTH);
            player.removeEffect(MobEffects.RESISTANCE);
            player.removeEffect(MobEffects.JUMP_BOOST);
            player.removeEffect(MobEffects.HASTE);

            // Once the Gate of Death is closed or extinguishes: inevitable fatal decay
            if (!player.isCreative() && !player.isSpectator()) {
                ninjaData.setShimonPenaltyActive(true);
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 1200, 2, false, true, true)); // Fatal decay
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, 3));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 3));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 2));

                player.level().playSound(null, player.blockPosition(), SoundEvents.SKELETON_DEATH, SoundSource.PLAYERS, 1.5f, 0.6f);
                player.sendSystemMessage(Component.translatable("jutsu.eight_gates_death.inevitable_death")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
        }
    }
}
