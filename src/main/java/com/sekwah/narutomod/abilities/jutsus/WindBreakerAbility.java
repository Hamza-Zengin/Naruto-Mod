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
    public com.sekwah.narutomod.abilities.ChakraElement getChakraElement() {
        return com.sekwah.narutomod.abilities.ChakraElement.WIND;
    }

    @Override
    public long defaultCombo() {
        return 231;
    }

    @Override
    public float getDefaultDamage() {
        return 8.0f;
    }

    @Override
    public float getDefaultChakraCost() {
        return 25.0f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        return tryConsume(player, ninjaData, getScaledChakraCost(ninjaData), 0, 25);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float powerMult = getPowerMultiplier(mastery);
        float damage = this.getDamage(ninjaData);
        int maxRange = 10 + (getMasteryTier(mastery) >= 4 ? 3 : 0);

        Vec3 look = player.getLookAngle();
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 2.0f, 0.7f);

        // Launch 3D Expanding Sonic Wind Funnel & Blade Rings
        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity windFunnel =
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                        serverLevel, player.getX() + look.x * 1.4, player.getY(), player.getZ() + look.z * 1.4,
                        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_WIND_SHOCKWAVE,
                        22, player.getYRot());
        windFunnel.setDeltaMovement(look.scale(0.60 * Math.min(1.3, powerMult)));
        serverLevel.addFreshEntity(windFunnel);

        for (int i = 1; i <= maxRange; i++) {
            Vec3 pos = player.position().add(look.scale(i)).add(0, player.getEyeHeight() * 0.7, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 15, 0.5 * (i * 0.2), 0.5 * (i * 0.2), 0.5 * (i * 0.2), 0.15);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 2, 0.3, 0.3, 0.3, 0.05);

            AABB box = new AABB(pos.x - (1.0 + i * 0.2), pos.y - 1.5, pos.z - (1.0 + i * 0.2),
                    pos.x + (1.0 + i * 0.2), pos.y + 1.5, pos.z + (1.0 + i * 0.2));

            List<Entity> entities = serverLevel.getEntities(player, box);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living && living.isAlive()
                        && !com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(player, living)) {
                    living.hurt(player.damageSources().playerAttack(player), damage);
                    living.setDeltaMovement(look.scale(2.2 * powerMult).add(0, 0.5 * powerMult, 0));
                    living.hurtMarked = true;
                } else if (entity instanceof Projectile projectile) {
                    projectile.setDeltaMovement(look.scale(2.0 * powerMult));
                }
            }
        }
    }

    @Override
    public int getCooldown() {
        return 6 * 20;
    }
}
