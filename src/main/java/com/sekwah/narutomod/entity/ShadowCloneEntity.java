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
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class ShadowCloneEntity extends PathfinderMob {

    public static final int MODE_FOLLOW = 0;
    public static final int MODE_PATROL = 1;
    public static final int MODE_HOLD = 2;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> DATA_OWNER_NAME =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_MAX_LIFESPAN =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_SLIM =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_COMMAND_MODE =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.INT);

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
        this.entityData.define(DATA_COMMAND_MODE, MODE_FOLLOW);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D) // Dies in 1 hit like shadow clone
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(2, new CloneFollowOwnerGoal(this, 1.25D));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                if (ShadowCloneEntity.this.getCommandMode() == MODE_HOLD) return false;
                return super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new CloneOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new CloneOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
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

    @Nullable
    public Player getOwner() {
        UUID uuid = this.getOwnerUUID();
        if (uuid == null) return null;
        return this.level().getPlayerByUUID(uuid);
    }

    public int getCommandMode() {
        return this.entityData.get(DATA_COMMAND_MODE);
    }

    public void setCommandMode(int mode) {
        this.entityData.set(DATA_COMMAND_MODE, mode);
    }

    public void setIsSlim(boolean slim) {
        this.entityData.set(DATA_IS_SLIM, slim);
    }

    public boolean isSlim() {
        return this.entityData.get(DATA_IS_SLIM);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getUUID().equals(this.getOwnerUUID()) && player.isCrouching()) {
            if (!this.level().isClientSide) {
                int nextMode = (this.getCommandMode() + 1) % 3;
                java.util.List<ShadowCloneEntity> clones = this.level().getEntitiesOfClass(
                        ShadowCloneEntity.class,
                        player.getBoundingBox().inflate(64.0D),
                        c -> player.getUUID().equals(c.getOwnerUUID())
                );
                for (ShadowCloneEntity clone : clones) {
                    clone.setCommandMode(nextMode);
                }

                String modeName;
                switch (nextMode) {
                    case MODE_PATROL -> modeName = "Saldırgan Devriye";
                    case MODE_HOLD -> modeName = "Konumu Koru";
                    default -> modeName = "Takip ve Koruma";
                }

                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§6Gölge Klonu Emri: §a[" + modeName + "]"),
                        true
                );
                this.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
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
        compound.putInt("CommandMode", this.getCommandMode());
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
        if (compound.contains("CommandMode")) {
            this.setCommandMode(compound.getInt("CommandMode"));
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

    public static class CloneFollowOwnerGoal extends Goal {
        private final ShadowCloneEntity clone;
        private final double speedModifier;
        private int timeToRecalcPath;

        public CloneFollowOwnerGoal(ShadowCloneEntity clone, double speedModifier) {
            this.clone = clone;
            this.speedModifier = speedModifier;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.clone.getCommandMode() != MODE_FOLLOW) {
                return false;
            }
            Player owner = this.clone.getOwner();
            if (owner == null || owner.isSpectator() || !owner.isAlive()) {
                return false;
            }
            return this.clone.distanceToSqr(owner) > 16.0D; // Greater than 4 blocks
        }

        @Override
        public boolean canContinueToUse() {
            if (this.clone.getCommandMode() != MODE_FOLLOW) {
                return false;
            }
            Player owner = this.clone.getOwner();
            if (owner == null || owner.isSpectator() || !owner.isAlive()) {
                return false;
            }
            return this.clone.distanceToSqr(owner) > 9.0D; // Stop within 3 blocks
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
        }

        @Override
        public void stop() {
            this.clone.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player owner = this.clone.getOwner();
            if (owner == null) return;

            this.clone.getLookControl().setLookAt(owner, 10.0F, (float) this.clone.getMaxHeadXRot());

            double distSq = this.clone.distanceToSqr(owner);
            // Teleport if too far (> 20 blocks = 400 distSq)
            if (distSq > 400.0D && owner.onGround()) {
                teleportNearOwner(owner);
                return;
            }

            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                this.clone.getNavigation().moveTo(owner, this.speedModifier);
            }
        }

        private void teleportNearOwner(Player owner) {
            for (int i = 0; i < 10; ++i) {
                int dx = this.clone.getRandom().nextIntBetweenInclusive(-3, 3);
                int dz = this.clone.getRandom().nextIntBetweenInclusive(-3, 3);
                int dy = this.clone.getRandom().nextIntBetweenInclusive(-1, 1);
                net.minecraft.core.BlockPos targetPos = owner.blockPosition().offset(dx, dy, dz);
                if (this.clone.level().getBlockState(targetPos).isAir()
                        && this.clone.level().getBlockState(targetPos.above()).isAir()
                        && !this.clone.level().getBlockState(targetPos.below()).isAir()) {
                    this.clone.moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, this.clone.getYRot(), this.clone.getXRot());
                    this.clone.getNavigation().stop();
                    return;
                }
            }
        }
    }

    public static class CloneOwnerHurtByTargetGoal extends TargetGoal {
        private final ShadowCloneEntity clone;
        private LivingEntity ownerLastHurtBy;
        private int timestamp;

        public CloneOwnerHurtByTargetGoal(ShadowCloneEntity clone) {
            super(clone, false);
            this.clone = clone;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (this.clone.getCommandMode() == MODE_HOLD && this.clone.getOwner() != null && this.clone.distanceToSqr(this.clone.getOwner()) > 100.0D) {
                return false;
            }
            Player owner = this.clone.getOwner();
            if (owner == null) {
                return false;
            }
            this.ownerLastHurtBy = owner.getLastHurtByMob();
            int i = owner.getLastHurtByMobTimestamp();
            return i != this.timestamp && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT) && !this.ownerLastHurtBy.getUUID().equals(owner.getUUID());
        }

        @Override
        public void start() {
            this.mob.setTarget(this.ownerLastHurtBy);
            Player owner = this.clone.getOwner();
            if (owner != null) {
                this.timestamp = owner.getLastHurtByMobTimestamp();
            }
            super.start();
        }
    }

    public static class CloneOwnerHurtTargetGoal extends TargetGoal {
        private final ShadowCloneEntity clone;
        private LivingEntity ownerLastHurt;
        private int timestamp;

        public CloneOwnerHurtTargetGoal(ShadowCloneEntity clone) {
            super(clone, false);
            this.clone = clone;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (this.clone.getCommandMode() == MODE_HOLD && this.clone.getOwner() != null && this.clone.distanceToSqr(this.clone.getOwner()) > 100.0D) {
                return false;
            }
            Player owner = this.clone.getOwner();
            if (owner == null) {
                return false;
            }
            this.ownerLastHurt = owner.getLastHurtMob();
            int i = owner.getLastHurtMobTimestamp();
            return i != this.timestamp && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT) && !this.ownerLastHurt.getUUID().equals(owner.getUUID());
        }

        @Override
        public void start() {
            this.mob.setTarget(this.ownerLastHurt);
            Player owner = this.clone.getOwner();
            if (owner != null) {
                this.timestamp = owner.getLastHurtMobTimestamp();
            }
            super.start();
        }
    }
}
