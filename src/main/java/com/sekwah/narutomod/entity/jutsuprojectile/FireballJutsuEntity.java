package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.damagesource.NarutoDamageSource;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.util.WorldProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_PHOENIX_MINI =
            net.minecraft.network.syncher.SynchedEntityData.defineId(FireballJutsuEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    public int time;
    public int lifeSpan = 70;
    public float explosionPower = 2;
    private int jutsuPower = 60;
    private boolean windBoosted = false;
    private float customDamage = -1.0f;

    public boolean isWindBoosted() {
        return this.windBoosted;
    }

    public void markWindBoosted() {
        this.windBoosted = true;
    }

    public boolean isPhoenixMini() {
        return this.entityData.get(DATA_PHOENIX_MINI);
    }

    public void setPhoenixMini(float damage) {
        this.entityData.set(DATA_PHOENIX_MINI, true);
        this.customDamage = damage;
        this.explosionPower = 0.8f;
        this.jutsuPower = 25;
        this.lifeSpan = 45;
    }

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
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.5D, this.getZ(), 45, 0.8D, 0.8D, 0.8D, 0.12D,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 15, 0.5D, 0.5D, 0.5D, 0.05D,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
        this.discard();
    }

    public FireballJutsuEntity(EntityType<FireballJutsuEntity> entityConstructor, Level level) {
        super(entityConstructor, level);
        this.time = 0;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IMPACTED, false);
        builder.define(DATA_IMPACT_TICKS, 0);
        builder.define(DATA_PHOENIX_MINI, false);
    }

    public FireballJutsuEntity(EntityType<? extends AbstractHurtingProjectile> p_36817_, double p_36818_, double p_36819_, double p_36820_, double p_36821_, double p_36822_, double p_36823_, Level p_36824_) {
        super(p_36817_, p_36818_, p_36819_, p_36820_, new Vec3(p_36821_, p_36822_, p_36823_), p_36824_);
        this.accelerationPower = 0.24D;
    }

    public FireballJutsuEntity(LivingEntity player, double xVel, double yVel, double zVel) {
        this(NarutoEntities.FIREBALL_JUTSU.get(), player.getX(), player.getEyeY() - 0.2f, player.getZ(), xVel, yVel, zVel, player.level());
        this.setOwner(player);
        this.setRot(player.getYRot(), player.getXRot());
    }

    @Override
    public EntityDimensions getDimensions(Pose p_19721_) {
        float baseSize = this.isPhoenixMini() ? ENTITY_SIZE * 0.45f : ENTITY_SIZE;
        return EntityDimensions.scalable(baseSize, baseSize)
                .scale(Math.min(INITIAL_SCALE + (GROW_SCALE - (GROW_SCALE * ((GROW_TIME - time) / GROW_TIME))), 1.0f));
    }

    @Override
    public void tick() {
        if (this.isImpacted()) {
            this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            this.time++;
            int it = this.getImpactTicks() + 1;
            if (!this.level().isClientSide()) {
                this.entityData.set(DATA_IMPACT_TICKS, it);
                if (this.level() instanceof ServerLevel serverLevel) {
                    boolean mini = this.isPhoenixMini();
                    int count = mini ? 2 : 8;
                    for (int i = 0; i < count; i++) {
                        double angle = (it * 0.5) + (i * Math.PI * 2.0 / count);
                        double r = mini ? (0.45 + it * 0.08) : (1.2 + (it * 0.18));
                        serverLevel.sendParticles(ParticleTypes.FLAME,
                                this.getX() + Math.cos(angle) * r,
                                this.getY() + (mini ? 0.25 : 0.5) + (this.random.nextDouble() - 0.3) * (mini ? 0.7 : 1.8),
                                this.getZ() + Math.sin(angle) * r,
                                mini ? 1 : 2, 0.15, 0.15, 0.15, 0.03,
                                net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                    }
                    if (it >= (mini ? 8 : 18)) {
                        this.discard();
                    }
                }
            }
            return;
        }

        if (!this.level().isClientSide() && !this.isImpacted()) {
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
            if (hitResult.getType() != HitResult.Type.MISS && !net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this, hitResult)) {
                this.onHit(hitResult);
                return;
            }
        }

        super.tick();
        ++this.time;
        this.refreshDimensions();

        if (!this.level().isClientSide() && !this.isImpacted() && this.isAlive()) {
            AABB clashBox = this.getBoundingBox().inflate(0.35D);
            List<Entity> others = this.level().getEntities(this, clashBox,
                    e -> e instanceof JutsuProjectile && e.isAlive() && e != this && !this.isPassengerOfSameVehicle(e));
            for (Entity e : others) {
                if (e instanceof EarthBulletEntity earthBullet
                        && com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(this.getOwner(), earthBullet.getOwner())) {
                    com.sekwah.narutomod.events.ElementalInteractionHandler.igniteEarthWithFire(earthBullet, this);
                    continue;
                }
                if (e instanceof VacuumSphereEntity vacuumSphere
                        && com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(this.getOwner(), vacuumSphere.getOwner())) {
                    com.sekwah.narutomod.events.ElementalInteractionHandler.boostFireballWithWind(this);
                    continue;
                }
                if (e instanceof JutsuProjectile other && this.isAlive() && e.isAlive()) {
                    this.handleClash(other);
                    if (!this.isAlive()) {
                        return;
                    }
                }
            }
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel && this.time % 2 == 0) {
            boolean mini = this.isPhoenixMini();
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + (mini ? 0.25 : 0.5), this.getZ(),
                    mini ? 2 : 5, mini ? 0.14 : 0.35, mini ? 0.14 : 0.35, mini ? 0.14 : 0.35, 0.03,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
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
                        this.isPhoenixMini() ? 12 : 40,
                        0.5, 0.2, 0.5, 0,
                        net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            }
            this.playSound(SoundEvents.FIRE_EXTINGUISH, 1F, 1.0F);
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != this.getOwner()
                && !com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(this.getOwner(), entity);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (this.isImpacted()) return;
        super.onHit(hitResult);
        this.entityData.set(DATA_IMPACTED, true);
        this.entityData.set(DATA_IMPACT_TICKS, 0);
        this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

        final boolean mini = this.isPhoenixMini();
        if(this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                    mini ? 22 : 85, mini ? 0.5 : 1.2, mini ? 0.4 : 1.0, mini ? 0.5 : 1.2, 0.12,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            int flameRadius = mini ? (this.isWindBoosted() ? 3 : 2) : 8;
            Entity owner = this.getOwner();
            if (!mini && owner instanceof Player player) {
                int mastery = player.getData(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA)
                        .getMastery(com.sekwah.narutomod.abilities.NarutoAbilities.FIREBALL.getId());
                int tier = com.sekwah.narutomod.abilities.Ability.getMasteryTier(mastery);
                if (tier >= 5) flameRadius = 10;
                else if (tier >= 3) flameRadius = 9;
            }

            final int effectiveRadius = flameRadius;
            final Entity directTarget = (hitResult instanceof EntityHitResult ehr) ? ehr.getEntity() : null;
            this.level().getEntities(this, this.getBoundingBox().inflate(flameRadius, flameRadius, flameRadius)).forEach(entity -> {
                if (mini && (entity == owner || com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(owner, entity))) {
                    return;
                }
                double distance = this.distanceToSqr(entity);
                if (distance > effectiveRadius * effectiveRadius && entity != directTarget) {
                    return;
                }

                // Line of Sight check via ClipContext RayTracing
                if (entity != directTarget) {
                    HitResult los = this.level().clip(new ClipContext(
                            this.position(), entity.getEyePosition(),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                    if (los.getType() != HitResult.Type.MISS) {
                        double blockDist = this.distanceToSqr(los.getLocation().x, los.getLocation().y, los.getLocation().z);
                        if (blockDist < distance * 0.85D) {
                            return; // Obstructed by terrain
                        }
                    }
                }

                float fireSecs = mini ? 60.0f : (float) (8f - (distance / 6f)) * 20;
                float baseDamage;
                if (mini && this.customDamage > 0) {
                    baseDamage = this.customDamage * (this.isWindBoosted() ? 1.25f : 1.0f);
                } else if (owner instanceof Player player) {
                    baseDamage = com.sekwah.narutomod.abilities.NarutoAbilities.FIREBALL.get().getDamage(
                            player.getData(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA));
                } else {
                    baseDamage = com.sekwah.narutomod.abilities.NarutoAbilities.FIREBALL.get().getDamage();
                }
                float fireDamage = mini
                        ? (entity == directTarget ? baseDamage + 1.5f : (float) Math.max(0.0, baseDamage - (distance / 3.0f)))
                        : (float) (baseDamage - (distance / 4f));
                if(fireDamage > 0) {
                    Entity entity1 = this.getOwner();
                    DamageSource source = NarutoDamageSource.fireball(this, entity1);
                    entity.hurtServer(serverLevel, source, fireDamage);
                    if (entity1 instanceof LivingEntity) {
                        EnchantmentHelper.doPostAttackEffects(serverLevel, entity, source);
                    }
                }
                if(entity.getRemainingFireTicks() < fireSecs) {
                    entity.setRemainingFireTicks(Math.round(fireSecs));
                }
            });

            if (!mini) {
                int fireSpread = 2;
                BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
                Entity fireOwner = this.getOwner();
                for (int x = (int) this.getX() - fireSpread; x < (int) this.getX() + fireSpread - 1; x++) {
                    for (int y = (int) this.getY() - fireSpread + 1; y < (int) this.getY() + fireSpread; y++) {
                        for (int z = (int) this.getZ() - fireSpread + 1; z < (int) this.getZ() + fireSpread; z++) {
                            blockPos.set(x, y, z);
                            if (this.random.nextInt(2) == 0
                                    && this.level().getBlockState(blockPos).isAir()
                                    && WorldProtectionHelper.canModifyBlock(serverLevel, blockPos, fireOwner)) {
                                this.level().setBlockAndUpdate(blockPos.immutable(), BaseFireBlock.getState(this.level(), blockPos));
                            }
                        }
                    }
                }
            } else {
                BlockPos impactPos = this.blockPosition();
                if (this.random.nextInt(3) == 0
                        && this.level().getBlockState(impactPos).isAir()
                        && WorldProtectionHelper.canModifyBlock(serverLevel, impactPos, owner)) {
                    this.level().setBlockAndUpdate(impactPos, BaseFireBlock.getState(this.level(), impactPos));
                }
            }

            this.playSound(SoundEvents.GENERIC_EXPLODE.value(), mini ? 1.1f : 3.0f, mini ? 1.45f : 0.9f);
            this.playSound(com.sekwah.narutomod.sounds.NarutoSounds.SIZZLE.get(), mini ? 0.9f : 2.0f, mini ? 1.3f : 1.1f);
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
    public boolean hurtServer(ServerLevel level, DamageSource p_36910_, float p_36911_) {
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
}
