package com.sekwah.narutomod.entity.mob;

import com.sekwah.narutomod.advancements.NinjaProgressTrigger;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
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
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

/**
 * Chūnin Arena Champion Boss
 * Waits passively in the Chūnin Arena until challenged via right-click or attack.
 * Features a yellow notched Boss Bar, multi-element jutsu AI, Chakra Dash blitzes,
 * a Phase 2 chakra shockwave below 50% HP, and high-tier scroll rewards.
 */
public class ChuninChampionEntity extends PathfinderMob implements NinjaJutsuUser {

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            UUID.randomUUID(),
            Component.translatable("entity.narutomod.chunin_champion"),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_10);

    private final Set<ServerPlayer> trackingPlayers = new HashSet<>();
    private final NinjaJutsuCooldowns jutsuCooldowns = new NinjaJutsuCooldowns(30);
    private int dashCooldown = 50;
    private boolean duelActive = false;
    private boolean phaseTwoTriggered = false;

    public ChuninChampionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 120;
        this.bossEvent.setVisible(false);
    }

    @Override
    public NinjaJutsuCooldowns getJutsuCooldowns() {
        return this.jutsuCooldowns;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.37D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new NinjaJutsuAttackGoal<>(this, NinjaJutsuProfile.chuninChampionPreset()));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3D, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (target, serverLevel) -> this.duelActive));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (!this.duelActive) {
            return false;
        }
        if (target instanceof VillageNinjaEntity || target instanceof ShinobiVillagerEntity) {
            return false;
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
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarutoItems.CHAKRA_BLADES.get()));
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(NarutoItems.HEADBAND_LEAF.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(NarutoItems.CHUNIN_VEST.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.08F);
        this.setDropChance(EquipmentSlot.HEAD, 0.05F);
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
        if (result && !this.duelActive && source.getEntity() instanceof Player player) {
            this.activateDuel(player);
        }
        return result;
    }

    public void activateDuel(Player challenger) {
        if (this.duelActive) {
            return;
        }
        this.duelActive = true;
        this.bossEvent.setVisible(true);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.setTarget(challenger);

        for (ServerPlayer sp : this.trackingPlayers) {
            this.bossEvent.addPlayer(sp);
        }
        if (challenger instanceof ServerPlayer sp) {
            this.bossEvent.addPlayer(sp);
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), NarutoSounds.JUTSU_CAST.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.0D, this.getZ(),
                    20, 0.45D, 0.5D, 0.45D, 0.06D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }

        Component msg = Component.translatable("naruto.boss.chunin_champion.challenge")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        for (ServerPlayer sp : this.trackingPlayers) {
            sp.sendSystemMessage(msg);
        }
        if (challenger instanceof ServerPlayer sp && !this.trackingPlayers.contains(sp)) {
            sp.sendSystemMessage(msg);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            // Ensure structure-spawned champions always have their iconic gear
            if (this.tickCount == 1 && this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                this.equipDefaultGear();
            }

            if (!this.duelActive) {
                if (this.getTarget() != null) {
                    this.setTarget(null);
                }
                return;
            }

            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            this.jutsuCooldowns.tick();
            if (this.dashCooldown > 0) {
                this.dashCooldown--;
            }

            if (!this.phaseTwoTriggered && this.getHealth() <= this.getMaxHealth() * 0.5F) {
                triggerPhaseTwo();
            }

            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double distSqr = this.distanceToSqr(target);
                if (distSqr >= 20.0D && distSqr <= 81.0D && this.dashCooldown <= 0 && this.onGround()) {
                    performChakraDash(target);
                    this.dashCooldown = this.phaseTwoTriggered
                            ? (45 + this.random.nextInt(20))
                            : (85 + this.random.nextInt(25));
                }
            }
        }
    }

    private void triggerPhaseTwo() {
        this.phaseTwoTriggered = true;
        this.dashCooldown = Math.min(this.dashCooldown, 15);

        this.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 1));
        this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600, 0));

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 1.1F);
            serverLevel.playSound(null, this.blockPosition(), NarutoSounds.CHIDORI.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0D, this.getZ(),
                    4, 0.6D, 0.4D, 0.6D, 0.02D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0D, this.getZ(),
                    32, 1.2D, 0.8D, 1.2D, 0.15D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.5D))) {
                if (entity == this || entity instanceof VillageNinjaEntity || entity instanceof ShinobiVillagerEntity) {
                    continue;
                }
                if (entity.distanceToSqr(this) <= 4.5D * 4.5D) {
                    entity.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(this), 6.0F);
                    Vec3 diff = entity.position().subtract(this.position());
                    Vec3 horizontal = new Vec3(diff.x, 0.0D, diff.z);
                    Vec3 push = horizontal.lengthSqr() > 1.0E-5D ? horizontal.normalize() : new Vec3(1.0D, 0.0D, 0.0D);
                    entity.push(push.x * 1.35D, 0.45D, push.z * 1.35D);
                    entity.needsSync = true;
                }
            }
        }

        Component msg = Component.translatable("naruto.boss.chunin_champion.phase2")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        for (ServerPlayer sp : this.trackingPlayers) {
            sp.sendSystemMessage(msg);
        }
    }

    private void performChakraDash(LivingEntity target) {
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 dir = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(dir.x * 1.4D, 0.32D, dir.z * 1.4D);
            this.needsSync = true;
            serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.2D, this.getZ(),
                    10, 0.25D, 0.1D, 0.25D, 0.04D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.playSound(null, this.blockPosition(), NarutoSounds.LEAP.get(), SoundSource.HOSTILE, 1.0F, 1.15F);
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
        this.bossEvent.setVisible(this.duelActive);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide()) {
            this.bossEvent.removeAllPlayers();
            if (source.getEntity() instanceof ServerPlayer killer) {
                // Award +9 bonus enemy defeats (+1 from LivingDeathEvent = +10 total) and +5 rogue ninja defeats
                var data = killer.getData(NinjaCapabilityHandler.NINJA_DATA);
                for (int i = 0; i < 9; i++) {
                    data.addEnemyDefeated();
                }
                for (int i = 0; i < 5; i++) {
                    data.addRogueNinjaDefeated();
                }
                NarutoRegistries.NINJA_PROGRESS_TRIGGER.get().trigger(
                        killer, NinjaProgressTrigger.Action.DEFEAT_BOSS, "chunin_champion");
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(serverLevel, source, recentlyHit);

        this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.CHUNIN_VEST.get(), 1));
        this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.CHAKRA_METAL.get(), 2 + this.random.nextInt(3)));
        this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.ICHIRAKU_RAMEN.get(), 1 + this.random.nextInt(2)));

        Item[] scrolls = {
                NarutoItems.SCROLL_RASENGAN.get(),
                NarutoItems.SCROLL_CHIDORI.get(),
                NarutoItems.SCROLL_RASENSHURIKEN.get(),
                NarutoItems.SCROLL_KIRIN.get(),
                NarutoItems.SCROLL_WATER_DRAGON.get()
        };
        Item chosenScroll = scrolls[this.random.nextInt(scrolls.length)];
        this.spawnAtLocation(serverLevel, new ItemStack(chosenScroll, 1));
    }
}
