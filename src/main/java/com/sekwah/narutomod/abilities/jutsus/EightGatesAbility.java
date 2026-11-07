package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Eight Gates (Hachimon Tonkō - 八門遁甲: Gate of Death / 死門)
 * The ultimate forbidden taijutsu that breaks all human limiters.
 * Grants godlike speed, jumps, and destructive physical strength (3x Multipliers).
 * 
 * IRREVERSIBLE FATE:
 * Once unsealed, the Gate of Death CANNOT be toggled off!
 * Stamina depletes steadily; once stamina hits 0, the body burns into ash,
 * draining the user's HP relentlessly until death!
 */
public class EightGatesAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.CHUNIN;
    }

    @Override
    public long defaultCombo() {
        return 233; // V - B - B
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // 3x Godlike Taijutsu Buffs
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 4, false, false, true)); // Speed V
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 3, false, false, false)); // Jump IV
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 6, false, false, true)); // Strength VII (+28 damage)
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false, false)); // Resistance II (40% dmg reduction)
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 2, false, false, false)); // Haste III

        // Balanced stamina consumption: 0.10f per tick (~2 stamina per second)
        // Allows a glorious 30-50 second combat frenzy before the death toll begins
        if (ninjaData.getStamina() > 0.10f) {
            ninjaData.useStamina(0.10f, 30);
        } else {
            ninjaData.setStamina(0);
            // STAMINA EXHAUSTED: Body begins burning into ash!
            if (ticksActive % 20 == 0 && player.level() instanceof ServerLevel serverLevel) {
                player.displayClientMessage(Component.translatable("jutsu.eight_gates.hp_drain").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
                
                // Relentless 3x death toll: 4.5 HP (2.25 full hearts) direct unblockable damage every second!
                player.hurt(serverLevel.damageSources().magic(), 4.5f);

                serverLevel.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.2f, 0.7f);
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.5f);

                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        6, 0.2, 0.2, 0.2, 0.05);
                serverLevel.sendParticles(ParticleTypes.LAVA,
                        player.getX(), player.getY() + 0.8, player.getZ(),
                        5, 0.3, 0.4, 0.3, 0.02);
            }
        }

        // Fierce Red and Crimson Steam bursting from every pore
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 0.8, player.getZ(),
                    4, 0.3, 0.5, 0.3, 0.08);
            if (ticksActive % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                        player.getX(), player.getY() + 0.2, player.getZ(),
                        3, 0.2, 0.4, 0.2, 0.02);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        2, 0.25, 0.3, 0.25, 0.01);
            }
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        if (!player.level().isClientSide()) {
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
            player.removeEffect(MobEffects.DAMAGE_BOOST);
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);

            // Once the Gate of Death is opened, death is inevitable!
            // Even if ended, lethal necrosis and fatal decay continues until the end
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 1200, 2, false, true, true)); // Fatal decay
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 3)); // Slowness IV (nearly immobilized)
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 3)); // Weakness IV
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 2));

            player.level().playSound(null, player.blockPosition(), SoundEvents.SKELETON_DEATH, SoundSource.PLAYERS, 1.5f, 0.6f);
            player.displayClientMessage(Component.translatable("jutsu.eight_gates.inevitable_death")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        }
    }
}
