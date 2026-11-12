package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class ShadowCloneEntity extends PathfinderMob {

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> DATA_OWNER_NAME =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_MAX_LIFESPAN =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_SLIM =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUUID;
    private int lifeTicks = 600; // 30 seconds default

    public ShadowCloneEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
        this.entityData.define(DATA_OWNER_NAME, "");
        this.entityData.define(DATA_MAX_LIFESPAN, 600);
        this.entityData.define(DATA_IS_SLIM, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D) // Dies in 1 hit like shadow clone
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, false));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                (target) -> target instanceof Enemy && !(target instanceof ShadowCloneEntity) && (this.ownerUUID == null || !target.getUUID().equals(this.ownerUUID))));
    }

    public void setOwner(Player player) {
        if (player != null) {
            this.ownerUUID = player.getUUID();
            this.entityData.set(DATA_OWNER_UUID, Optional.of(player.getUUID()));
            String name = player.getGameProfile().getName();
            this.entityData.set(DATA_OWNER_NAME, name);
            this.setCustomName(player.getName());
            this.setCustomNameVisible(true);

            // Copy player equipment
            for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                net.minecraft.world.item.ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    this.setItemSlot(slot, stack.copy());
                    this.setDropChance(slot, 0.0F); // Prevent duplication
                }
            }
        }
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(this.ownerUUID);
    }

    public String getOwnerName() {
        return this.entityData.get(DATA_OWNER_NAME);
    }

    public void setMaxLifespan(int ticks) {
        this.lifeTicks = ticks;
        this.entityData.set(DATA_MAX_LIFESPAN, ticks);
    }

    public int getMaxLifespan() {
        return this.entityData.get(DATA_MAX_LIFESPAN);
    }

    public void setIsSlim(boolean slim) {
        this.entityData.set(DATA_IS_SLIM, slim);
    }

    public boolean isSlim() {
        return this.entityData.get(DATA_IS_SLIM);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.getOwnerUUID() != null) {
            compound.putUUID("Owner", this.getOwnerUUID());
        }
        compound.putString("OwnerName", this.getOwnerName());
        compound.putInt("LifeTicks", this.lifeTicks);
        compound.putInt("MaxLifespan", this.getMaxLifespan());
        compound.putBoolean("IsSlim", this.isSlim());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("Owner")) {
            UUID uuid = compound.getUUID("Owner");
            this.ownerUUID = uuid;
            this.entityData.set(DATA_OWNER_UUID, Optional.of(uuid));
        }
        if (compound.contains("OwnerName")) {
            this.entityData.set(DATA_OWNER_NAME, compound.getString("OwnerName"));
        }
        if (compound.contains("LifeTicks")) {
            this.lifeTicks = compound.getInt("LifeTicks");
        }
        if (compound.contains("MaxLifespan")) {
            this.entityData.set(DATA_MAX_LIFESPAN, compound.getInt("MaxLifespan"));
        }
        if (compound.contains("IsSlim")) {
            this.entityData.set(DATA_IS_SLIM, compound.getBoolean("IsSlim"));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            this.lifeTicks--;
            if (this.lifeTicks <= 0) {
                this.poof();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide) {
            this.poof();
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void dropAllDeathLoot(DamageSource damageSource) {
        // Shadow clones vanish into thin smoke and never drop copied gear
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        // Anti-dupe protection
    }

    @Override
    protected void dropEquipment() {
        // Anti-dupe protection
    }

    public void poof() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.8, this.getZ(),
                    25, 0.4, 0.5, 0.4, 0.05);
            serverLevel.playSound(null, this.blockPosition(), NarutoSounds.CLONE_POOF.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        this.discard();
    }
}
