package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Rasengan (Spiralling Sphere)
 * High density swirling chakra ball that deals massive knockback and damage.
 */
public class RasenganAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 112;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < 40) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(40, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        Vec3 forwardPos = player.position().add(look.scale(1.8)).add(0, player.getEyeHeight() * 0.7, 0);

        // Visual swirl effect
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, forwardPos.x, forwardPos.y, forwardPos.z, 5, 0.4, 0.4, 0.4, 0.05);
        serverLevel.sendParticles(ParticleTypes.CLOUD, forwardPos.x, forwardPos.y, forwardPos.z, 20, 0.3, 0.3, 0.3, 0.08);

        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.RASENGAN.get(), SoundSource.PLAYERS, 1.2f, 1.0f);

        // Check for targets in front
        AABB hitBox = new AABB(forwardPos.x - 2.0, forwardPos.y - 1.5, forwardPos.z - 2.0,
                forwardPos.x + 2.0, forwardPos.y + 1.5, forwardPos.z + 2.0);

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            // Apply damage
            target.hurt(player.damageSources().playerAttack(player), 18.0f);

            // Massive knockback
            Vec3 knockback = look.scale(2.5).add(0, 0.6, 0);
            target.setDeltaMovement(knockback);
            target.hurtMarked = true;

            serverLevel.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.0, target.getZ(), 2, 0, 0, 0, 0);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }

    @Override
    public int getCooldown() {
        return 8 * 20;
    }
}
