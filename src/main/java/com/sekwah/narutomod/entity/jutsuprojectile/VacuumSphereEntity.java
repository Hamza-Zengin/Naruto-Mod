package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.events.ElementalInteractionHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
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
 * Fūton: Vacuum Spheres Projectile Entity (Shinkūgyoku)
 * Rapid piercing wind sphere with Wind > Lightning clash superiority and allied Wind + Fire fanning.
 */
public class VacuumSphereEntity extends ThrowableItemProjectile implements JutsuProjectile {

    private static final EntityDataAccessor<Integer> DATA_MAX_LIFE =
            SynchedEntityData.defineId(VacuumSphereEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(VacuumSphereEntity.class, EntityDataSerializers.FLOAT);

    private int jutsuPower = 40;

    public VacuumSphereEntity(EntityType<? extends VacuumSphereEntity> type, Level level) {
        super(type, level);
    }

    public VacuumSphereEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.VACUUM_SPHERE.get(), shooter, level, new ItemStack(Items.WIND_CHARGE));
        this.setNoGravity(true);
    }

    public VacuumSphereEntity(Level level, double x, double y, double z) {
        super(NarutoEntities.VACUUM_SPHERE.get(), x, y, z, level, new ItemStack(Items.WIND_CHARGE));
        this.setNoGravity(true);
    }

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
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 1.5F);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 0.25D, this.getZ(),
                    14, 0.35D, 0.35D, 0.35D, 0.12D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    this.getX(), this.getY() + 0.25D, this.getZ(),
                    2, 0.2D, 0.2D, 0.2D, 0.05D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
        this.discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MAX_LIFE, 32);
        builder.define(DATA_DAMAGE, 6.5F);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = this.getOwner();
        if (target == owner || (owner != null && target.getUUID().equals(owner.getUUID()))
                || ShadowCloneEntity.areAllies(owner, target)) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.WIND_CHARGE;
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
    protected double getDefaultGravity() {
        return this.isNoGravity() ? 0.0D : 0.01D;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.CLOUD,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.25D,
                    this.getY() + 0.2D + (this.random.nextDouble() - 0.5D) * 0.25D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.25D,
                    0.0D, 0.01D, 0.0D);
        } else if (this.level() instanceof ServerLevel serverLevel) {
            if (this.tickCount % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        this.getX(), this.getY() + 0.2D, this.getZ(),
                        3, 0.15D, 0.15D, 0.15D, 0.03D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            }

            // Wind + Fire synergy check: fans allied FireballJutsuEntity instead of self-clashing
            if (this.isAlive()) {
                AABB boostBox = this.getBoundingBox().inflate(2.2D);
                List<FireballJutsuEntity> fireballs = this.level().getEntitiesOfClass(
                        FireballJutsuEntity.class, boostBox,
                        fb -> fb.isAlive() && !fb.isWindBoosted() && ShadowCloneEntity.areAllies(this.getOwner(), fb.getOwner()));
                for (FireballJutsuEntity fb : fireballs) {
                    ElementalInteractionHandler.boostFireballWithWind(fb);
                }
            }

            if (this.isAlive()) {
                AABB clashBox = this.getBoundingBox().inflate(0.35D);
                List<Entity> others = this.level().getEntities(this, clashBox,
                        e -> e instanceof JutsuProjectile && e.isAlive() && e != this && !this.isPassengerOfSameVehicle(e));
                for (Entity e : others) {
                    if (e instanceof Projectile p && ShadowCloneEntity.areAllies(this.getOwner(), p.getOwner())) {
                        // Never clash with allied Fireball (fanned above) or sibling VacuumSphere shots
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

            if (this.tickCount >= this.getMaxLife()) {
                this.discard();
                return;
            }
            if (this.isAlive()) {
                int lightLevel = com.sekwah.narutomod.events.JutsuLightManager.resolveLightLevelForEntityClass(this.getClass());
                if (lightLevel > 0) {
                    com.sekwah.narutomod.events.JutsuLightManager.updateLight(
                            serverLevel, this, this.blockPosition(), lightLevel);
                }
            }
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
            float physicalDmg = dmg * 0.65F;
            float piercingDmg = Math.max(1.0F, dmg * 0.35F);

            // Reset invulnerability window so staggered 3-sphere bursts land cleanly
            hitEntity.setInvulnerableTime(0);
            if (owner instanceof LivingEntity livingOwner) {
                hitEntity.hurtServer(serverLevel, serverLevel.damageSources().mobProjectile(this, livingOwner), physicalDmg);
            } else {
                hitEntity.hurtServer(serverLevel, serverLevel.damageSources().generic(), physicalDmg);
            }
            hitEntity.setInvulnerableTime(0);
            hitEntity.hurtServer(serverLevel, serverLevel.damageSources().magic(), piercingDmg);

            if (hitEntity instanceof LivingEntity livingTarget) {
                Vec3 motion = this.getDeltaMovement();
                Vec3 push = (motion.lengthSqr() > 1.0E-4D ? motion.normalize() : new Vec3(0, 0, 1))
                        .scale(0.85D).add(0.0D, 0.18D, 0.0D);
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
        if (!this.level().isClientSide()) {
            this.explodeAndDiscard();
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("MaxLife", this.getMaxLife());
        output.putFloat("Damage", this.getDamage());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setMaxLife(input.getIntOr("MaxLife", 32));
        this.setDamage(input.getFloatOr("Damage", 6.5F));
    }
}
