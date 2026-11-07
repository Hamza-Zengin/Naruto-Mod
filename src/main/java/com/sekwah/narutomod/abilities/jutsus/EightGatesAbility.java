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
 * Eight Gates (Hachimon Tonkō - 八門遁甲)
 * Forbidden Taijutsu removing mental and physical limits on chakra flow.
 * Grants super speed, jump, and extreme strength.
 * Consumes stamina continuously. When stamina runs out, begins draining user's HP directly!
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
        // Gates can be opened even with low stamina, but player will suffer self-damage once stamina is gone
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // High speed and supreme physical strength
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 2, false, false, true)); // Speed III
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, 1, false, false, false)); // Jump II
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 2, false, false, true)); // Strength III (+9-12 dmg)
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 30, 1, false, false, false)); // Haste II

        // Stamina cost: 0.15f per tick (~3 per second)
        if (ninjaData.getStamina() > 0.15f) {
            ninjaData.useStamina(0.15f, 30);
        } else {
            ninjaData.setStamina(0);
            // Stamina depleted! The forbidden technique begins consuming the user's life force!
            if (ticksActive % 20 == 0 && player.level() instanceof ServerLevel serverLevel) {
                player.displayClientMessage(Component.translatable("jutsu.eight_gates.hp_drain").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
                player.hurt(serverLevel.damageSources().magic(), 1.5f); // 1.5 HP damage every second

                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        4, 0.2, 0.2, 0.2, 0.05);
            }
        }

        // Green/Red steam particle effects bursting from body
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 0.8, player.getZ(),
                    3, 0.3, 0.5, 0.3, 0.08);
            if (ticksActive % 5 == 0) {
                serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                        player.getX(), player.getY() + 0.2, player.getZ(),
                        2, 0.2, 0.3, 0.2, 0.02);
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

            // Gate exhaustion backlash
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 160, 1));

            player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.2f, 0.8f);
            player.displayClientMessage(Component.translatable("jutsu.eight_gates.exhaustion").withStyle(ChatFormatting.DARK_RED), false);
        }
    }
}
