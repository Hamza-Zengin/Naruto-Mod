package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Suiton: Water Bullet Projectile Entity (Mizurappa / Suikōdan)
 * Physical water projectile derived from ThrowableItemProjectile with splash particles and impact damage.
 */
public class WaterBulletEntity extends ThrowableItemProjectile implements JutsuProjectile {

    private static final EntityDataAccessor<Integer> DATA_MAX_LIFE =
            SynchedEntityData.defineId(WaterBulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(WaterBulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_IS_DRAGON =
            SynchedEntityData.defineId(WaterBulletEntity.class, EntityDataSerializers.BOOLEAN);

    private int jutsuPower = 50;
    private float prevMotionYaw;
    private float prevMotionPitch;
    private float motionYaw;
    private float motionPitch;
    private boolean hasMotionRot = false;
    private int lockedTargetId = -1;

    public static float computeYawFromMotion(Vec3 motion) {
        return (float) (Mth.atan2(-motion.x, motion.z) * (180.0D / Math.PI));
    }

    public static float computePitchFromMotion(Vec3 motion) {
        return (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * (180.0D / Math.PI));
    }

    public void alignRotationToMovement() {
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6D) {
            float yaw = computeYawFromMotion(motion);
            float pitch = computePitchFromMotion(motion);
            this.setYRot(yaw);
            this.setXRot(pitch);
            this.prevMotionYaw = yaw;
            this.prevMotionPitch = pitch;
            this.motionYaw = yaw;
            this.motionPitch = pitch;
            this.hasMotionRot = true;
        }
    }

    public boolean hasMotionRot() {
        return this.hasMotionRot;
    }

    public float getPrevMotionYaw() {
        return this.prevMotionYaw;
    }

    public float getPrevMotionPitch() {
        return this.prevMotionPitch;
    }

    private void syncRotationFromMotion() {
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6D) {
            this.motionYaw = computeYawFromMotion(motion);
            this.motionPitch = computePitchFromMotion(motion);
            if (!this.hasMotionRot) {
                this.prevMotionYaw = this.motionYaw;
                this.prevMotionPitch = this.motionPitch;
                this.hasMotionRot = true;
            }
            this.setYRot(this.motionYaw);
            this.setXRot(this.motionPitch);
        }
    }

    @Override
    public ChakraElement getElement() {
        return ChakraElement.WATER;
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
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    25, 0.5D, 0.5D, 0.5D, 0.15D,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    15, 0.4D, 0.4D, 0.4D, 0.1D,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
        this.discard();
    }

    public WaterBulletEntity(EntityType<? extends WaterBulletEntity> type, Level level) {
        super(type, level);
    }

    public WaterBulletEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.WATER_BULLET.get(), shooter, level, new ItemStack(Items.WATER_BUCKET));
        this.setNoGravity(true);
    }

    public WaterBulletEntity(Level level, double x, double y, double z) {
        super(NarutoEntities.WATER_BULLET.get(), x, y, z, level, new ItemStack(Items.WATER_BUCKET));
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MAX_LIFE, 40);
        builder.define(DATA_DAMAGE, 12.0F);
        builder.define(DATA_IS_DRAGON, false);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = this.getOwner();
        if (target == owner || (owner != null && target.getUUID().equals(owner.getUUID()))
                || com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(owner, target)) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.WATER_BUCKET;
    }

    public int getMaxLife() {
        return this.entityData.get(DATA_MAX_LIFE);
    }

    public void setMaxLife(int maxLife) {
        this.entityData.set(DATA_MAX_LIFE, maxLife);
    }

    public float getDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    public void setDamage(float damage) {
        this.entityData.set(DATA_DAMAGE, damage);
    }

    public boolean isDragon() {
        return this.entityData.get(DATA_IS_DRAGON);
    }

    public void setDragon(boolean dragon) {
        this.entityData.set(DATA_IS_DRAGON, dragon);
        if (dragon) {
            this.jutsuPower = 90;
        }
    }

    @Override
    protected double getDefaultGravity() {
        return this.isNoGravity() ? 0.0D : 0.02D;
    }

    @Override
    public void tick() {
        Vec3 preMotion = this.getDeltaMovement();
        if (!this.hasMotionRot && preMotion.lengthSqr() > 1.0E-6D) {
            this.alignRotationToMovement();
        } else if (this.hasMotionRot) {
            this.prevMotionYaw = this.motionYaw;
            this.prevMotionPitch = this.motionPitch;
        }

        super.tick();

        if (this.level().isClientSide()) {
            this.syncRotationFromMotion();
            int particleCount = this.isDragon() ? 5 : 3;
            for (int i = 0; i < particleCount; i++) {
                this.level().addParticle(ParticleTypes.SPLASH,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 0.6D,
                        this.getY() + 0.3D + (this.random.nextDouble() - 0.5D) * 0.6D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 0.6D,
                        0.0D, 0.05D, 0.0D);
                this.level().addParticle(ParticleTypes.DRIPPING_WATER,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 0.4D,
                        this.getY() + 0.2D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 0.4D,
                        0.0D, -0.02D, 0.0D);
            }
        } else if (this.level() instanceof ServerLevel serverLevel) {
            if (this.isDragon() && this.tickCount > 2) {
                this.steerTowardNearestTarget(serverLevel);
            }
            this.syncRotationFromMotion();

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    this.isDragon() ? 14 : 8, 0.35D, 0.35D, 0.35D, 0.08D,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(this.isDragon() ? ParticleTypes.BUBBLE : ParticleTypes.DRIPPING_WATER,
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    this.isDragon() ? 8 : 4, 0.25D, 0.25D, 0.25D, 0.04D,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            if (!this.level().isClientSide() && this.isAlive()) {
                AABB clashBox = this.getBoundingBox().inflate(this.isDragon() ? 0.65D : 0.35D);
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

            if (this.tickCount >= this.getMaxLife()) {
                this.discard();
            }
        }
    }

    private void steerTowardNearestTarget(ServerLevel serverLevel) {
        Vec3 motion = this.getDeltaMovement();
        double speed = motion.length();
        if (speed < 1.0E-4D) {
            return;
        }
        Vec3 forward = motion.scale(1.0D / speed);

        LivingEntity bestTarget = null;
        if (this.lockedTargetId >= 0) {
            Entity cached = serverLevel.getEntity(this.lockedTargetId);
            if (cached instanceof LivingEntity living && living.isAlive() && this.canHitEntity(living)) {
                Vec3 toTarget = living.getEyePosition().subtract(this.position());
                double distSq = toTarget.lengthSqr();
                if (distSq >= 1.0E-4D && distSq <= 196.0D
                        && forward.dot(toTarget) > 0.35D * Math.sqrt(distSq)) {
                    bestTarget = living;
                }
            }
            if (bestTarget == null) {
                this.lockedTargetId = -1;
            }
        }

        if (bestTarget == null || this.tickCount % 3 == 0) {
            AABB searchBox = this.getBoundingBox().inflate(12.0D);
            List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox,
                    e -> e.isAlive() && this.canHitEntity(e));
            LivingEntity scannedBest = null;
            double bestDistSq = Double.MAX_VALUE;
            for (LivingEntity candidate : candidates) {
                Vec3 toTarget = candidate.getEyePosition().subtract(this.position());
                double distSq = toTarget.lengthSqr();
                if (distSq < 1.0E-4D || distSq > 196.0D) {
                    continue;
                }
                if (forward.dot(toTarget) > 0.35D * Math.sqrt(distSq) && distSq < bestDistSq) {
                    bestDistSq = distSq;
                    scannedBest = candidate;
                }
            }
            bestTarget = scannedBest;
            this.lockedTargetId = bestTarget != null ? bestTarget.getId() : -1;
        }

        if (bestTarget != null) {
            Vec3 desiredDir = bestTarget.getEyePosition().subtract(this.position()).normalize();
            Vec3 newDir = forward.scale(0.84D).add(desiredDir.scale(0.16D)).normalize();
            this.setDeltaMovement(newDir.scale(speed));
            this.needsSync = true;
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity hitEntity = result.getEntity();
            Entity owner = this.getOwner();
            if (hitEntity == owner) {
                return;
            }

            float dmg = this.getDamage();
            if (owner instanceof LivingEntity livingOwner) {
                hitEntity.hurtServer(serverLevel, serverLevel.damageSources().mobProjectile(this, livingOwner), dmg);
            } else {
                hitEntity.hurtServer(serverLevel, serverLevel.damageSources().drown(), dmg);
            }

            hitEntity.clearFire();
            if (hitEntity instanceof LivingEntity livingTarget) {
                double pushScale = this.isDragon() ? 1.85D : 1.4D;
                Vec3 motion = this.getDeltaMovement();
                Vec3 dir = motion.lengthSqr() > 1.0E-4D ? motion.normalize() : new Vec3(0, 0, 1);
                Vec3 push = dir.scale(pushScale).add(0.0D, this.isDragon() ? 0.45D : 0.35D, 0.0D);
                livingTarget.setDeltaMovement(push);
                livingTarget.needsSync = true;
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    this.getX(), this.getY() + 0.5D, this.getZ(),
                    this.isDragon() ? 45 : 25, 0.5D, 0.5D, 0.5D, 0.15D,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    this.getX(), this.getY() + 0.5D, this.getZ(),
                    this.isDragon() ? 24 : 12, 0.4D, 0.4D, 0.4D, 0.1D,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("MaxLife", this.getMaxLife());
        output.putFloat("Damage", this.getDamage());
        output.putBoolean("IsDragon", this.isDragon());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setMaxLife(input.getIntOr("MaxLife", 40));
        this.setDamage(input.getFloatOr("Damage", 12.0F));
        this.setDragon(input.getBooleanOr("IsDragon", false));
    }
}
