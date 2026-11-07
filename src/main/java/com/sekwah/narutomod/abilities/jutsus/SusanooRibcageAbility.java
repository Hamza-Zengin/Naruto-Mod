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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Susanoo Ribcage (Susanoo: Abara)
 * Skeletal ribcage of chakra enveloping the user, conferring immense damage resistance and knockback aura.
 */
public class SusanooRibcageAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public long defaultCombo() {
        return 122; // C - V - V
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getClan() == null || !ninjaData.getClan().getPath().equalsIgnoreCase("uchiha")) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notclan",
                    Component.literal("Uchiha").withStyle(ChatFormatting.RED)), true);
            return false;
        }

        if (ninjaData.getChakra() < 0.6f) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }

        ninjaData.useChakra(0.6f, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // High defense buffs
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false, true)); // Resistance III (60% dmg reduction)
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, false));

        if (ticksActive % 20 == 0 && player.level() instanceof ServerLevel serverLevel) {
            // Ethereal flame/aura around player
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI / 12) * i;
                double x = player.getX() + 1.2 * Math.cos(angle);
                double z = player.getZ() + 1.2 * Math.sin(angle);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, player.getY() + 0.8, z, 1, 0, 0.05, 0, 0.02);
                serverLevel.sendParticles(ParticleTypes.ENCHANT, x, player.getY() + 1.5, z, 2, 0.1, 0.1, 0.1, 0.05);
            }

            // Knock back aggressive foes nearby
            AABB auraBox = player.getBoundingBox().inflate(2.5);
            List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, auraBox, e -> e != player && e.isAlive());
            for (LivingEntity enemy : enemies) {
                Vec3 push = enemy.position().subtract(player.position()).normalize().scale(0.8);
                enemy.push(push.x, 0.3, push.z);
                enemy.hurt(serverLevel.damageSources().playerAttack(player), 3.0f);
            }
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        if (!player.level().isClientSide()) {
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }
}
