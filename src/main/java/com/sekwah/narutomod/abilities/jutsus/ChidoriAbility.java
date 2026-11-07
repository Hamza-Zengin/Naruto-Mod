package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Chidori / Raikiri (One Thousand Birds)
 * Lightning-infused thrust that grants high velocity and piercing lightning damage.
 */
public class ChidoriAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 213;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < 45) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(45, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Give high-speed burst effect
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5 * 20, 2, false, false, true));

        // Sound effect
        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.CHIDORI.get(), SoundSource.PLAYERS, 1.2f, 1.0f);

        // Burst in look direction
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * 2.2, 0.2, look.z * 2.2);

        // Visual lightning sparks
        for (int i = 0; i < 30; i++) {
            double ox = (player.getRandom().nextDouble() - 0.5) * 1.5;
            double oy = player.getRandom().nextDouble() * 1.8;
            double oz = (player.getRandom().nextDouble() - 0.5) * 1.5;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX() + ox, player.getY() + oy, player.getZ() + oz,
                    2, 0, 0, 0, 0.2);
        }

        // Deal lightning damage to entities directly in front
        Vec3 forward = player.position().add(look.scale(2.2));
        AABB box = new AABB(forward.x - 1.5, forward.y - 1.0, forward.z - 1.5,
                forward.x + 1.5, forward.y + 2.0, forward.z + 1.5);

        List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
        for (LivingEntity enemy : enemies) {
            enemy.hurt(player.damageSources().lightningBolt(), 22.0f);
            serverLevel.sendParticles(ParticleTypes.FLASH, enemy.getX(), enemy.getY() + 1.0, enemy.getZ(), 1, 0, 0, 0, 0);
        }
    }

    @Override
    public int getCooldown() {
        return 10 * 20;
    }
}
