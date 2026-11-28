package com.sekwah.narutomod.entity.mob;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.advancements.NinjaProgressTrigger;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.MissionDataManager;
import com.sekwah.narutomod.capabilities.MissionDefinition;
import com.sekwah.narutomod.capabilities.MissionLogic;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaMission;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.ai.NinjaJutsuAttackGoal;
import com.sekwah.narutomod.entity.ai.NinjaJutsuCooldowns;
import com.sekwah.narutomod.entity.ai.NinjaJutsuProfile;
import com.sekwah.narutomod.entity.ai.NinjaJutsuUser;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.registries.NarutoRegistries;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Toad Sage Trial Boss (Kurbağa Bilge Sınavı Bossu)
 * Waits peacefully at the Myōbokuzan Shrine until challenged via right-click or attack.
 * Phase 1 uses Water/Wind jutsus, melee, and toad leap-slams; Phase 2 (<50% HP) unleashes
 * Nature Energy bursts, self-buffs, and periodic knockback shockwaves.
 * Defeating this boss unlocks {@code narutomod:sage_mode} for participating duelists.
 * Entity drops are defined in {@code data/narutomod/loot_table/entities/sage_trial_boss.json}.
 */
public class SageTrialBossEntity extends PathfinderMob implements NinjaJutsuUser {

    public static final Identifier SAGE_MODE_ID = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "sage_mode");
    public static final double BOSS_PARTICIPATION_RADIUS = 32.0D;
    private static final int LEAP_ANIM_DURATION = 18;

    private static final EntityDataAccessor<Boolean> DATA_DUEL_ACTIVE =
            SynchedEntityData.defineId(SageTrialBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO =
            SynchedEntityData.defineId(SageTrialBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_LEAP_TICKS =
            SynchedEntityData.defineId(SageTrialBossEntity.class, EntityDataSerializers.INT);

    @Nullable
    private static BiConsumer<ServerPlayer, INinjaData> bossDefeatMissionHook;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            Component.translatable("entity.narutomod.sage_trial_boss"),
            BossEvent.BossBarColor.GREEN,
            BossEvent.BossBarOverlay.NOTCHED_10);

    private final Set<ServerPlayer> trackingPlayers = new HashSet<>();
    private final Set<UUID> participantUuids = new HashSet<>();
    private final NinjaJutsuCooldowns jutsuCooldowns = new NinjaJutsuCooldowns(26);
    private int dashCooldown = 50;
    private int shockwaveCooldown = 90;
    private int leapAirTicks = 0;
    private int prevLeapTicks = 0;
    private boolean leapSlamArmed = false;
    private boolean duelActive = false;
    private boolean phaseTwoTriggered = false;

    public SageTrialBossEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 200;
        this.bossEvent.setVisible(false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DUEL_ACTIVE, false);
        builder.define(DATA_PHASE_TWO, false);
        builder.define(DATA_LEAP_TICKS, 0);
    }

    public boolean isDuelActive() {
        return this.duelActive || this.entityData.get(DATA_DUEL_ACTIVE);
    }

    public boolean isPhaseTwo() {
        return this.phaseTwoTriggered || this.entityData.get(DATA_PHASE_TWO);
    }

    public float getLeapAnimation(float partialTick) {
        int current = this.entityData.get(DATA_LEAP_TICKS);
        if (current <= 0 && this.prevLeapTicks <= 0) {
            return 0.0F;
        }
        float interpolated = Mth.lerp(partialTick, (float) this.prevLeapTicks, (float) current);
        return Mth.clamp(1.0F - (interpolated / (float) LEAP_ANIM_DURATION), 0.0F, 1.0F);
    }

    public static void setBossDefeatMissionHook(@Nullable BiConsumer<ServerPlayer, INinjaData> hook) {
        bossDefeatMissionHook = hook;
    }

    /**
     * Advances active DEFEAT_BOSS mission progress and invokes optional Stage 2 hook.
     */
    public static void notifyBossDefeatMissionProgress(ServerPlayer player, NinjaData ninjaData) {
        if (player == null || ninjaData == null) {
            return;
        }
        if (bossDefeatMissionHook != null) {
            bossDefeatMissionHook.accept(player, ninjaData);
            return;
        }
        if (NinjaMission.MissionStatus.fromString(ninjaData.getMissionStatus()) != NinjaMission.MissionStatus.IN_PROGRESS) {
            return;
        }
        MissionDefinition active = MissionDataManager.get(ninjaData.getActiveMissionId());
        if (active != null && active.targetType() == NinjaMission.TargetType.DEFEAT_BOSS) {
            boolean justCompleted = MissionLogic.incrementMissionProgress(ninjaData, 1);
            if (justCompleted) {
                player.sendSystemMessage(Component.translatable("naruto.mission.completed", active.getTitleComponent())
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
        }
    }

    public static NinjaJutsuProfile createSageTrialProfile() {
        return new NinjaJutsuProfile.Builder()
                .rangedDistances(7.0D, 24.0D)
                .closeThreatDistance(6.5D)
                .criticalHealthRatio(0.35F)
                .globalCooldownTicks(26)
                .handSealWindupTicks(8)
                .waterBullet(true, 60, 20, 11.5F, 36, 1.35D)
                .vacuumSpheres(true, 65, 20, 6.5F, 32, 1.60D)
                .kawarimi(true, 130, 5.5D, 75, 1)
                .build();
    }

    @Override
    public NinjaJutsuCooldowns getJutsuCooldowns() {
        return this.jutsuCooldowns;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 260.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.60D)
                .add(Attributes.FOLLOW_RANGE, 38.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new NinjaJutsuAttackGoal<>(this, createSageTrialProfile()));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.35D, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this,
                VillageNinjaEntity.class, ShinobiVillagerEntity.class, ChuninChampionEntity.class,
                SageTrialBossEntity.class, AbstractVillager.class, AbstractGolem.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (target, serverLevel) -> this.duelActive));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AkatsukiNinjaEntity.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, RogueNinjaEntity.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                (target, serverLevel) -> isValidHostileTarget(target)
                        && !(target instanceof RogueNinjaEntity)
                        && !(target instanceof AkatsukiNinjaEntity)));
    }

    public static boolean isProtectedFriendly(@Nullable Entity target) {
        return target instanceof VillageNinjaEntity
                || target instanceof ShinobiVillagerEntity
                || target instanceof ChuninChampionEntity
                || target instanceof SageTrialBossEntity
                || target instanceof AbstractVillager
                || target instanceof AbstractGolem;
    }

    public static boolean isProtectedFriendlyClass(@Nullable Class<?> targetClass) {
        if (targetClass == null) {
            return false;
        }
        return VillageNinjaEntity.class.isAssignableFrom(targetClass)
                || ShinobiVillagerEntity.class.isAssignableFrom(targetClass)
                || ChuninChampionEntity.class.isAssignableFrom(targetClass)
                || SageTrialBossEntity.class.isAssignableFrom(targetClass)
                || AbstractVillager.class.isAssignableFrom(targetClass)
                || AbstractGolem.class.isAssignableFrom(targetClass);
    }

    public static boolean isValidHostileTarget(@Nullable LivingEntity target) {
        return target != null
                && !isProtectedFriendly(target)
                && !(target instanceof Player)
                && !(target instanceof ShadowCloneEntity);
    }

    public static boolean canAttackTargetClass(@Nullable Class<?> targetClass, boolean duelActive) {
        if (targetClass == null || isProtectedFriendlyClass(targetClass)) {
            return false;
        }
        if (Player.class.isAssignableFrom(targetClass) || ShadowCloneEntity.class.isAssignableFrom(targetClass)) {
            return duelActive;
        }
        return true;
    }

    public static boolean shouldClearTargetOutsideDuel(@Nullable LivingEntity currentTarget, boolean duelActive) {
        return !duelActive && (currentTarget instanceof Player || currentTarget instanceof ShadowCloneEntity);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (isProtectedFriendly(target)) {
            return false;
        }
        if (target instanceof Player || target instanceof ShadowCloneEntity) {
            return this.duelActive && super.canAttack(target);
        }
        return super.canAttack(target);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        this.setPersistenceRequired();
        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.equipDefaultGear();
    }

    private void equipDefaultGear() {
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(NarutoItems.SAGE_CLOAK.get()));
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.duelActive) {
            if (!this.level().isClientSide()) {
                this.activateDuel(player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        boolean result = super.hurtServer(serverLevel, source, amount);
        if (result && source.getEntity() instanceof Player player) {
            this.participantUuids.add(player.getUUID());
            if (!this.duelActive) {
                this.activateDuel(player);
            }
        }
        return result;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FROG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FROG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FROG_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F + (this.random.nextFloat() - 0.5F) * 0.08F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 140;
    }

    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, Entity target) {
        boolean hit = super.doHurtTarget(serverLevel, target);
        if (hit) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.HOSTILE, 1.0F, 0.72F);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.FROG_STEP,
                    SoundSource.HOSTILE, 0.9F, 0.55F);
        }
        return hit;
    }

    public void activateDuel(Player challenger) {
        if (challenger != null) {
            this.participantUuids.add(challenger.getUUID());
        }
        if (this.duelActive) {
            return;
        }
        this.duelActive = true;
        this.entityData.set(DATA_DUEL_ACTIVE, true);
        this.bossEvent.setVisible(true);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (challenger != null) {
            this.setTarget(challenger);
        }

        for (ServerPlayer sp : this.trackingPlayers) {
            this.bossEvent.addPlayer(sp);
        }
        if (challenger instanceof ServerPlayer sp) {
            this.bossEvent.addPlayer(sp);
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.FROG_AMBIENT,
                    SoundSource.HOSTILE, 1.35F, 0.52F);
            serverLevel.playSound(null, this.blockPosition(), NarutoSounds.JUTSU_CAST.get(),
                    SoundSource.HOSTILE, 1.1F, 0.90F);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.0D, this.getZ(),
                    20, 0.55D, 0.6D, 0.55D, 0.05D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.1D, this.getZ(),
                    14, 0.45D, 0.5D, 0.45D, 0.04D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }

        Component msg = Component.translatable("entity.narutomod.sage_trial_boss.challenge")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        for (ServerPlayer sp : this.trackingPlayers) {
            sp.sendSystemMessage(msg);
        }
        if (challenger instanceof ServerPlayer sp && !this.trackingPlayers.contains(sp)) {
            sp.sendSystemMessage(msg);
        }
    }

    @Override
    public void tick() {
        this.prevLeapTicks = this.entityData.get(DATA_LEAP_TICKS);
        super.tick();

        if (this.level().isClientSide()) {
            this.spawnClientAmbientParticles();
            return;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.tickCount == 1 && this.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
                this.equipDefaultGear();
            }

            int currentLeap = this.entityData.get(DATA_LEAP_TICKS);
            if (currentLeap > 0) {
                this.entityData.set(DATA_LEAP_TICKS, currentLeap - 1);
            }

            if (this.leapSlamArmed) {
                this.leapAirTicks++;
                if (this.onGround() && this.leapAirTicks >= 4) {
                    this.performToadGroundSlam(serverLevel);
                    this.leapSlamArmed = false;
                    this.leapAirTicks = 0;
                } else if (this.leapAirTicks > 40) {
                    this.leapSlamArmed = false;
                    this.leapAirTicks = 0;
                }
            }

            if (shouldClearTargetOutsideDuel(this.getTarget(), this.duelActive)) {
                this.setTarget(null);
            }

            if (this.duelActive) {
                this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
                if (!this.phaseTwoTriggered && this.getHealth() <= this.getMaxHealth() * 0.5F) {
                    this.triggerPhaseTwo(serverLevel);
                }

                if (this.phaseTwoTriggered) {
                    if (this.tickCount % 10 == 0) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                this.getX(), this.getY() + 1.0D, this.getZ(),
                                3, 0.4D, 0.5D, 0.4D, 0.02D,
                                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                        serverLevel.sendParticles(ParticleTypes.END_ROD,
                                this.getX(), this.getY() + 1.0D, this.getZ(),
                                1, 0.3D, 0.4D, 0.3D, 0.01D,
                                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                    }

                    if (this.shockwaveCooldown > 0) {
                        this.shockwaveCooldown--;
                    } else if (this.getTarget() != null && this.getTarget().isAlive()
                            && this.distanceToSqr(this.getTarget()) <= 6.5D * 6.5D) {
                        this.performNatureEnergyShockwave(serverLevel, 5.0D, 6.0F);
                        this.shockwaveCooldown = 90 + this.random.nextInt(30);
                    }
                }
            }

            this.jutsuCooldowns.tick();
            if (this.dashCooldown > 0) {
                this.dashCooldown--;
            }

            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double distSqr = this.distanceToSqr(target);
                if (distSqr >= 20.0D && distSqr <= 90.0D && this.dashCooldown <= 0 && this.onGround()) {
                    this.performNatureDash(serverLevel, target);
                    this.dashCooldown = this.phaseTwoTriggered
                            ? (45 + this.random.nextInt(20))
                            : (75 + this.random.nextInt(25));
                }
            }
        }
    }

    private void spawnClientAmbientParticles() {
        // Subtle kiseru pipe smoke puffs drifting up from the left side of the toad's mouth
        if (this.tickCount % 7 == 0) {
            float yawRad = this.yBodyRot * Mth.DEG_TO_RAD;
            double forwardX = -Mth.sin(yawRad);
            double forwardZ = Mth.cos(yawRad);
            double leftX = Mth.cos(yawRad);
            double leftZ = Mth.sin(yawRad);

            double pipeX = this.getX() + forwardX * 0.95D + leftX * 0.62D;
            double pipeY = this.getY() + 1.56D;
            double pipeZ = this.getZ() + forwardZ * 0.95D + leftZ * 0.62D;

            this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pipeX, pipeY, pipeZ,
                    (this.random.nextDouble() - 0.5D) * 0.01D,
                    0.028D + this.random.nextDouble() * 0.012D,
                    (this.random.nextDouble() - 0.5D) * 0.01D);
        }

        // Phase 2 Senjutsu aura particles swirling around the boss
        if (this.isPhaseTwo() && this.tickCount % 4 == 0) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.75D + this.random.nextDouble() * 0.65D;
            double px = this.getX() + Math.cos(angle) * radius;
            double py = this.getY() + 0.35D + this.random.nextDouble() * 1.45D;
            double pz = this.getZ() + Math.sin(angle) * radius;
            this.level().addParticle(
                    this.random.nextBoolean() ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.END_ROD,
                    px, py, pz, 0.0D, 0.04D, 0.0D);
        }
    }

    private void triggerPhaseTwo(ServerLevel serverLevel) {
        this.phaseTwoTriggered = true;
        this.entityData.set(DATA_PHASE_TWO, true);
        this.dashCooldown = Math.min(this.dashCooldown, 15);
        this.shockwaveCooldown = 85;

        this.addEffect(new MobEffectInstance(MobEffects.SPEED, 800, 1));
        this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 800, 1));

        serverLevel.playSound(null, this.blockPosition(), SoundEvents.FROG_AMBIENT, SoundSource.HOSTILE, 1.45F, 0.48F);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 0.9F, 1.05F);
        serverLevel.playSound(null, this.blockPosition(), NarutoSounds.JUTSU_CAST.get(), SoundSource.HOSTILE, 1.2F, 0.9F);
        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.0D, this.getZ(),
                30, 0.8D, 0.7D, 0.8D, 0.18D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.0D, this.getZ(),
                28, 1.0D, 0.8D, 1.0D, 0.08D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.0D, this.getZ(),
                24, 0.9D, 0.7D, 0.9D, 0.10D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        this.performNatureEnergyShockwave(serverLevel, 5.5D, 7.5F);

        Component msg = Component.translatable("entity.narutomod.sage_trial_boss.phase2")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        for (ServerPlayer sp : this.trackingPlayers) {
            sp.sendSystemMessage(msg);
        }
    }

    private void performNatureEnergyShockwave(ServerLevel serverLevel, double radius, float damage) {
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 0.95F, 1.15F);
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.8D, this.getZ(),
                24, radius * 0.4D, 0.35D, radius * 0.4D, 0.06D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.9D, this.getZ(),
                18, radius * 0.35D, 0.35D, radius * 0.35D, 0.08D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 0.9D, this.getZ(),
                14, radius * 0.3D, 0.35D, radius * 0.3D, 0.12D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        double radiusSq = radius * radius;
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius))) {
            if (entity == this || !entity.isAlive() || entity.isSpectator() || isProtectedFriendly(entity)) {
                continue;
            }
            if (entity instanceof Player && !this.duelActive) {
                continue;
            }
            if (entity.distanceToSqr(this) <= radiusSq) {
                entity.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(this), damage);
                Vec3 diff = entity.position().subtract(this.position());
                Vec3 horizontal = new Vec3(diff.x, 0.0D, diff.z);
                Vec3 push = horizontal.lengthSqr() > 1.0E-5D ? horizontal.normalize() : new Vec3(1.0D, 0.0D, 0.0D);
                entity.push(push.x * 1.45D, 0.45D, push.z * 1.45D);
                entity.needsSync = true;
            }
        }
    }

    private void performNatureDash(ServerLevel serverLevel, LivingEntity target) {
        Vec3 diff = target.position().subtract(this.position());
        Vec3 horizontal = new Vec3(diff.x, 0.0D, diff.z);
        Vec3 dir = horizontal.lengthSqr() > 1.0E-5D ? horizontal.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        this.setDeltaMovement(dir.x * 1.25D, 0.62D, dir.z * 1.25D);
        this.needsSync = true;
        this.leapSlamArmed = true;
        this.leapAirTicks = 0;
        this.entityData.set(DATA_LEAP_TICKS, LEAP_ANIM_DURATION);

        serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.25D, this.getZ(),
                14, 0.55D, 0.2D, 0.55D, 0.05D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.FROG_LONG_JUMP, SoundSource.HOSTILE, 1.3F, 0.55F);
        serverLevel.playSound(null, this.blockPosition(), NarutoSounds.LEAP.get(), SoundSource.HOSTILE, 1.0F, 0.95F);
    }

    private void performToadGroundSlam(ServerLevel serverLevel) {
        double radius = this.phaseTwoTriggered ? 6.0D : 4.8D;
        float damage = this.phaseTwoTriggered ? 8.5F : 6.0F;

        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 0.9F, 0.72F);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.FROG_STEP,
                SoundSource.HOSTILE, 1.2F, 0.50F);

        serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.2D, this.getZ(),
                26, radius * 0.42D, 0.15D, radius * 0.42D, 0.08D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        serverLevel.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.3D, this.getZ(),
                22, radius * 0.38D, 0.25D, radius * 0.38D, 0.12D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        double radiusSq = radius * radius;
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius))) {
            if (entity == this || !entity.isAlive() || entity.isSpectator() || isProtectedFriendly(entity)) {
                continue;
            }
            if (entity instanceof Player && !this.duelActive) {
                continue;
            }
            if (entity.distanceToSqr(this) <= radiusSq) {
                entity.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(this), damage);
                Vec3 diff = entity.position().subtract(this.position());
                Vec3 horizontal = new Vec3(diff.x, 0.0D, diff.z);
                Vec3 push = horizontal.lengthSqr() > 1.0E-5D ? horizontal.normalize() : new Vec3(1.0D, 0.0D, 0.0D);
                entity.push(push.x * 1.25D, 0.58D, push.z * 1.25D);
                entity.needsSync = true;
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.trackingPlayers.add(player);
        if (this.duelActive) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.trackingPlayers.remove(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("DuelActive", this.duelActive);
        output.putBoolean("PhaseTwoTriggered", this.phaseTwoTriggered);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.duelActive = input.getBooleanOr("DuelActive", false);
        this.phaseTwoTriggered = input.getBooleanOr("PhaseTwoTriggered", false);
        this.entityData.set(DATA_DUEL_ACTIVE, this.duelActive);
        this.entityData.set(DATA_PHASE_TWO, this.phaseTwoTriggered);
        this.bossEvent.setVisible(this.duelActive);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Set<ServerPlayer> winners = new HashSet<>();
            if (source.getEntity() instanceof ServerPlayer killer) {
                winners.add(killer);
            }

            double maxDistSq = BOSS_PARTICIPATION_RADIUS * BOSS_PARTICIPATION_RADIUS;
            for (ServerPlayer sp : serverLevel.players()) {
                if (sp.distanceToSqr(this) <= maxDistSq
                        && (this.participantUuids.contains(sp.getUUID()) || this.bossEvent.getPlayers().contains(sp))) {
                    winners.add(sp);
                }
            }

            this.bossEvent.removeAllPlayers();

            Component victoryMsg = Component.translatable("entity.narutomod.sage_trial_boss.victory")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);

            for (ServerPlayer winner : winners) {
                NinjaData data = winner.getData(NinjaCapabilityHandler.NINJA_DATA);
                data.unlockAbility(SAGE_MODE_ID);
                winner.sendSystemMessage(victoryMsg);
                winner.sendOverlayMessage(victoryMsg);
                NarutoRegistries.NINJA_PROGRESS_TRIGGER.get().trigger(
                        winner, NinjaProgressTrigger.Action.DEFEAT_BOSS, "sage_trial_boss");
                notifyBossDefeatMissionProgress(winner, data);
            }

            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    this.getX(), this.getY() + 1.0D, this.getZ(),
                    32, 0.7D, 0.8D, 0.7D, 0.2D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }
    }
}
