package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.damagesource.NarutoDamageSource;
import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FireballJutsuEntity extends AbstractHurtingProjectile implements JutsuProjectile {

    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_IMPACTED =
            net.minecraft.network.syncher.SynchedEntityData.defineId(FireballJutsuEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_IMPACT_TICKS =
            net.minecraft.network.syncher.SynchedEntityData.defineId(FireballJutsuEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    public int time;
    public int lifeSpan = 70;
    public float explosionPower = 2;
    private int jutsuPower = 60;

    public static final float INITIAL_SCALE = 0.25f;
    public static final float GROW_SCALE = 1 - INITIAL_SCALE;
    public static final float GROW_TIME = 12.0f;
    public static final float ENTITY_SIZE = 1.5f;

    @Override
    public ChakraElement getElement() {
        return ChakraElement.FIRE;
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
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.5D, this.getZ(), 45, 0.8D, 0.8D, 0.8D, 0.12D);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 15, 0.5D, 0.5D, 0.5D, 0.05D);
        }
        this.discard();
    }

    public FireballJutsuEntity(EntityType<FireballJutsuEntity> entityConstructor, Level level) {
        super(entityConstructor, level);
        this.time = 0;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IMPACTED, false);
        this.entityData.define(DATA_IMPACT_TICKS, 0);
    }

    public FireballJutsuEntity(EntityType<? extends AbstractHurtingProjectile> p_36817_, double p_36818_, double p_36819_, double p_36820_, double p_36821_, double p_36822_, double p_36823_, Level p_36824_) {
        super(p_36817_, p_36824_);
        this.moveTo(p_36818_, p_36819_, p_36820_, this.getYRot(), this.getXRot());
        this.reapplyPosition();
        double d0 = Math.sqrt(p_36821_ * p_36821_ + p_36822_ * p_36822_ + p_36823_ * p_36823_);
        if (d0 != 0.0D) {
            this.xPower = p_36821_ / d0 * 0.24D;
            this.yPower = p_36822_ / d0 * 0.24D;
            this.zPower = p_36823_ / d0 * 0.24D;
        }
    }

    public FireballJutsuEntity(LivingEntity player, double xVel, double yVel, double zVel) {
        this(NarutoEntities.FIREBALL_JUTSU.get(), player.getX(), player.getEyeY() - 0.2f, player.getZ(), xVel, yVel, zVel, player.level());
        this.setOwner(player);
        this.setRot(player.getYRot(), player.getXRot());
    }

    public EntityDimensions getDimensions(Pose p_19721_) {
        return EntityDimensions.scalable(ENTITY_SIZE, ENTITY_SIZE)
                .scale(Math.min(INITIAL_SCALE + (GROW_SCALE - (GROW_SCALE * ((GROW_TIME - time) / GROW_TIME))), 1.0f));
    }

    @Override
    public void tick() {
        if (this.isImpacted()) {
            this.setDeltaMovement( net.minecraft.world.phys.Vec3.ZERO);
            this.time++;
            int it = this.getImpactTicks() + 1;
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_IMPACT_TICKS, it);
                if (this.level() instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 8; i++) {
                        double angle = (it * 0.5) + (i * Math.PI / 4.0);
                        double r = 1.2 + (it * 0.18);
                        serverLevel.sendParticles(ParticleTypes.FLAME,
                                this.getX() + Math.cos(angle) * r,
                                this.getY() + 0.5 + (this.random.nextDouble() - 0.3) * 1.8,
                                this.getZ() + Math.sin(angle) * r,
                                2, 0.15, 0.15, 0.15, 0.03);
                    }
                    if (it >= 18) {
                        this.discard();
                    }
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
        this.refreshDimensions();

        if (!this.level().isClientSide && !this.isImpacted() && this.isAlive()) {
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

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel && this.time % 2 == 0) {
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.5, this.getZ(), 5, 0.35, 0.35, 0.35, 0.03);
        }

        if (this.isInWaterOrRain()) {
            lifeSpan--;
            if (lifeSpan % 5 == 0) {
                this.playSound(SoundEvents.FIRE_EXTINGUISH, 1F, 1.0F);
            }
        }

        if (this.isInWater() || lifeSpan-- <= 0) {
            if(this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        this.getX(),
                        this.getY() + this.getBbHeight() + 1,
                        this.getZ(),
                        40,
                        0.5, 0.2, 0.5, 0);
            }
            this.playSound(SoundEvents.FIRE_EXTINGUISH, 1F, 1.0F);
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != this.getOwner();
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (this.isImpacted()) return;
        super.onHit(hitResult);
        this.entityData.set(DATA_IMPACTED, true);
        this.entityData.set(DATA_IMPACT_TICKS, 0);
        this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

        if(this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                    85, 1.2, 1.0, 1.2, 0.15);
        }

        if (!this.level().isClientSide) {
            int flameRadius = 8;
            this.level().getEntities(this, this.getBoundingBox().inflate(flameRadius, flameRadius, flameRadius)).forEach(entity -> {
                double distance = this.distanceToSqr(entity);
                if (distance > flameRadius * flameRadius) {
                    return;
                }

                // Line of Sight check via ClipContext RayTracing
                HitResult los = this.level().clip(new ClipContext(
                        this.position(), entity.getEyePosition(),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (los.getType() != HitResult.Type.MISS) {
                    double blockDist = this.distanceToSqr(los.getLocation().x, los.getLocation().y, los.getLocation().z);
                    if (blockDist < distance * 0.85D) {
                        return; // Obstructed by terrain
                    }
                }

                float fireSecs = (float) (8f - (distance / 6f)) * 20;
                float baseDamage = com.sekwah.narutomod.abilities.NarutoAbilities.FIREBALL.get().getDamage();
                float fireDamage = (float) (baseDamage - (distance / 4f));
                if(fireDamage > 0) {
                    Entity entity1 = this.getOwner();
                    entity.hurt(NarutoDamageSource.fireball(this, entity1), fireDamage);
                    if (entity1 instanceof LivingEntity) {
                        this.doEnchantDamageEffects((LivingEntity)entity1, entity);
                    }
                }
                if(entity.getRemainingFireTicks() < fireSecs) {
                    entity.setRemainingFireTicks(Math.round(fireSecs));
                }
            });

            boolean flag = net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level(), this.getOwner());

            if(flag) {
                int fireSpread = 2;
                BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
                for (int x = (int) this.getX() - fireSpread; x < (int) this.getX() + fireSpread - 1; x++) {
                    for (int y = (int) this.getY() - fireSpread + 1; y < (int) this.getY() + fireSpread; y++) {
                        for (int z = (int) this.getZ() - fireSpread + 1; z < (int) this.getZ() + fireSpread; z++) {
                            blockPos.set(x, y, z);
                            if (this.random.nextInt(2) == 0 && this.level().getBlockState(blockPos).isAir()) {
                                this.level().setBlockAndUpdate(blockPos.immutable(), BaseFireBlock.getState(this.level(), blockPos));
                            }
                        }
                    }
                }
            }

            this.playSound(SoundEvents.GENERIC_EXPLODE, 3.0f, 0.9f);
            this.playSound(com.sekwah.narutomod.sounds.NarutoSounds.SIZZLE.get(), 2.0f, 1.1f);
        }
    }

    public boolean isImpacted() {
        return this.entityData.get(DATA_IMPACTED);
    }

    public int getImpactTicks() {
        return this.entityData.get(DATA_IMPACT_TICKS);
    }

    @Override
    public void refreshDimensions() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.refreshDimensions();
        this.setPos(d0, d1, d2);
    }

    @Override
    public boolean hurt(DamageSource p_36910_, float p_36911_) {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return this.isInWaterOrRain() ? ParticleTypes.CLOUD : ParticleTypes.LARGE_SMOKE;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket p_150128_) {
        super.recreateFromPacket(p_150128_);
        double d0 = p_150128_.getXa();
        double d1 = p_150128_.getYa();
        double d2 = p_150128_.getZa();
        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
        if (d3 != 0.0D) {
            this.xPower = d0 / d3 * 0.2D;
            this.yPower = d1 / d3 * 0.2D;
            this.zPower = d2 / d3 * 0.2D;
        }
    }
}
