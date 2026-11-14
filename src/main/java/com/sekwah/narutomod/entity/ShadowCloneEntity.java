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
    private final java.util.List<net.minecraft.resources.ResourceLocation> unlockedCloneJutsus = new java.util.ArrayList<>();
    private int cloneJutsuCooldown = 0;
    private boolean fastJutsuCooldown = false;

    public ShadowCloneEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public void setUnlockedCloneJutsus(java.util.List<net.minecraft.resources.ResourceLocation> jutsus) {
        this.unlockedCloneJutsus.clear();
        if (jutsus != null) {
            this.unlockedCloneJutsus.addAll(jutsus);
        }
    }

    public java.util.List<net.minecraft.resources.ResourceLocation> getUnlockedCloneJutsus() {
        return this.unlockedCloneJutsus;
    }

    public void setFastJutsuCooldown(boolean fast) {
        this.fastJutsuCooldown = fast;
    }

    private static final EntityDataAccessor<String> DATA_ACTIVE_ABILITIES =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SHARINGAN_STAGE =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HACHIMON_GATE =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
        this.entityData.define(DATA_OWNER_NAME, "");
        this.entityData.define(DATA_MAX_LIFESPAN, 600);
        this.entityData.define(DATA_IS_SLIM, false);
        this.entityData.define(DATA_COMMAND_MODE, MODE_FOLLOW);
        this.entityData.define(DATA_ACTIVE_ABILITIES, "");
        this.entityData.define(DATA_SHARINGAN_STAGE, 0);
        this.entityData.define(DATA_HACHIMON_GATE, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D) // 2 hearts base health
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(2, new CloneJutsuAttackGoal(this));
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
            if (this.cloneJutsuCooldown > 0) {
                this.cloneJutsuCooldown--;
            }
            this.lifeTicks--;
            if (this.lifeTicks <= 0) {
                this.poof();
            }
        }
    }

    @Nullable
    public static UUID resolveRootOwnerUUID(@Nullable net.minecraft.world.entity.Entity entity) {
        if (entity == null) return null;
        if (entity instanceof Player player) {
            return player.getUUID();
        }
        if (entity instanceof ShadowCloneEntity clone) {
            return clone.getOwnerUUID();
        }
        if (entity instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() != null) {
            return resolveRootOwnerUUID(projectile.getOwner());
        }
        return null;
    }

    public static boolean areAllies(@Nullable net.minecraft.world.entity.Entity a, @Nullable net.minecraft.world.entity.Entity b) {
        if (a == null || b == null) return false;
        if (a == b || a.getUUID().equals(b.getUUID())) return true;
        UUID ownerA = resolveRootOwnerUUID(a);
        UUID ownerB = resolveRootOwnerUUID(b);
        return ownerA != null && ownerA.equals(ownerB);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (areAllies(this, source.getEntity()) || areAllies(this, source.getDirectEntity())) {
            return false;
        }
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide && this.getHealth() <= 0.0F) {
            this.poof();
        }
        return result;
    }

    public void copyActiveAbilitiesFrom(Player owner, com.sekwah.narutomod.capabilities.INinjaData ninjaData) {
        if (ninjaData == null) return;
        java.util.Set<net.minecraft.resources.ResourceLocation> toggles = ninjaData.getToggleAbilityData().getAbilitiesHashSet();
        String joined = toggles.stream()
                .map(net.minecraft.resources.ResourceLocation::toString)
                .collect(java.util.stream.Collectors.joining(","));
        this.entityData.set(DATA_ACTIVE_ABILITIES, joined);

        if (ninjaData.isSharinganActive()) {
            this.entityData.set(DATA_SHARINGAN_STAGE,
                    com.sekwah.narutomod.abilities.jutsus.SharinganAbility.getStage(ninjaData).ordinal() + 1);
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 1200, 0, false, false));
        }
        if (hasActiveAbility("susanoo_ribcage")) {
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 1200, 1, false, false));
        }
        if (hasActiveAbility("eight_gates")) {
            int gate = com.sekwah.narutomod.abilities.jutsus.EightGatesAbility.getStage(ninjaData).ordinal() + 1;
            this.entityData.set(DATA_HACHIMON_GATE, gate);
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 1200, Math.min(2, gate / 3), false, false));
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 1200, Math.min(2, gate / 3), false, false));
        }
    }

    public boolean hasActiveAbility(String abilityPath) {
        String data = this.entityData.get(DATA_ACTIVE_ABILITIES);
        if (data == null || data.isEmpty()) return false;
        for (String token : data.split(",")) {
            if (token.equals(abilityPath) || token.endsWith(":" + abilityPath)) {
                return true;
            }
        }
        return false;
    }

    public int getSharinganStage() {
        return this.entityData.get(DATA_SHARINGAN_STAGE);
    }

    public int getHachimonGate() {
        return this.entityData.get(DATA_HACHIMON_GATE);
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

        private static final double START_FOLLOW_DIST_SQ = 36.0D; // 6.0 blocks
        private static final double STOP_FOLLOW_DIST_SQ = 9.0D;   // 3.0 blocks
        private static final double TELEPORT_DIST_SQ = 900.0D;   // 30.0 blocks

        @Override
        public boolean canUse() {
            if (this.clone.getCommandMode() != MODE_FOLLOW) {
                return false;
            }
            Player owner = this.clone.getOwner();
            if (owner == null || owner.isSpectator() || !owner.isAlive()) {
                return false;
            }
            return this.clone.distanceToSqr(owner) > START_FOLLOW_DIST_SQ;
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
            return this.clone.distanceToSqr(owner) > STOP_FOLLOW_DIST_SQ;
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
            // Teleport if too far (> 30 blocks)
            if (distSq > TELEPORT_DIST_SQ && owner.onGround()) {
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

    public static class CloneJutsuAttackGoal extends Goal {
        private static final float CLONE_JUTSU_DAMAGE_MULTIPLIER = 0.65F;
        private static final int NORMAL_COOLDOWN_TICKS = 120; // 6 seconds
        private static final int FAST_COOLDOWN_TICKS = 80;    // 4 seconds (600+ mastery)

        private final ShadowCloneEntity clone;

        public CloneJutsuAttackGoal(ShadowCloneEntity clone) {
            this.clone = clone;
        }

        @Override
        public boolean canUse() {
            if (this.clone.unlockedCloneJutsus.isEmpty()) return false;
            if (this.clone.cloneJutsuCooldown > 0) return false;
            LivingEntity target = this.clone.getTarget();
            return target != null && target.isAlive() && this.clone.hasLineOfSight(target);
        }

        @Override
        public void start() {
            LivingEntity target = this.clone.getTarget();
            if (target == null || !(this.clone.level() instanceof ServerLevel serverLevel)) return;

            double dist = this.clone.distanceTo(target);
            java.util.List<net.minecraft.resources.ResourceLocation> candidates = new java.util.ArrayList<>();
            for (net.minecraft.resources.ResourceLocation id : this.clone.unlockedCloneJutsus) {
                String path = id.getPath();
                if ("fireball".equals(path) && dist >= 6.0D && dist <= 20.0D) candidates.add(id);
                else if ("water_bullet".equals(path) && dist >= 5.0D && dist <= 20.0D) candidates.add(id);
                else if ("wind_breaker".equals(path) && dist >= 3.5D && dist <= 12.0D) candidates.add(id);
                else if (("rasengan".equals(path) || "chidori".equals(path)) && dist <= 4.5D) candidates.add(id);
            }

            if (candidates.isEmpty()) return;

            net.minecraft.resources.ResourceLocation chosen = candidates.get(this.clone.getRandom().nextInt(candidates.size()));
            this.clone.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.clone.swing(InteractionHand.MAIN_HAND);
            serverLevel.playSound(null, this.clone.blockPosition(), NarutoSounds.JUTSU_CAST.get(), SoundSource.PLAYERS, 0.9F, 1.1F);

            net.minecraft.world.phys.Vec3 dir = target.getEyePosition().subtract(this.clone.getEyePosition()).normalize();
            String path = chosen.getPath();

            if ("fireball".equals(path)) {
                com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity fireball =
                        new com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity(this.clone, dir.x, dir.y, dir.z);
                net.minecraft.world.phys.Vec3 spawnPos = this.clone.getEyePosition().add(dir.scale(1.3));
                fireball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                serverLevel.addFreshEntity(fireball);
            } else if ("water_bullet".equals(path)) {
                com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity bullet =
                        new com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity(serverLevel, this.clone);
                net.minecraft.world.phys.Vec3 spawnPos = this.clone.getEyePosition().add(dir.scale(1.3));
                bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                bullet.setDamage(10.0F * CLONE_JUTSU_DAMAGE_MULTIPLIER);
                bullet.setMaxLife(25);
                bullet.setDeltaMovement(dir.scale(1.1));
                serverLevel.addFreshEntity(bullet);
            } else if ("wind_breaker".equals(path)) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0, target.getZ(), 18, 0.5, 0.5, 0.5, 0.15);
                target.hurt(this.clone.damageSources().mobAttack(this.clone), 8.0F * CLONE_JUTSU_DAMAGE_MULTIPLIER);
                target.setDeltaMovement(dir.scale(1.6).add(0, 0.35, 0));
            } else if ("rasengan".equals(path)) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.3, 0.3, 0.3, 0.15);
                target.hurt(this.clone.damageSources().mobAttack(this.clone), 14.0F * CLONE_JUTSU_DAMAGE_MULTIPLIER);
                serverLevel.playSound(null, target.blockPosition(), NarutoSounds.RASENGAN.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            } else if ("chidori".equals(path)) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 24, 0.4, 0.4, 0.4, 0.2);
                target.hurt(this.clone.damageSources().mobAttack(this.clone), 14.0F * CLONE_JUTSU_DAMAGE_MULTIPLIER);
                serverLevel.playSound(null, target.blockPosition(), NarutoSounds.CHIDORI.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            }

            this.clone.cloneJutsuCooldown = this.clone.fastJutsuCooldown ? FAST_COOLDOWN_TICKS : NORMAL_COOLDOWN_TICKS;
        }
    }
}
