package com.sekwah.narutomod.entity.jutsuprojectile.visual;

import com.sekwah.narutomod.abilities.JutsuCombatHelper;
import com.sekwah.narutomod.abilities.jutsus.AshPileBurningAbility;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import com.sekwah.narutomod.entity.projectile.ExplosiveKunaiEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Handles Katon: Haisekisho (Ash Pile Burning Cloud) expansion, DoT, and chain ignition detonation.
 */
public class AshCloudVisualBehavior implements JutsuVisualBehavior {

    @Override
    public void tickServer(JutsuVisualEffectEntity entity, ServerLevel serverLevel, @Nullable Entity tracked) {
        final double cloudRadius = AshPileBurningAbility.CLOUD_RADIUS;
        if (entity.tickCount % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    entity.getX(), entity.getY() + 0.9D, entity.getZ(),
                    10, 1.45D, 0.55D, 1.45D, 0.02D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.ASH,
                    entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                    14, 1.6D, 0.7D, 1.6D, 0.01D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }

        // Check for Fire/Explosive ignition triggers every 4 ticks
        if (!entity.isAshDetonated() && entity.tickCount % 4 == 0) {
            AABB cloudBox = entity.getBoundingBox().inflate(cloudRadius, 2.0D, cloudRadius);
            List<Projectile> igniters = serverLevel.getEntitiesOfClass(Projectile.class, cloudBox,
                    p -> p.isAlive() && (p instanceof FireballJutsuEntity
                            || p instanceof ExplosiveKunaiEntity
                            || p.isOnFire()));
            if (!igniters.isEmpty()) {
                entity.detonateAshCloud(serverLevel);
                return;
            }
        }

        // Periodic searing ash tick damage + Blindness + Slowness II
        if (!entity.isAshDetonated() && entity.tickCount % 10 == 0) {
            Player caster = entity.getCasterPlayer();
            float baseDmg = Math.max(6.0F, entity.getEffectDamage());
            float dotDmg = baseDmg * 0.12F;
            List<LivingEntity> victims = JutsuCombatHelper.findHostileTargetsInRadius(
                    serverLevel, caster, entity.position(), cloudRadius, 2.2D);
            DamageSource source = JutsuCombatHelper.playerOrMagicDamage(serverLevel, caster);
            for (LivingEntity victim : victims) {
                victim.hurtServer(serverLevel, source, dotDmg);
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0, false, false));
            }
        }

        // Final auto-detonation right before expiry
        if (!entity.isAshDetonated() && entity.tickCount >= entity.getMaxLife() - 2) {
            entity.detonateAshCloud(serverLevel);
        }
    }

    public static void detonate(JutsuVisualEffectEntity entity, ServerLevel serverLevel) {
        if (entity.isAshDetonated() || entity.getVisualType() != JutsuVisualEffectEntity.TYPE_ASH_CLOUD) {
            return;
        }
        entity.setAshDetonated(true);
        Player caster = entity.getCasterPlayer();
        float baseDmg = Math.max(8.0F, entity.getEffectDamage());
        float burstDmg = baseDmg * 0.75F;
        double radius = AshPileBurningAbility.CLOUD_RADIUS + 0.4D;

        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.7F, 0.9F);
        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.3F, 0.65F);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                entity.getX(), entity.getY() + 0.9D, entity.getZ(),
                2, 0.8D, 0.4D, 0.8D, 0.02D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        serverLevel.sendParticles(ParticleTypes.FLAME,
                entity.getX(), entity.getY() + 0.8D, entity.getZ(),
                48, 1.6D, 0.7D, 1.6D, 0.14D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        List<LivingEntity> victims = JutsuCombatHelper.findHostileTargetsInRadius(
                serverLevel, caster, entity.position(), radius, 2.5D);
        DamageSource source = caster != null ? serverLevel.damageSources().playerAttack(caster) : serverLevel.damageSources().inFire();
        for (LivingEntity victim : victims) {
            victim.hurtServer(serverLevel, source, burstDmg);
            victim.setRemainingFireTicks(100);
            Vec3 away = victim.position().subtract(entity.position());
            double horiz = Math.hypot(away.x, away.z);
            if (horiz > 1.0E-4D) {
                JutsuCombatHelper.applyVelocity(victim, new Vec3((away.x / horiz) * 1.15D, 0.48D, (away.z / horiz) * 1.15D), true);
            } else {
                JutsuCombatHelper.applyVelocity(victim, new Vec3(0.0D, 0.55D, 0.0D), true);
            }
        }
        entity.discard();
    }
}
