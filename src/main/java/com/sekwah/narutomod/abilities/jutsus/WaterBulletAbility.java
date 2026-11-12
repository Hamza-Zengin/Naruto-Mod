package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Suiton: Water Bullet Jutsu
 * Blasts concentrated torrents of water forward.
 */
public class WaterBulletAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public com.sekwah.narutomod.abilities.ChakraElement getChakraElement() {
        return com.sekwah.narutomod.abilities.ChakraElement.WATER;
    }

    @Override
    public long defaultCombo() {
        return 312;
    }

    @Override
    public float getDefaultChakraCost() {
        return 25.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 5 * 20;
    }

    @Override
    public float getDefaultDamage() {
        return 12.0f;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5f, 1.0f);

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float powerMult = getPowerMultiplier(mastery);
        float bulletDamage = this.getDamage(ninjaData);

        // Spawn physical WaterBulletEntity projectile
        WaterBulletEntity waterBullet = new WaterBulletEntity(serverLevel, player);
        waterBullet.setPos(player.getX() + look.x * 1.2, player.getY() + player.getEyeHeight() * 0.6, player.getZ() + look.z * 1.2);
        waterBullet.setYRot(player.getYRot());
        waterBullet.setXRot(player.getXRot());
        waterBullet.setDamage(bulletDamage);
        waterBullet.setMaxLife(25);
        waterBullet.setDeltaMovement(look.scale(1.1 * Math.min(1.4, powerMult)));
        serverLevel.addFreshEntity(waterBullet);

        for (int i = 1; i <= 8; i++) {
            Vec3 pos = player.position().add(look.scale(i)).add(0, player.getEyeHeight() * 0.7, 0);
            serverLevel.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, 20, 0.4, 0.4, 0.4, 0.1);
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER, pos.x, pos.y, pos.z, 10, 0.3, 0.3, 0.3, 0.05);

            AABB box = new AABB(pos.x - 1.2, pos.y - 1.2, pos.z - 1.2, pos.x + 1.2, pos.y + 1.2, pos.z + 1.2);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
            for (LivingEntity target : targets) {
                target.hurt(player.damageSources().drown(), bulletDamage);
                target.clearFire();
                target.setDeltaMovement(look.scale(1.5 * powerMult).add(0, 0.3 * powerMult, 0));
                target.hurtMarked = true;
            }
        }
    }
}
