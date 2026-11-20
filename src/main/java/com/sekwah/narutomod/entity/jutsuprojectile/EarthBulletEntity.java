package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.events.ElementalInteractionHandler;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Doton: Earth Dragon Bullet Projectile Entity (Doryūdan)
 * Heavy earthen projectile dealing strong knockback, Slowness II, and Weakness I.
 * Passing near an allied FireballJutsuEntity ignites it into a Molten Earth Bullet (+35% damage + burn).
 */
public class EarthBulletEntity extends ThrowableItemProjectile implements JutsuProjectile {

    private static final EntityDataAccessor<Integer> DATA_MAX_LIFE =
            SynchedEntityData.defineId(EarthBulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(EarthBulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_MOLTEN =
            SynchedEntityData.defineId(EarthBulletEntity.class, EntityDataSerializers.BOOLEAN);

    private static final class Particles {
        static final BlockParticleOption COBBLESTONE_PARTICLE =
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState());
    }

    private int jutsuPower = 55;

    public EarthBulletEntity(EntityType<? extends EarthBulletEntity> type, Level level) {
        super(type, level);
    }

    public EarthBulletEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.EARTH_BULLET.get(), shooter, level, new ItemStack(Items.COBBLESTONE));
        this.setNoGravity(true);
    }

    public EarthBulletEntity(Level level, double x, double y, double z) {
        super(NarutoEntities.EARTH_BULLET.get(), x, y, z, level, new ItemStack(Items.COBBLESTONE));
        this.setNoGravity(true);
    }

    @Override
    public ChakraElement getElement() {
        return ChakraElement.EARTH;
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
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.5F, 0.8F);
            serverLevel.sendParticles(Particles.COBBLESTONE_PARTICLE,
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    22, 0.5D, 0.5D, 0.5D, 0.14D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            if (this.isMolten()) {
                serverLevel.sendParticles(ParticleTypes.LAVA,
                        this.getX(), this.getY() + 0.4D, this.getZ(),
                        12, 0.4D, 0.4D, 0.4D, 0.08D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            }
        }
        this.discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MAX_LIFE, 45);
        builder.define(DATA_DAMAGE, 14.0F);
        builder.define(DATA_MOLTEN, false);
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
        return Items.COBBLESTONE;
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

    public boolean isMolten() {
        return this.entityData.get(DATA_MOLTEN);
    }

    public void setMolten(boolean molten) {
        this.entityData.set(DATA_MOLTEN, molten);
        if (molten && this.jutsuPower < 75) {
            this.jutsuPower = 75;
        }
    }

    @Override
    protected double getDefaultGravity() {
        return this.isNoGravity() ? 0.0D : 0.02D;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(Particles.COBBLESTONE_PARTICLE,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 0.5D,
                        this.getY() + 0.25D + (this.random.nextDouble() - 0.5D) * 0.5D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 0.5D,
                        0.0D, 0.03D, 0.0D);
            }
            if (this.isMolten()) {
                this.level().addParticle(ParticleTypes.FLAME,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 0.45D,
                        this.getY() + 0.3D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 0.45D,
                        0.0D, 0.04D, 0.0D);
            }
        } else if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(Particles.COBBLESTONE_PARTICLE,
                    this.getX(), this.getY() + 0.35D, this.getZ(),
                    4, 0.3D, 0.3D, 0.3D, 0.05D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            if (this.isMolten()) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        this.getX(), this.getY() + 0.35D, this.getZ(),
                        5, 0.25D, 0.25D, 0.25D, 0.03D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            }

            if (this.isAlive()) {
                boolean molten = this.isMolten();
                AABB clashBox = this.getBoundingBox().inflate(0.35D);
                AABB scanBox = molten ? clashBox : this.getBoundingBox().inflate(2.2D);
                List<Entity> others = this.level().getEntities(this, scanBox,
                        e -> e instanceof JutsuProjectile && e.isAlive() && e != this && !this.isPassengerOfSameVehicle(e));
                Entity owner = this.getOwner();
                for (Entity e : others) {
                    if (e instanceof FireballJutsuEntity fb && ShadowCloneEntity.areAllies(owner, fb.getOwner())) {
                        if (!molten) {
                            ElementalInteractionHandler.igniteEarthWithFire(this, fb);
                            molten = this.isMolten();
                        }
                        continue;
                    }
                    if (!clashBox.intersects(e.getBoundingBox())) {
                        continue;
                    }
                    if (e instanceof Projectile p && ShadowCloneEntity.areAllies(owner, p.getOwner())) {
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
            if (owner instanceof LivingEntity livingOwner) {
                hitEntity.hurtServer(serverLevel, serverLevel.damageSources().mobProjectile(this, livingOwner), dmg);
            } else {
                hitEntity.hurtServer(serverLevel, serverLevel.damageSources().generic(), dmg);
            }

            if (hitEntity instanceof LivingEntity livingTarget) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                livingTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                if (this.isMolten()) {
                    livingTarget.setRemainingFireTicks(Math.max(livingTarget.getRemainingFireTicks(), 80));
                }
                Vec3 motion = this.getDeltaMovement();
                Vec3 push = (motion.lengthSqr() > 1.0E-4D ? motion.normalize() : new Vec3(0, 0, 1))
                        .scale(1.6D).add(0.0D, 0.40D, 0.0D);
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
        output.putBoolean("Molten", this.isMolten());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setMaxLife(input.getIntOr("MaxLife", 45));
        this.setDamage(input.getFloatOr("Damage", 14.0F));
        this.setMolten(input.getBooleanOr("Molten", false));
    }
}
