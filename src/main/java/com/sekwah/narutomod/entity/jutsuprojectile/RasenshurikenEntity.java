package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.damagesource.NarutoDamageSource;
import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Fūton: Rasenshuriken 3D Projectile Entity
 * High-speed spinning 4-bladed wind chakra shuriken that expands into a cellular vortex dome upon impact.
 */
public class RasenshurikenEntity extends AbstractHurtingProjectile implements JutsuProjectile {

    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_IMPACTED =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RasenshurikenEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_VORTEX_TICKS =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RasenshurikenEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    public int time;
    public int lifeSpan = 80;
    private int jutsuPower = 80;

    @Override
    public ChakraElement getElement() {
        return ChakraElement.WIND;
    }

    @Override
    public int getJutsuPower() {
        return this.jutsuPower;
    }

    @Override
    public void weaken(int amount) {
        this.jutsuPower = Math.max(0, this.jutsuPower - amount);
        if (this.jutsuPower <= 0) {
            this.explodeAndDiscard();
        }
    }

    @Override
    public void explodeAndDiscard() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.5D, this.getZ(), 30, 0.8D, 0.8D, 0.8D, 0.1D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5F, 1.2F);
        }
        this.discard();
    }

    public RasenshurikenEntity(EntityType<RasenshurikenEntity> entityConstructor, Level level) {
        super(entityConstructor, level);
        this.time = 0;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IMPACTED, false);
        this.entityData.define(DATA_VORTEX_TICKS, 0);
    }

    public RasenshurikenEntity(EntityType<? extends AbstractHurtingProjectile> type, double x, double y, double z, double xVel, double yVel, double zVel, Level level) {
        super(type, level);
        this.moveTo(x, y, z, this.getYRot(), this.getXRot());
        this.reapplyPosition();
        double d0 = Math.sqrt(xVel * xVel + yVel * yVel + zVel * zVel);
        if (d0 != 0.0D) {
            this.xPower = xVel / d0 * 0.32D;
            this.yPower = yVel / d0 * 0.32D;
            this.zPower = zVel / d0 * 0.32D;
        }
    }

    public RasenshurikenEntity(LivingEntity shooter, double xVel, double yVel, double zVel) {
        this(NarutoEntities.RASENSHURIKEN.get(), shooter.getX(), shooter.getEyeY() - 0.1f, shooter.getZ(), xVel, yVel, zVel, shooter.level());
        this.setOwner(shooter);
        this.setRot(shooter.getYRot(), shooter.getXRot());
    }

    @Override
    public void tick() {
        if (this.isImpacted()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.time++;
            int vt = this.getVortexTicks() + 1;
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_VORTEX_TICKS, vt);
            }
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                double radius = 5.5D;
                // Spiral wind dome particles
                for (int i = 0; i < 12; i++) {
                    double angle = (vt * 0.45) + (i * Math.PI / 6.0);
                    double r = 1.5 + (i % 3) * 1.4;
                    double px = this.getX() + Math.cos(angle) * r;
                    double pz = this.getZ() + Math.sin(angle) * r;
                    double py = this.getY() + (this.random.nextDouble() - 0.3) * 2.5;
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, px, py, pz, 1, 0.1, 0.1, 0.1, 0.0);
                    serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 2, 0.15, 0.15, 0.15, 0.02);
                }

                // Multi-hit cellular wind blades & pull towards vortex center
                if (vt % 4 == 0) {
                    Entity owner = this.getOwner();
                    AABB box = this.getBoundingBox().inflate(radius);
                    List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                            e -> e.isAlive() && e != owner);
                    for (LivingEntity target : targets) {
                        double dx = this.getX() - target.getX();
                        double dz = this.getZ() - target.getZ();
                        double distSq = dx * dx + dz * dz;

                        // Line of Sight check via ClipContext
                        HitResult los = serverLevel.clip(new ClipContext(
                                this.position(), target.getEyePosition(),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                        if (los.getType() != HitResult.Type.MISS) {
                            double blockDist = this.distanceToSqr(los.getLocation().x, los.getLocation().y, los.getLocation().z);
                            if (blockDist < distSq * 0.85D) {
                                continue; // Obstructed by terrain
                            }
                        }

                        if (distSq > 1.0E-4D) {
                            double dist = Math.sqrt(distSq);
                            double factor = 0.35D / dist;
                            target.setDeltaMovement(dx * factor, 0.12, dz * factor);
                        }
                        target.hurtMarked = true;
                        target.invulnerableTime = 0;
                        target.hurt(NarutoDamageSource.rasenshuriken(this, owner), com.sekwah.narutomod.abilities.NarutoAbilities.RASENSHURIKEN.get().getDamage());
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                    }
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 1.6f);
                }

                if (vt >= 28) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5f, 1.2f);
                    this.discard();
                }
            }
            return;
        }

        if (!this.level().isClientSide && !this.isImpacted()) {
            Vec3 start = this.position();
            Vec3 delta = this.getDeltaMovement();
            Vec3 end = start.add(delta);
            HitResult hitResult = this.level().clip(new ClipContext(
                    start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hitResult.getType() != HitResult.Type.MISS) {
                end = hitResult.getLocation();
            }
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    this.level(), this, start, end, this.getBoundingBox().expandTowards(delta).inflate(1.0D), this::canHitEntity);
            if (entityHit != null) {
                hitResult = entityHit;
            }
            if (hitResult.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitResult)) {
                this.onHit(hitResult);
                return;
            }
        }

        super.tick();
        ++this.time;

        if (!this.level().isClientSide && !this.isImpacted() && this.isAlive()) {
            AABB clashBox = this.getBoundingBox().inflate(0.4D);
            List<Entity> others = this.level().getEntities(this, clashBox,
                    e -> e instanceof JutsuProjectile && e.isAlive() && e != this && !this.isPassengerOfSameVehicle(e));
            for (Entity e : others) {
                if (e instanceof JutsuProjectile other && this.isAlive() && e.isAlive()) {
                    this.handleClash(other);
                    if (!this.isAlive()) {
                        return;
                    }
                }
            }
        }

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            if (this.time % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3, this.getZ(), 3, 0.4, 0.1, 0.4, 0.02);
                serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.3, this.getZ(), 2, 0.3, 0.1, 0.3, 0.01);
            }
            if (this.time % 5 == 0) {
                serverLevel.playSound(null, this.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6f, 1.8f);
            }
        }

        if (this.lifeSpan-- <= 0) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != this.getOwner();
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (!this.isImpacted()) {
            this.entityData.set(DATA_IMPACTED, true);
            this.entityData.set(DATA_VORTEX_TICKS, 0);
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide) {
                this.playSound(SoundEvents.TRIDENT_THUNDER, 2.0f, 1.5f);
            }
        }
    }

    public boolean isImpacted() {
        return this.entityData.get(DATA_IMPACTED);
    }

    public int getVortexTicks() {
        return this.entityData.get(DATA_VORTEX_TICKS);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.CLOUD;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        double d0 = packet.getXa();
        double d1 = packet.getYa();
        double d2 = packet.getZa();
        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
        if (d3 != 0.0D) {
            this.xPower = d0 / d3 * 0.32D;
            this.yPower = d1 / d3 * 0.32D;
            this.zPower = d2 / d3 * 0.32D;
        }
    }
}
