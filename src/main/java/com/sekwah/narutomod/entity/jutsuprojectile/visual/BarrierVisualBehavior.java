package com.sekwah.narutomod.entity.jutsuprojectile.visual;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.JutsuCombatHelper;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuProjectile;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
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
 * Handles Water Wall and Earth Wall barrier & projectile collision/extinguish logic.
 */
public class BarrierVisualBehavior implements JutsuVisualBehavior {

    @Override
    public void tickServer(JutsuVisualEffectEntity entity, ServerLevel serverLevel, @Nullable Entity tracked) {
        if (entity.getVisualType() != JutsuVisualEffectEntity.TYPE_WATER_WALL) {
            return;
        }

        // --- SUITON: SUIJINHEKI (Water Encampment Wall & Projectile Dampening) ---
        Player caster = entity.getCasterPlayer();
        if (entity.tickCount % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    entity.getX(), entity.getY() + 1.1D, entity.getZ(),
                    12, 1.35D, 0.75D, 1.35D, 0.08D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }

        AABB barrierBox = entity.getWaterWallBarrierBox();
        List<Entity> incoming = serverLevel.getEntities(entity, barrierBox.inflate(4.0D),
                e -> e.isAlive()
                        && !(e instanceof JutsuVisualEffectEntity)
                        && (e instanceof Projectile || e instanceof JutsuProjectile));
        for (Entity other : incoming) {
            AABB sweptBox = other.getBoundingBox().expandTowards(other.getDeltaMovement()).inflate(0.5D);
            if (!barrierBox.intersects(other.getBoundingBox()) && !barrierBox.intersects(sweptBox)) {
                continue;
            }
            if (other instanceof FireballJutsuEntity fireball) {
                entity.extinguishFireballWithWaterWall(serverLevel, fireball);
                continue;
            }
            if (other instanceof JutsuProjectile jutsuProj && jutsuProj.getElement() == ChakraElement.FIRE) {
                jutsuProj.explodeAndDiscard();
                continue;
            }
            if (other instanceof Projectile projectile && projectile.isOnFire()) {
                projectile.clearFire();
                serverLevel.playSound(null, projectile.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.1F, 1.1F);
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        projectile.getX(), projectile.getY() + 0.3D, projectile.getZ(),
                        12, 0.4D, 0.4D, 0.4D, 0.05D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            }
            boolean ownedByAlly = entity.isOwnedByAlly(other)
                    || (caster != null && other instanceof Projectile p
                            && (p.getOwner() == caster || ShadowCloneEntity.areAllies(caster, p.getOwner())));
            if (ownedByAlly) {
                continue;
            }
            boolean hadHorizontalMotion = other.getDeltaMovement().horizontalDistanceSqr() > 0.01D;
            if (other instanceof JutsuProjectile jutsuProj) {
                other.setDeltaMovement(0.0D, -0.25D, 0.0D);
                other.setNoGravity(false);
                other.needsSync = true;
                if (hadHorizontalMotion) {
                    serverLevel.playSound(null, other.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 1.1F);
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            other.getX(), other.getY() + 0.3D, other.getZ(),
                            10, 0.35D, 0.35D, 0.35D, 0.08D,
                            ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                }
                jutsuProj.weaken(35);
            } else if (other instanceof Projectile projectile) {
                projectile.setDeltaMovement(0.0D, -0.25D, 0.0D);
                projectile.setNoGravity(false);
                projectile.needsSync = true;
                if (hadHorizontalMotion) {
                    serverLevel.playSound(null, projectile.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.85F, 1.2F);
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            projectile.getX(), projectile.getY() + 0.2D, projectile.getZ(),
                            8, 0.3D, 0.3D, 0.3D, 0.06D,
                            ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                }
            }
        }

        if (entity.tickCount % 6 == 0) {
            float pushDmg = entity.tickCount == 6 ? Math.max(4.0F, entity.getEffectDamage() * 0.5F) : 1.5F;
            List<LivingEntity> pushed = JutsuCombatHelper.findHostileTargetsInRadius(
                    serverLevel, caster, entity.position(), 3.0D, 2.2D);
            DamageSource source = JutsuCombatHelper.playerOrMagicDamage(serverLevel, caster);
            for (LivingEntity target : pushed) {
                if (target.isOnFire()) {
                    target.clearFire();
                }
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false));
                if (entity.tickCount == 6 || entity.tickCount % 18 == 0) {
                    target.hurtServer(serverLevel, source, pushDmg);
                }
                Vec3 away = target.position().subtract(caster != null ? caster.position() : entity.position());
                double horiz = Math.hypot(away.x, away.z);
                if (horiz > 1.0E-4D) {
                    JutsuCombatHelper.applyVelocity(target, new Vec3((away.x / horiz) * 0.65D, 0.20D, (away.z / horiz) * 0.65D), false);
                }
            }
        }
    }
}
