package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

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

    private int jutsuPower = 50;

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
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    25, 0.5D, 0.5D, 0.5D, 0.15D);
            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    15, 0.4D, 0.4D, 0.4D, 0.1D);
        }
        this.discard();
    }

    public WaterBulletEntity(EntityType<? extends WaterBulletEntity> type, Level level) {
        super(type, level);
    }

    public WaterBulletEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.WATER_BULLET.get(), shooter, level);
        this.setNoGravity(true);
    }

    public WaterBulletEntity(Level level, double x, double y, double z) {
        super(NarutoEntities.WATER_BULLET.get(), x, y, z, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MAX_LIFE, 40);
        this.entityData.define(DATA_DAMAGE, 12.0F);
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

    @Override
    protected float getGravity() {
        return this.isNoGravity() ? 0.0F : 0.02F;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            for (int i = 0; i < 3; i++) {
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
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    8, 0.35D, 0.35D, 0.35D, 0.08D);
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    4, 0.25D, 0.25D, 0.25D, 0.04D);

            if (!this.level().isClientSide && this.isAlive()) {
                AABB clashBox = this.getBoundingBox().inflate(0.35D);
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

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            Entity hitEntity = result.getEntity();
            Entity owner = this.getOwner();
            if (hitEntity == owner) {
                return;
            }

            float dmg = this.getDamage();
            if (owner instanceof LivingEntity livingOwner) {
                hitEntity.hurt(serverLevel.damageSources().mobProjectile(this, livingOwner), dmg);
            } else {
                hitEntity.hurt(serverLevel.damageSources().drown(), dmg);
            }

            hitEntity.clearFire();
            if (hitEntity instanceof LivingEntity livingTarget) {
                Vec3 push = this.getDeltaMovement().normalize().scale(1.4D).add(0.0D, 0.35D, 0.0D);
                livingTarget.setDeltaMovement(push);
                livingTarget.hurtMarked = true;
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
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    this.getX(), this.getY() + 0.5D, this.getZ(),
                    25, 0.5D, 0.5D, 0.5D, 0.15D);
            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    this.getX(), this.getY() + 0.5D, this.getZ(),
                    12, 0.4D, 0.4D, 0.4D, 0.1D);
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MaxLife", this.getMaxLife());
        tag.putFloat("Damage", this.getDamage());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("MaxLife")) {
            this.setMaxLife(tag.getInt("MaxLife"));
        }
        if (tag.contains("Damage")) {
            this.setDamage(tag.getFloat("Damage"));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
