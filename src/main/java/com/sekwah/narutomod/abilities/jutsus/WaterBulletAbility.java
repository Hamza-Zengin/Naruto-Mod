package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
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
 * Suiton: Water Bullet Jutsu
 * Blasts concentrated torrents of water forward.
 */
public class WaterBulletAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 312;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        return tryConsume(player, ninjaData, 25.0f, 0, 25);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5f, 1.0f);

        // Launch 3D Surging Water Shark / Hydro Vortex Torpedo
        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity waterVortex =
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                        serverLevel, player.getX() + look.x * 1.2, player.getY(), player.getZ() + look.z * 1.2,
                        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_WATER_VORTEX,
                        20, player.getYRot());
        waterVortex.setDeltaMovement(look.scale(0.65));
        serverLevel.addFreshEntity(waterVortex);

        for (int i = 1; i <= 8; i++) {
            Vec3 pos = player.position().add(look.scale(i)).add(0, player.getEyeHeight() * 0.7, 0);
            serverLevel.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, 20, 0.4, 0.4, 0.4, 0.1);
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER, pos.x, pos.y, pos.z, 10, 0.3, 0.3, 0.3, 0.05);

            AABB box = new AABB(pos.x - 1.2, pos.y - 1.2, pos.z - 1.2, pos.x + 1.2, pos.y + 1.2, pos.z + 1.2);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
            for (LivingEntity target : targets) {
                target.hurt(player.damageSources().drown(), 12.0f);
                target.clearFire();
                target.setDeltaMovement(look.scale(1.5).add(0, 0.3, 0));
                target.hurtMarked = true;
            }
        }
    }

    @Override
    public int getCooldown() {
        return 5 * 20;
    }
}
