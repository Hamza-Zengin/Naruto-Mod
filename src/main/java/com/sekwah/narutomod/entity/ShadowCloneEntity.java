package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.entity.ai.ShadowCloneGoals.CloneFollowOwnerGoal;
import com.sekwah.narutomod.entity.ai.ShadowCloneGoals.CloneJutsuAttackGoal;
import com.sekwah.narutomod.entity.ai.ShadowCloneGoals.CloneOwnerHurtByTargetGoal;
import com.sekwah.narutomod.entity.ai.ShadowCloneGoals.CloneOwnerHurtTargetGoal;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class ShadowCloneEntity extends PathfinderMob {

    public static final int MODE_FOLLOW = 0;
    public static final int MODE_PATROL = 1;
    public static final int MODE_HOLD = 2;

    private static final EntityDataAccessor<Optional<net.minecraft.world.entity.EntityReference<LivingEntity>>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
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
    private final java.util.List<Identifier> unlockedCloneJutsus = new java.util.ArrayList<>();
    private int cloneJutsuCooldown = 0;
    private boolean fastJutsuCooldown = false;
    private boolean decoyMode = false;

    public void setDecoyMode(boolean decoyMode) {
        this.decoyMode = decoyMode;
        if (decoyMode) {
            this.setNoAi(true);
        }
    }

    public boolean isDecoyMode() {
        return this.decoyMode;
    }

    public ShadowCloneEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public void setUnlockedCloneJutsus(java.util.List<Identifier> jutsus) {
        this.unlockedCloneJutsus.clear();
        if (jutsus != null) {
            this.unlockedCloneJutsus.addAll(jutsus);
        }
    }

    public java.util.List<Identifier> getUnlockedCloneJutsus() {
        return this.unlockedCloneJutsus;
    }

    public void setFastJutsuCooldown(boolean fast) {
        this.fastJutsuCooldown = fast;
    }

    public boolean isFastJutsuCooldown() {
        return this.fastJutsuCooldown;
    }

    public int getCloneJutsuCooldown() {
        return this.cloneJutsuCooldown;
    }

    public void setCloneJutsuCooldown(int ticks) {
        this.cloneJutsuCooldown = ticks;
    }

    private static final EntityDataAccessor<String> DATA_ACTIVE_ABILITIES =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SHARINGAN_STAGE =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HACHIMON_GATE =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_ACTIVE_HAND_JUTSU =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.STRING);

    private int activeHandJutsuTicks = 0;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_OWNER_NAME, "");
        builder.define(DATA_MAX_LIFESPAN, 600);
        builder.define(DATA_IS_SLIM, false);
        builder.define(DATA_COMMAND_MODE, MODE_FOLLOW);
        builder.define(DATA_ACTIVE_ABILITIES, "");
        builder.define(DATA_SHARINGAN_STAGE, 0);
        builder.define(DATA_HACHIMON_GATE, 0);
        builder.define(DATA_ACTIVE_HAND_JUTSU, "");
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
                (target, serverLevel) -> target instanceof Enemy && !(target instanceof ShadowCloneEntity) && (this.ownerUUID == null || !target.getUUID().equals(this.ownerUUID))));
    }

    public void setOwner(Player player) {
        if (player != null) {
            this.ownerUUID = player.getUUID();
            this.entityData.set(DATA_OWNER_UUID, Optional.of(net.minecraft.world.entity.EntityReference.of(player)));
            String name = player.getGameProfile().name();
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
        return this.entityData.get(DATA_OWNER_UUID).map(net.minecraft.world.entity.EntityReference::getUUID).orElse(this.ownerUUID);
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
            if (!this.level().isClientSide()) {
                int nextMode = (this.getCommandMode() + 1) % 3;
                java.util.List<ShadowCloneEntity> clones = this.level().getEntitiesOfClass(
                        ShadowCloneEntity.class,
                        player.getBoundingBox().inflate(64.0D),
                        c -> player.getUUID().equals(c.getOwnerUUID())
                );
                for (ShadowCloneEntity clone : clones) {
                    clone.setCommandMode(nextMode);
                }

                String modeKey = switch (nextMode) {
                    case MODE_PATROL -> "naruto.clone.mode.patrol";
                    case MODE_HOLD -> "naruto.clone.mode.hold";
                    default -> "naruto.clone.mode.follow";
                };

                player.sendOverlayMessage(
                        net.minecraft.network.chat.Component.translatable("naruto.clone.order",
                                net.minecraft.network.chat.Component.translatable(modeKey))
                );
                this.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        UUID owner = this.getOwnerUUID();
        if (owner != null) {
            output.store("Owner", UUIDUtil.CODEC, owner);
        }
        output.putString("OwnerName", this.getOwnerName());
        output.putInt("LifeTicks", this.lifeTicks);
        output.putInt("MaxLifespan", this.getMaxLifespan());
        output.putBoolean("IsSlim", this.isSlim());
        output.putInt("CommandMode", this.getCommandMode());
        output.putString("ActiveAbilities", this.entityData.get(DATA_ACTIVE_ABILITIES));
        output.putInt("SharinganStage", this.getSharinganStage());
        output.putInt("HachimonGate", this.getHachimonGate());
        output.putString("ActiveHandJutsu", this.getActiveHandJutsu());
        output.putInt("ActiveHandJutsuTicks", this.activeHandJutsuTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("Owner", UUIDUtil.CODEC).ifPresent(uuid -> {
            this.ownerUUID = uuid;
            this.entityData.set(DATA_OWNER_UUID, Optional.of(net.minecraft.world.entity.EntityReference.of(uuid)));
        });
        this.entityData.set(DATA_OWNER_NAME, input.getStringOr("OwnerName", ""));
        this.lifeTicks = input.getIntOr("LifeTicks", 600);
        this.entityData.set(DATA_MAX_LIFESPAN, input.getIntOr("MaxLifespan", 600));
        this.entityData.set(DATA_IS_SLIM, input.getBooleanOr("IsSlim", false));
        this.setCommandMode(input.getIntOr("CommandMode", MODE_FOLLOW));
        this.entityData.set(DATA_ACTIVE_ABILITIES, input.getStringOr("ActiveAbilities", ""));
        this.entityData.set(DATA_SHARINGAN_STAGE, input.getIntOr("SharinganStage", 0));
        this.entityData.set(DATA_HACHIMON_GATE, input.getIntOr("HachimonGate", 0));
        this.entityData.set(DATA_ACTIVE_HAND_JUTSU, input.getStringOr("ActiveHandJutsu", ""));
        this.activeHandJutsuTicks = input.getIntOr("ActiveHandJutsuTicks", 0);
        if (this.hasActiveAbility("water_walk")) {
            this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, 0.0F);
        }
    }

    @Override
    public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluidState) {
        if (this.hasActiveAbility("water_walk")
                && (fluidState.is(net.minecraft.world.level.material.Fluids.WATER)
                || fluidState.is(net.minecraft.world.level.material.Fluids.FLOWING_WATER))) {
            return true;
        }
        return super.canStandOnFluid(fluidState);
    }

    private boolean isWaterBlock(net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.material.FluidState fluidState = this.level().getFluidState(pos);
        return fluidState.is(net.minecraft.world.level.material.Fluids.WATER)
                || fluidState.is(net.minecraft.world.level.material.Fluids.FLOWING_WATER);
    }

    private void updateWaterWalkMovement() {
        int blockX = (int) Math.floor(this.getX());
        int blockZ = (int) Math.floor(this.getZ());
        net.minecraft.core.BlockPos.MutableBlockPos mutablePos = new net.minecraft.core.BlockPos.MutableBlockPos();

        boolean steadyCheck = isWaterBlock(mutablePos.set(blockX, (int) Math.round(this.getY() - 0.56f), blockZ));
        boolean pushUpFast = isWaterBlock(mutablePos.set(blockX, (int) Math.round(this.getY()), blockZ));
        boolean pushUpNormal = isWaterBlock(mutablePos.set(blockX, (int) Math.round(this.getY() - 0.47f), blockZ));

        if (!steadyCheck && !pushUpFast && !pushUpNormal) {
            return;
        }

        net.minecraft.world.phys.Vec3 vec = this.getDeltaMovement();
        double resultingYSpeed = vec.y();
        if (pushUpFast) {
            resultingYSpeed = Math.min(0.6D, resultingYSpeed + 0.2D);
        } else if (pushUpNormal) {
            resultingYSpeed = Math.min(0.2D, resultingYSpeed + 0.1D);
        } else if (steadyCheck && resultingYSpeed < 0.0D) {
            resultingYSpeed = 0.0D;
            this.resetFallDistance();
            this.setOnGround(true);
        }
        this.setDeltaMovement(vec.x(), resultingYSpeed, vec.z());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.hasActiveAbility("water_walk")) {
            this.updateWaterWalkMovement();
        }
        if (!this.level().isClientSide()) {
            if (this.cloneJutsuCooldown > 0) {
                this.cloneJutsuCooldown--;
            }
            if (this.activeHandJutsuTicks > 0) {
                this.activeHandJutsuTicks--;
                if (this.activeHandJutsuTicks == 0) {
                    this.entityData.set(DATA_ACTIVE_HAND_JUTSU, "");
                }
            }
            if (this.tickCount % 20 == 0 && this.level() instanceof ServerLevel serverLevel) {
                applyPeriodicAbilityEffects(serverLevel);
            }
            this.lifeTicks--;
            if (this.lifeTicks <= 0) {
                this.poof();
            }
        }
    }

    private void applyPeriodicAbilityEffects(ServerLevel serverLevel) {
        if (this.hasActiveAbility("byakugan")) {
            net.minecraft.world.phys.AABB field = this.getBoundingBox().inflate(24.0D);
            java.util.List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, field,
                    e -> e != this && e.isAlive() && !areAllies(this, e));
            for (LivingEntity entity : nearby) {
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, 30, 0, false, false, false));
            }
        }
        int sharinganStage = this.getSharinganStage();
        if (this.hasActiveAbility("sharingan") && sharinganStage > 0) {
            double radius = 6.0D + Math.max(0, sharinganStage - 1) * 2.0D;
            int slowAmp = sharinganStage >= 3 ? 1 : 0;
            boolean applyWeakness = sharinganStage == 2 || sharinganStage >= 4;
            net.minecraft.world.phys.AABB field = this.getBoundingBox().inflate(radius);
            java.util.List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, field,
                    e -> e != this && e.isAlive() && !areAllies(this, e));
            for (LivingEntity enemy : enemies) {
                enemy.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.SLOWNESS, 40, slowAmp, false, false));
                if (applyWeakness) {
                    enemy.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.WEAKNESS, 40, 0, false, false));
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(serverLevel, target);
        if (hit && this.hasActiveAbility("byakugan") && target instanceof LivingEntity livingTarget && !areAllies(this, livingTarget)) {
            livingTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0, false, false));
            livingTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SLOWNESS, 40, 0, false, false));
            if (livingTarget instanceof Player targetPlayer) {
                targetPlayer.getData(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA)
                        .lockChakraRegen(com.sekwah.narutomod.abilities.jutsus.ByakuganAbility.CHAKRA_LOCK_TICKS);
            }
        }
        return hit;
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
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        if (areAllies(this, source.getEntity()) || areAllies(this, source.getDirectEntity())) {
            return false;
        }
        boolean result = super.hurtServer(serverLevel, source, amount);
        if (result && this.getHealth() <= 0.0F) {
            this.poof();
        }
        return result;
    }

    public void copyActiveAbilitiesFrom(Player owner, com.sekwah.narutomod.capabilities.INinjaData ninjaData) {
        if (ninjaData == null) return;
        java.util.Set<String> activeIds = new java.util.LinkedHashSet<>();
        com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData toggleData = ninjaData.getToggleAbilityData();
        if (toggleData != null) {
            for (Identifier id : toggleData.getAbilitiesSnapshot()) {
                if (id != null) {
                    activeIds.add(id.toString());
                }
            }
        }
        if (ninjaData.isSharinganActive()) activeIds.add(com.sekwah.narutomod.NarutoMod.MOD_ID + ":sharingan");
        if (ninjaData.isByakuganActive()) activeIds.add(com.sekwah.narutomod.NarutoMod.MOD_ID + ":byakugan");
        if (ninjaData.isEightGatesActive()) activeIds.add(com.sekwah.narutomod.NarutoMod.MOD_ID + ":eight_gates");
        if (ninjaData.isEighthGateActive()) activeIds.add(com.sekwah.narutomod.NarutoMod.MOD_ID + ":eight_gates_death");
        if (ninjaData.isWaterWalkActive()) activeIds.add(com.sekwah.narutomod.NarutoMod.MOD_ID + ":water_walk");

        this.entityData.set(DATA_ACTIVE_ABILITIES, String.join(",", activeIds));

        if (ninjaData.getActiveHandJutsuTicks() > 0 && ninjaData.getActiveHandJutsu() != null && !ninjaData.getActiveHandJutsu().isEmpty()) {
            this.setActiveHandJutsu(ninjaData.getActiveHandJutsu(), ninjaData.getActiveHandJutsuTicks());
        } else {
            this.setActiveHandJutsu("", 0);
        }

        int lifespan = Math.max(1200, this.getMaxLifespan());

        if (hasActiveAbility("water_walk")) {
            this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, 0.0F);
        }

        if (hasActiveAbility("sharingan")) {
            int stage = com.sekwah.narutomod.abilities.jutsus.SharinganAbility.getStage(ninjaData).ordinal() + 1;
            this.entityData.set(DATA_SHARINGAN_STAGE, stage);
            int speedAmp = stage >= 3 ? 1 : 0;
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SPEED, lifespan, speedAmp, false, false));
            if (stage >= 3) {
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.STRENGTH, lifespan, 0, false, false));
            }
            if (stage >= 4) {
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.RESISTANCE, lifespan, 0, false, false));
            }
        } else {
            this.entityData.set(DATA_SHARINGAN_STAGE, 0);
        }

        if (hasActiveAbility("susanoo_ribcage")) {
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.RESISTANCE, lifespan, 1, false, false));
        }

        if (hasActiveAbility("eight_gates_death")) {
            this.entityData.set(DATA_HACHIMON_GATE, 8);
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.STRENGTH, lifespan, 3, false, false));
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SPEED, lifespan, 3, false, false));
        } else if (hasActiveAbility("eight_gates")) {
            int gate = com.sekwah.narutomod.abilities.jutsus.EightGatesAbility.getStage(ninjaData).ordinal() + 1;
            this.entityData.set(DATA_HACHIMON_GATE, gate);
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.STRENGTH, lifespan, Math.min(2, gate / 3), false, false));
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SPEED, lifespan, Math.min(2, gate / 3), false, false));
        } else {
            this.entityData.set(DATA_HACHIMON_GATE, 0);
        }
    }

    public boolean hasActiveAbility(String abilityPath) {
        if (abilityPath == null || abilityPath.isEmpty()) return false;
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

    public String getActiveHandJutsu() {
        String val = this.entityData.get(DATA_ACTIVE_HAND_JUTSU);
        return val != null ? val : "";
    }

    public void setActiveHandJutsu(String jutsu, int ticks) {
        this.entityData.set(DATA_ACTIVE_HAND_JUTSU, jutsu != null ? jutsu : "");
        this.activeHandJutsuTicks = Math.max(0, ticks);
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel serverLevel, DamageSource damageSource) {
        // Shadow clones vanish into thin smoke and never drop copied gear
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean recentlyHit) {
        // Anti-dupe protection
    }

    @Override
    protected void dropEquipment(ServerLevel serverLevel) {
        // Anti-dupe protection
    }

    public void poof() {
        if (!this.decoyMode && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.8, this.getZ(),
                    25, 0.4, 0.5, 0.4, 0.05, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.playSound(null, this.blockPosition(), NarutoSounds.CLONE_POOF.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        this.discard();
    }
}
