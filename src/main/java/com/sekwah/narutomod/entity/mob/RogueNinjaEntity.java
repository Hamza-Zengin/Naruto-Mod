package com.sekwah.narutomod.entity.mob;

import com.sekwah.narutomod.entity.ai.NinjaJutsuAttackGoal;
import com.sekwah.narutomod.entity.ai.NinjaJutsuCooldowns;
import com.sekwah.narutomod.entity.ai.NinjaJutsuProfile;
import com.sekwah.narutomod.entity.ai.NinjaJutsuUser;
import com.sekwah.narutomod.entity.projectile.KunaiEntity;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * Rogue Ninja (Nukenin)
 * Agile hostile shinobi that casts basic Water Bullet jutsu at long range,
 * throws kunai at medium range, closes in with melee strikes, and executes safe Kawarimi evasion at low health.
 */
public class RogueNinjaEntity extends Monster implements NinjaJutsuUser {

    private final NinjaJutsuCooldowns jutsuCooldowns = new NinjaJutsuCooldowns(60);
    private int kunaiCooldown = 40;

    public RogueNinjaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    @Override
    public NinjaJutsuCooldowns getJutsuCooldowns() {
        return this.jutsuCooldowns;
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
        this.goalSelector.addGoal(2, new NinjaJutsuAttackGoal<>(this, NinjaJutsuProfile.roguePreset()));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0F));
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
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarutoItems.KUNAI.get()));
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(NarutoItems.HEADBAND_LEAF_SCRATCHED.get()));
        this.setDropChance(EquipmentSlot.HEAD, 0.06F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.jutsuCooldowns.tick();
            if (this.kunaiCooldown > 0) {
                this.kunaiCooldown--;
            }

            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double distSqr = this.distanceToSqr(target);
                if (distSqr >= 25.0D && distSqr <= 95.0D && this.kunaiCooldown <= 0 && this.hasLineOfSight(target)) {
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
    protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(serverLevel, source, recentlyHit);

        this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.KUNAI.get(), 2 + this.random.nextInt(4)));
        if (this.random.nextFloat() < 0.45f) {
            this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.CHAKRA_METAL.get(), 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.25f) {
            this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.MILITARY_RATIONS_PILL.get(), 1));
        }
        if (this.random.nextFloat() < 0.08f) {
            this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.BLANK_JUTSU_SCROLL.get(), 1));
        }
        if (this.random.nextFloat() < 0.06f) {
            this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.ANBU_TANTO.get(), 1));
        }
        if (this.random.nextFloat() < 0.05f) {
            var scroll = this.random.nextBoolean()
                    ? NarutoItems.SCROLL_STONE_SPIKES.get()
                    : NarutoItems.SCROLL_WATER_DRAGON.get();
            this.spawnAtLocation(serverLevel, new ItemStack(scroll, 1));
        }
    }
}
