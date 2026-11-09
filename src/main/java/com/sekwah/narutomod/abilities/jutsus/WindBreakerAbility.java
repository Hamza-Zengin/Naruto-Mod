package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Fuuton: Great Breakthrough (Daitoppa)
 * Blasts a high power gust of wind forward, deflecting projectiles and blowing enemies away.
 */
public class WindBreakerAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 231;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < 25) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(25, 25);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 2.0f, 0.7f);

        // Launch 3D Expanding Sonic Wind Funnel & Blade Rings
        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity windFunnel =
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                        serverLevel, player.getX() + look.x * 1.4, player.getY(), player.getZ() + look.z * 1.4,
                        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_WIND_SHOCKWAVE,
                        22, player.getYRot());
        windFunnel.setDeltaMovement(look.scale(0.60));
        serverLevel.addFreshEntity(windFunnel);

        for (int i = 1; i <= 10; i++) {
            Vec3 pos = player.position().add(look.scale(i)).add(0, player.getEyeHeight() * 0.7, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 15, 0.5 * (i * 0.2), 0.5 * (i * 0.2), 0.5 * (i * 0.2), 0.15);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 2, 0.3, 0.3, 0.3, 0.05);

            AABB box = new AABB(pos.x - (1.0 + i * 0.2), pos.y - 1.5, pos.z - (1.0 + i * 0.2),
                    pos.x + (1.0 + i * 0.2), pos.y + 1.5, pos.z + (1.0 + i * 0.2));

            List<Entity> entities = serverLevel.getEntities(player, box);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    living.hurt(player.damageSources().playerAttack(player), 8.0f);
                    living.setDeltaMovement(look.scale(2.2).add(0, 0.5, 0));
                    living.hurtMarked = true;
                } else if (entity instanceof Projectile projectile) {
                    projectile.setDeltaMovement(look.scale(2.0));
                }
            }
        }
    }

    @Override
    public int getCooldown() {
        return 6 * 20;
    }
}
