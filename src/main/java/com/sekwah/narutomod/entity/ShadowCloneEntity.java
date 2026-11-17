package com.sekwah.narutomod.entity;

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

    private static final EntityDataAccessor<String> DATA_ACTIVE_ABILITIES =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SHARINGAN_STAGE =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HACHIMON_GATE =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.INT);

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
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
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
        java.util.Set<Identifier> toggles = ninjaData.getToggleAbilityData().getAbilitiesHashSet();
        String joined = toggles.stream()
                .map(Identifier::toString)
                .collect(java.util.stream.Collectors.joining(","));
        this.entityData.set(DATA_ACTIVE_ABILITIES, joined);

        if (ninjaData.isSharinganActive()) {
            this.entityData.set(DATA_SHARINGAN_STAGE,
                    com.sekwah.narutomod.abilities.jutsus.SharinganAbility.getStage(ninjaData).ordinal() + 1);
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SPEED, 1200, 0, false, false));
        }
        if (hasActiveAbility("susanoo_ribcage")) {
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.RESISTANCE, 1200, 1, false, false));
        }
        if (hasActiveAbility("eight_gates")) {
            int gate = com.sekwah.narutomod.abilities.jutsus.EightGatesAbility.getStage(ninjaData).ordinal() + 1;
            this.entityData.set(DATA_HACHIMON_GATE, gate);
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.STRENGTH, 1200, Math.min(2, gate / 3), false, false));
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SPEED, 1200, Math.min(2, gate / 3), false, false));
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
