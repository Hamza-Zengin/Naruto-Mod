package com.sekwah.narutomod.entity.jutsuprojectile.visual;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.JutsuCombatHelper;
import com.sekwah.narutomod.abilities.jutsus.ChidoriStreamAbility;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
 * Handles bind, seal, and target/caster-attached aura effects:
 * Shadow Bind, Adamantine Chains, Chidori Burst/Stream aura, and Rasengan Sphere aura.
 */
public class BindAndAuraVisualBehavior implements JutsuVisualBehavior {

    @Override
    public void tickServer(JutsuVisualEffectEntity entity, ServerLevel serverLevel, @Nullable Entity tracked) {
        switch (entity.getVisualType()) {
            case JutsuVisualEffectEntity.TYPE_CHIDORI_BURST -> tickChidoriBurst(entity, serverLevel, tracked);
            case JutsuVisualEffectEntity.TYPE_RASENGAN_SPHERE -> tickRasenganSphere(entity, serverLevel);
            case JutsuVisualEffectEntity.TYPE_ADAMANTINE_CHAINS -> tickAdamantineChains(entity, serverLevel);
            case JutsuVisualEffectEntity.TYPE_SHADOW_BIND -> {
                // Shadow bind physics/immobilization are managed by ShadowPossessionAbility.
            }
        }
    }

    private static void tickChidoriBurst(JutsuVisualEffectEntity entity, ServerLevel serverLevel, @Nullable Entity tracked) {
        if (entity.tickCount % 8 == 0) {
            serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    NarutoSounds.CHIDORI_LOOP.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
        }
        if (entity.getTargetId() >= 0 && entity.getEffectDamage() > 0.0F) {
            if (!(tracked instanceof LivingEntity fieldOwner) || !fieldOwner.isAlive()) {
                entity.discard();
                return;
            }
            double radius = ChidoriStreamAbility.STREAM_RADIUS;
            double radiusSq = radius * radius;
            Vec3 center = fieldOwner.position();
            AABB fieldBox = fieldOwner.getBoundingBox().inflate(radius, 2.5D, radius);

            // Continuous projectile deflection while the electric field is active
            List<Projectile> incoming = serverLevel.getEntitiesOfClass(Projectile.class, fieldBox,
                    p -> p.isAlive() && p.getOwner() != fieldOwner
                            && !ShadowCloneEntity.areAllies(fieldOwner, p.getOwner())
                            && p.position().distanceToSqr(center) <= radiusSq);
            for (Projectile projectile : incoming) {
                Vec3 away = projectile.position().subtract(center);
                if (away.lengthSqr() < 1.0E-4D) {
                    away = new Vec3(0.0D, 1.0D, 0.0D);
                }
                projectile.setDeltaMovement(away.normalize().scale(1.15D).add(0.0D, 0.22D, 0.0D));
                projectile.needsSync = true;
            }

            // Continuous ambient electric sparks around the caster
            if (entity.tickCount % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        center.x, center.y + 1.0D, center.z,
                        14, 1.9D, 0.7D, 1.9D, 0.14D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            }

            // Periodic lightning shockwave pulse every 10 ticks (0.5s)
            if (entity.tickCount % ChidoriStreamAbility.PULSE_INTERVAL_TICKS == 0) {
                float pulseDmg = Math.max(4.0F, entity.getEffectDamage());
                List<LivingEntity> shocked = serverLevel.getEntitiesOfClass(LivingEntity.class, fieldBox,
                        e -> e.isAlive() && e != fieldOwner
                                && !ShadowCloneEntity.areAllies(fieldOwner, e)
                                && e.position().distanceToSqr(center) <= radiusSq);
                DamageSource chidoriDamage = entity.createChidoriLightningDamage(serverLevel, fieldOwner);
                for (LivingEntity victim : shocked) {
                    float finalDmg = victim.isInWaterOrRain() ? pulseDmg * 1.25F : pulseDmg;
                    victim.hurtServer(serverLevel, chidoriDamage, finalDmg);
                    victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 2));
                    Vec3 away = victim.position().subtract(center);
                    double horiz = Math.hypot(away.x, away.z);
                    if (horiz > 1.0E-4D) {
                        victim.push((away.x / horiz) * 0.35D, 0.14D, (away.z / horiz) * 0.35D);
                    }
                    victim.needsSync = true;
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                            10, 0.25D, 0.35D, 0.25D, 0.12D,
                            ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                }

                PacketHandler.sentToTracking(
                        new ClientJutsuEffectPacket(
                                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "elemental_burst"),
                                center.add(0.0D, 1.0D, 0.0D),
                                new Vec3(0.0D, 1.0D, 0.0D),
                                1.9F,
                                0x55CCFF),
                        fieldOwner);
            }
        }
    }

    private static void tickRasenganSphere(JutsuVisualEffectEntity entity, ServerLevel serverLevel) {
        if (entity.tickCount % 8 == 0) {
            serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    NarutoSounds.RASENGAN_LOOP.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        }
    }

    private static void tickAdamantineChains(JutsuVisualEffectEntity entity, ServerLevel serverLevel) {
        final double sealRadius = 3.4D;
        final double sealHalfHeight = 2.5D;
        if (entity.tickCount == 1) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    entity.getX(), entity.getY() + 0.5D, entity.getZ(),
                    18, 1.4D, 0.4D, 1.4D, 0.08D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
        boolean doPin = entity.tickCount % 2 == 0 && entity.tickCount <= entity.getMaxLife() - 6;
        boolean doPulse = entity.tickCount == 3 || (entity.tickCount >= 18 && entity.tickCount <= entity.getMaxLife() - 6 && entity.tickCount % 15 == 0);
        Player caster = (doPin || doPulse) ? entity.getCasterPlayer() : null;
        if (doPin) {
            List<LivingEntity> pinned = JutsuCombatHelper.findHostileTargetsInRadius(
                    serverLevel, caster, entity.position(), sealRadius, sealHalfHeight);
            for (LivingEntity victim : pinned) {
                Vec3 toCenter = entity.position().subtract(victim.position());
                double pullX = Mth.clamp(toCenter.x * 0.08D, -0.18D, 0.18D);
                double pullZ = Mth.clamp(toCenter.z * 0.08D, -0.18D, 0.18D);
                double clampedY = Math.min(0.0D, victim.getDeltaMovement().y);
                JutsuCombatHelper.applyVelocity(victim, new Vec3(pullX, clampedY, pullZ), false);
            }
        }
        if (doPulse) {
            float baseDmg = Math.max(4.0F, entity.getEffectDamage());
            float pulseDmg = (entity.tickCount == 3) ? baseDmg * 0.40F : baseDmg * 0.20F;
            serverLevel.playSound(null, entity.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS,
                    1.35F, entity.tickCount == 3 ? 0.82F : 1.08F);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    entity.getX(), entity.getY() + 0.85D, entity.getZ(),
                    14, 1.35D, 0.65D, 1.35D, 0.04D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    entity.getX(), entity.getY() + 0.45D, entity.getZ(),
                    18, 1.7D, 0.4D, 1.7D, 0.14D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            List<LivingEntity> victims = JutsuCombatHelper.findHostileTargetsInRadius(
                    serverLevel, caster, entity.position(), sealRadius, sealHalfHeight);
            DamageSource source = JutsuCombatHelper.playerOrMagicDamage(serverLevel, caster);
            for (LivingEntity victim : victims) {
                victim.hurtServer(serverLevel, source, pulseDmg);
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 3, false, false));
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, false, false));
                if (victim instanceof Player targetPlayer) {
                    var targetData = targetPlayer.getData(NinjaCapabilityHandler.NINJA_DATA);
                    targetData.lockChakraRegen(120);
                    float siphon = (entity.tickCount == 3) ? 12.0F : 4.0F;
                    targetData.setChakra(Math.max(0.0F, targetData.getChakra() - siphon));
                }
            }
        } else if (entity.tickCount >= 12 && entity.tickCount <= entity.getMaxLife() - 6 && entity.tickCount % 15 == 12) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    entity.getX(), entity.getY() + 0.6D, entity.getZ(),
                    6, 1.5D, 0.4D, 1.5D, 0.02D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
    }
}
