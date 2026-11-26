package com.sekwah.narutomod.entity.jutsuprojectile.visual;

import com.sekwah.narutomod.abilities.JutsuCombatHelper;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Handles area, DoT, and projectile-like visual effects:
 * Amaterasu Pillar, Kirin Dragon, Bone Forest, Stone Spikes, Water Vortex, Wind Shockwave, and Wood Prison.
 */
public class AreaEffectVisualBehavior implements JutsuVisualBehavior {

    private static final BlockParticleOption DRIPSTONE_PARTICLE =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.POINTED_DRIPSTONE.defaultBlockState());

    @Override
    public void tickServer(JutsuVisualEffectEntity entity, ServerLevel serverLevel, @Nullable Entity tracked) {
        switch (entity.getVisualType()) {
            case JutsuVisualEffectEntity.TYPE_WIND_SHOCKWAVE -> tickWindShockwave(entity, serverLevel);
            case JutsuVisualEffectEntity.TYPE_BONE_FOREST -> tickBoneForest(entity, serverLevel);
            case JutsuVisualEffectEntity.TYPE_AMATERASU_PILLAR -> tickAmaterasuPillar(entity, serverLevel);
            case JutsuVisualEffectEntity.TYPE_KIRIN_DRAGON -> tickKirinDragon(entity, serverLevel);
            case JutsuVisualEffectEntity.TYPE_STONE_SPIKES -> tickStoneSpikes(entity, serverLevel);
            case JutsuVisualEffectEntity.TYPE_WATER_VORTEX, JutsuVisualEffectEntity.TYPE_WOOD_PRISON -> {
                // Visual-only server lifetime; physical/area logic handled by dedicated entities or lifetime expiration.
            }
        }
    }

    private static void tickWindShockwave(JutsuVisualEffectEntity entity, ServerLevel serverLevel) {
        Player caster = entity.getCasterPlayer();
        float progress = (float) entity.tickCount / (float) Math.max(1, entity.getMaxLife());
        double radius = 1.2D + progress * 1.8D;
        AABB gustBox = entity.getBoundingBox().inflate(radius, 1.2D, radius);

        DamageSource gustDamage = caster != null
                ? serverLevel.damageSources().playerAttack(caster)
                : serverLevel.damageSources().generic();
        Vec3 push = entity.getDeltaMovement().normalize().scale(2.2D).add(0, 0.45D, 0);
        for (LivingEntity living : JutsuCombatHelper.findHostileTargetsInBox(serverLevel, caster, gustBox)) {
            living.hurtServer(serverLevel, gustDamage, 10.0F);
            JutsuCombatHelper.applyVelocity(living, push, true);
        }

        List<Projectile> projectiles = serverLevel.getEntitiesOfClass(Projectile.class, gustBox, Projectile::isAlive);
        for (Projectile projectile : projectiles) {
            if (caster != null && (projectile.getOwner() == caster || ShadowCloneEntity.areAllies(caster, projectile.getOwner()))) {
                if (projectile instanceof FireballJutsuEntity fireball) {
                    entity.boostAlliedFireball(fireball);
                }
                continue;
            }
            projectile.setDeltaMovement(entity.getDeltaMovement().scale(1.8D));
            projectile.needsSync = true;
        }
    }

    private static void tickBoneForest(JutsuVisualEffectEntity entity, ServerLevel serverLevel) {
        if (entity.tickCount <= 22 && entity.tickCount % 7 == 0) {
            Player caster = entity.getCasterPlayer();
            double currentRadius = (entity.tickCount / 7) * 2.2D + 1.2D;
            AABB ringBox = entity.getBoundingBox().inflate(currentRadius, 1.5D, currentRadius);
            List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, ringBox,
                    e -> e.isAlive() && (caster == null || (e != caster && !ShadowCloneEntity.areAllies(caster, e))));
            for (LivingEntity v : victims) {
                v.hurtServer(serverLevel, caster != null ? serverLevel.damageSources().playerAttack(caster) : serverLevel.damageSources().generic(), 7.0F);
                v.setDeltaMovement(v.getDeltaMovement().x, 0.48D, v.getDeltaMovement().z);
                v.needsSync = true;
                v.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1));
            }
        }
    }

    private static void tickAmaterasuPillar(JutsuVisualEffectEntity entity, ServerLevel serverLevel) {
        if (entity.tickCount % 10 == 0) {
            Player caster = entity.getCasterPlayer();
            AABB flameBox = entity.getBoundingBox().inflate(1.8D, 1.2D, 1.8D);
            List<LivingEntity> burning = serverLevel.getEntitiesOfClass(LivingEntity.class, flameBox,
                    e -> e.isAlive() && (caster == null || (e != caster && !ShadowCloneEntity.areAllies(caster, e))));
            for (LivingEntity b : burning) {
                b.hurtServer(serverLevel, caster != null ? serverLevel.damageSources().playerAttack(caster) : serverLevel.damageSources().inFire(), 5.0F);
                b.setRemainingFireTicks(240);
                b.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2));
            }
        }
    }

    private static void tickKirinDragon(JutsuVisualEffectEntity entity, ServerLevel serverLevel) {
        if (entity.tickCount == 6) {
            Player caster = entity.getCasterPlayer();
            // Ground slam moment
            serverLevel.playSound(null, entity.blockPosition(), NarutoSounds.KIRIN_STRIKE.get(), SoundSource.WEATHER, 3.5F, 0.85F);
            serverLevel.playSound(null, entity.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3.5F, 0.75F);
            serverLevel.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6F, 1.0F);

            AABB slamBox = entity.getBoundingBox().inflate(6.5D, 3.0D, 6.5D);
            List<LivingEntity> struck = serverLevel.getEntitiesOfClass(LivingEntity.class, slamBox,
                    e -> e.isAlive() && (caster == null || (e != caster && !ShadowCloneEntity.areAllies(caster, e))));
            DamageSource kirinDamage = entity.createKirinLightningDamage(serverLevel, caster);
            for (LivingEntity s : struck) {
                s.hurtServer(serverLevel, kirinDamage, 32.0F);
                double dx = s.getX() - entity.getX();
                double dz = s.getZ() - entity.getZ();
                double distSq = dx * dx + dz * dz;
                if (distSq > 1.0E-4D) {
                    double dist = Math.sqrt(distSq);
                    double factor = 2.2D / dist;
                    s.push(dx * factor, 0.75D, dz * factor);
                } else {
                    s.push(0, 0.75D, 0);
                }
                s.setRemainingFireTicks(100);
                s.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
            }
        }
    }

    private static void tickStoneSpikes(JutsuVisualEffectEntity entity, ServerLevel serverLevel) {
        if (entity.tickCount == 2) {
            serverLevel.playSound(null, entity.blockPosition(), NarutoSounds.MUD_WALL_RISE.get(), SoundSource.PLAYERS, 1.5F, 0.85F);
            serverLevel.playSound(null, entity.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.8F, 0.7F);
            serverLevel.sendParticles(DRIPSTONE_PARTICLE,
                    entity.getX(), entity.getY() + 0.6D, entity.getZ(),
                    36, 1.6D, 0.8D, 1.6D, 0.16D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
        if (entity.tickCount == 3 || (entity.tickCount >= 12 && entity.tickCount <= entity.getMaxLife() - 8 && entity.tickCount % 12 == 0)) {
            Player caster = entity.getCasterPlayer();
            float baseDmg = Math.max(4.0F, entity.getEffectDamage());
            float hitDmg = entity.tickCount == 3 ? baseDmg * 0.70F : baseDmg * 0.35F;
            AABB spikeBox = entity.getBoundingBox().inflate(3.0D, 2.2D, 3.0D);
            List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, spikeBox,
                    e -> e.isAlive() && (caster == null || (!e.equals(caster) && !ShadowCloneEntity.areAllies(caster, e))));
            for (LivingEntity v : victims) {
                v.hurtServer(serverLevel, caster != null ? serverLevel.damageSources().playerAttack(caster) : serverLevel.damageSources().generic(), hitDmg);
                if (entity.tickCount == 3) {
                    v.setDeltaMovement(v.getDeltaMovement().x * 0.5D, 0.68D, v.getDeltaMovement().z * 0.5D);
                    v.needsSync = true;
                    v.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
                } else {
                    v.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                }
            }
        }
    }
}
