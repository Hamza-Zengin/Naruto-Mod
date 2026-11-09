package com.sekwah.narutomod.entity.mob;

import com.sekwah.narutomod.entity.projectile.KunaiEntity;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Rogue Ninja (Nukenin)
 * Agile hostile shinobi that alternates between throwing kunai from range,
 * closing in with swift melee strikes, and executing a Kawarimi smoke evasion at low health.
 */
public class RogueNinjaEntity extends Monster {

    private int kunaiCooldown = 40;
    private boolean usedKawarimi = false;

    public RogueNinjaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 45.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 5.5D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        RandomSource random = level.getRandom();
        this.populateDefaultEquipmentSlots(random, difficulty);
        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (random.nextBoolean()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarutoItems.KUNAI.get()));
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        if (random.nextFloat() < 0.35f) {
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(NarutoItems.HEADBAND_LEAF_SCRATCHED.get()));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.kunaiCooldown > 0) {
                this.kunaiCooldown--;
            }

            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double distSqr = this.distanceToSqr(target);
                if (distSqr >= 36.0D && distSqr <= 256.0D && this.kunaiCooldown <= 0 && this.hasLineOfSight(target)) {
                    throwKunaiAtTarget(target);
                    this.kunaiCooldown = 55 + this.random.nextInt(25);
                }
            }
        }
    }

    private void throwKunaiAtTarget(LivingEntity target) {
        KunaiEntity kunai = new KunaiEntity(this.level(), this);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - kunai.getY();
        double dz = target.getZ() - this.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        kunai.shoot(dx, dy + horiz * 0.15D, dz, 2.2F, 4.0F);
        kunai.setBaseDamage(3.0D);
        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.2F);
        this.level().addFreshEntity(kunai);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && !this.usedKawarimi && this.isAlive() && this.getHealth() < this.getMaxHealth() * 0.35F) {
            this.usedKawarimi = true;
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.9, this.getZ(), 20, 0.3, 0.5, 0.3, 0.04);
                serverLevel.playSound(null, this.blockPosition(), NarutoSounds.CLONE_POOF.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
                Vec3 look = this.getLookAngle();
                this.teleportTo(this.getX() - look.x * 3.5, this.getY() + 0.5, this.getZ() - look.z * 3.5);
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));
            }
        }
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (this.level().isClientSide) return;

        this.spawnAtLocation(new ItemStack(NarutoItems.KUNAI.get(), 2 + this.random.nextInt(4)));
        if (this.random.nextFloat() < 0.45f) {
            this.spawnAtLocation(new ItemStack(NarutoItems.CHAKRA_METAL.get(), 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.25f) {
            this.spawnAtLocation(new ItemStack(NarutoItems.MILITARY_RATIONS_PILL.get(), 1));
        }
        if (this.random.nextFloat() < 0.08f) {
            this.spawnAtLocation(new ItemStack(NarutoItems.SCROLL_SHADOW_POSSESSION.get(), 1));
        }
    }
}
