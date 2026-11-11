package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
    public com.sekwah.narutomod.capabilities.NinjaRank getRequiredRank() {
        return com.sekwah.narutomod.capabilities.NinjaRank.CHUNIN;
    }

    @Override
    public long defaultCombo() {
        return 112;
    }

    @Override
    public float getDefaultChakraCost() {
        return 40.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 8 * 20;
    }

    @Override
    public float getDefaultDamage() {
        return 22.0f;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Activate 5-second hand holding window (100 ticks)
        ninjaData.setActiveHandJutsu("rasengan", 100);

        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.RASENGAN.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.RASENGAN_LOOP.get(), SoundSource.PLAYERS, 0.9f, 1.0f);
        player.displayClientMessage(Component.translatable("jutsu.rasengan.ready").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
    }

    public static void executeStrike(Player player, INinjaData ninjaData, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        Vec3 strikePos = target != null ? target.position().add(0, target.getBbHeight() * 0.5, 0)
                : player.position().add(look.scale(1.8)).add(0, player.getEyeHeight() * 0.7, 0);

        // Spawn 3D Rasengan Impact Burst Entity
        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity rasengan3D =
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                        serverLevel, strikePos.x, strikePos.y - 0.5, strikePos.z,
                        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_RASENGAN_SPHERE,
                        18, player.getYRot());
        serverLevel.addFreshEntity(rasengan3D);

        // Check for targets if direct target not provided
        List<LivingEntity> hitTargets = new java.util.ArrayList<>();
        if (target != null && target.isAlive()) {
            hitTargets.add(target);
        } else {
            AABB hitBox = new AABB(strikePos.x - 2.0, strikePos.y - 1.5, strikePos.z - 2.0,
                    strikePos.x + 2.0, strikePos.y + 1.5, strikePos.z + 2.0);
            hitTargets.addAll(serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != player && e.isAlive()));
        }

        float strikeDamage = com.sekwah.narutomod.abilities.NarutoAbilities.RASENGAN.get().getDamage();
        for (LivingEntity hit : hitTargets) {
            hit.hurt(player.damageSources().playerAttack(player), strikeDamage);
            Vec3 knockback = look.scale(2.5).add(0, 0.6, 0);
            if (hit instanceof Player targetPlayer) {
                com.sekwah.sekclib.util.PlayerUtil.setVelocity(targetPlayer, knockback, true);
            } else {
                hit.setDeltaMovement(knockback);
                hit.hurtMarked = true;
            }
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, hit.getX(), hit.getY() + 1.0, hit.getZ(), 1, 0, 0, 0, 0);
        }

        serverLevel.playSound(null, BlockPos.containing(strikePos), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 1.1f);
        serverLevel.playSound(null, BlockPos.containing(strikePos), NarutoSounds.RASENGAN.get(), SoundSource.PLAYERS, 1.4f, 1.2f);
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, strikePos.x, strikePos.y, strikePos.z, 8, 0.3, 0.3, 0.3, 0.05);

        ninjaData.clearActiveHandJutsu();
    }
}
