package com.sekwah.narutomod.entity.mob;

import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.ai.NinjaJutsuAttackGoal;
import com.sekwah.narutomod.entity.ai.NinjaJutsuCooldowns;
import com.sekwah.narutomod.entity.ai.NinjaJutsuProfile;
import com.sekwah.narutomod.entity.ai.NinjaJutsuUser;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Akatsuki S-Rank Rogue Shinobi (Mini-Boss)
 * Features a dynamic red Boss Bar, tactical Fire/Water jutsu & defensive Kawarimi/MudWall AI,
 * executes Chakra Dash blitzes, wields Kusanagi or Samehada, and grants +5 exam progress on defeat.
 */
public class AkatsukiNinjaEntity extends Monster implements NinjaJutsuUser {

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            java.util.UUID.randomUUID(),
            Component.translatable("entity.narutomod.akatsuki_ninja"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10);

    private final NinjaJutsuCooldowns jutsuCooldowns = new NinjaJutsuCooldowns(40);
    private int dashCooldown = 60;

    public AkatsukiNinjaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 75;
    }

    @Override
    public NinjaJutsuCooldowns getJutsuCooldowns() {
        return this.jutsuCooldowns;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 160.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ATTACK_DAMAGE, 8.5D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new NinjaJutsuAttackGoal<>(this, NinjaJutsuProfile.akatsukiPreset()));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25D, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        RandomSource random = level.getRandom();
        this.populateDefaultEquipmentSlots(random, difficulty);
        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (random.nextBoolean()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarutoItems.KUSANAGI_SWORD.get()));
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarutoItems.SAMEHADA.get()));
        }
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(NarutoItems.AKATSUKI_CLOAK.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.12F);
        this.setDropChance(EquipmentSlot.CHEST, 0.18F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

            this.jutsuCooldowns.tick();
            if (this.dashCooldown > 0) this.dashCooldown--;

            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double distSqr = this.distanceToSqr(target);

                // Execute Chakra Dash blitz at medium range
                if (distSqr >= 20.0D && distSqr <= 81.0D && this.dashCooldown <= 0 && this.onGround()) {
                    performChakraDash(target);
                    this.dashCooldown = 90 + this.random.nextInt(30);
                }
            }
        }
    }

    private void performChakraDash(LivingEntity target) {
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 dir = target.position().subtract(this.position()).normalize();
            this.setDeltaMovement(dir.x * 1.35D, 0.32D, dir.z * 1.35D);
            this.needsSync = true;
            serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.2, this.getZ(), 8, 0.2, 0.1, 0.2, 0.03,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.playSound(null, this.blockPosition(), NarutoSounds.LEAP.get(), SoundSource.HOSTILE, 1.0f, 1.1f);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide() && source.getEntity() instanceof Player killer) {
            // Award +4 bonus enemy defeats (+1 from standard LivingDeathEvent = +5 total exam progress)
            var data = killer.getData(NinjaCapabilityHandler.NINJA_DATA);
            for (int i = 0; i < 4; i++) {
                data.addEnemyDefeated();
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(serverLevel, source, recentlyHit);

        this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.CHAKRA_METAL.get(), 2 + this.random.nextInt(3)));
        this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.ICHIRAKU_RAMEN.get(), 1 + this.random.nextInt(2)));
        this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.FUMA_SHURIKEN.get(), 1 + this.random.nextInt(2)));

        float roll = this.random.nextFloat();
        if (roll < 0.35f) {
            this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.SCROLL_RASENSHURIKEN.get(), 1));
        } else if (roll < 0.65f) {
            this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.SCROLL_KIRIN.get(), 1));
        }
    }
}
