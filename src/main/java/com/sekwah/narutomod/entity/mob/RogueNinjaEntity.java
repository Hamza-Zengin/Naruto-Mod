package com.sekwah.narutomod.entity.mob;

import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.ai.NinjaJutsuAttackGoal;
import com.sekwah.narutomod.entity.ai.NinjaJutsuCooldowns;
import com.sekwah.narutomod.entity.ai.NinjaJutsuProfile;
import com.sekwah.narutomod.entity.ai.NinjaJutsuUser;
import com.sekwah.narutomod.entity.projectile.KunaiEntity;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;

/**
 * Rogue Ninja (Nukenin)
 * Agile hostile shinobi that casts basic Water Bullet jutsu at long range,
 * throws kunai at medium range, closes in with melee strikes, and executes safe Kawarimi evasion at low health.
 * May spawn as an A-Rank or S-Rank Bingo Book mini-boss with elemental jutsu mastery and bounty loot.
 */
public class RogueNinjaEntity extends Monster implements NinjaJutsuUser {

    private static final EntityDataAccessor<Boolean> DATA_IS_BINGO_TARGET =
            SynchedEntityData.defineId(RogueNinjaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_BINGO_RANK =
            SynchedEntityData.defineId(RogueNinjaEntity.class, EntityDataSerializers.STRING);

    private static final float BINGO_SPAWN_CHANCE = 0.12F;

    private final NinjaJutsuCooldowns jutsuCooldowns = new NinjaJutsuCooldowns(60);
    private int kunaiCooldown = 40;
    private String bingoElement = "";
    @Nullable
    private NinjaJutsuProfile bingoJutsuProfile;

    public RogueNinjaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IS_BINGO_TARGET, false);
        builder.define(DATA_BINGO_RANK, "");
    }

    public boolean isBingoTarget() {
        return this.entityData.get(DATA_IS_BINGO_TARGET);
    }

    public String getBingoRank() {
        return this.entityData.get(DATA_BINGO_RANK);
    }

    private NinjaJutsuProfile getActiveJutsuProfile() {
        return this.bingoJutsuProfile != null ? this.bingoJutsuProfile : NinjaJutsuProfile.roguePreset();
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
        this.goalSelector.addGoal(2, new NinjaJutsuAttackGoal<>(this, this::getActiveJutsuProfile));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, RogueNinjaEntity.class, AkatsukiNinjaEntity.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, VillageNinjaEntity.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, ChuninChampionEntity.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, ShadowCloneEntity.class, true));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, AbstractGolem.class, true));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, ShinobiVillagerEntity.class, true));
        this.targetSelector.addGoal(8, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
        this.targetSelector.addGoal(9, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                (target, serverLevel) -> isValidHostileTarget(target)));
    }

    public static boolean isOutlawAlly(@Nullable Entity target) {
        return target instanceof RogueNinjaEntity || target instanceof AkatsukiNinjaEntity;
    }

    public static boolean isOutlawAllyClass(@Nullable Class<?> targetClass) {
        if (targetClass == null) {
            return false;
        }
        return RogueNinjaEntity.class.isAssignableFrom(targetClass)
                || AkatsukiNinjaEntity.class.isAssignableFrom(targetClass);
    }

    public static boolean isValidHostileTarget(@Nullable LivingEntity target) {
        return target != null && !isOutlawAlly(target);
    }

    public static boolean shouldTargetClass(@Nullable Class<?> targetClass) {
        if (targetClass == null || isOutlawAllyClass(targetClass)) {
            return false;
        }
        return Player.class.isAssignableFrom(targetClass)
                || VillageNinjaEntity.class.isAssignableFrom(targetClass)
                || ShinobiVillagerEntity.class.isAssignableFrom(targetClass)
                || ChuninChampionEntity.class.isAssignableFrom(targetClass)
                || ShadowCloneEntity.class.isAssignableFrom(targetClass)
                || AbstractVillager.class.isAssignableFrom(targetClass)
                || AbstractGolem.class.isAssignableFrom(targetClass)
                || Monster.class.isAssignableFrom(targetClass);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (isOutlawAlly(target)) {
            return false;
        }
        return super.canAttack(target);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        RandomSource random = level.getRandom();
        this.populateDefaultEquipmentSlots(random, difficulty);
        if (reason != EntitySpawnReason.SPAWN_ITEM_USE && random.nextFloat() < BINGO_SPAWN_CHANCE) {
            this.promoteToBingoTarget(random);
        }
        return data;
    }

    public BingoBookProfile promoteToBingoTarget(RandomSource random) {
        BingoBookProfile profile = BingoBookProfile.generate(random);
        this.entityData.set(DATA_IS_BINGO_TARGET, true);
        this.entityData.set(DATA_BINGO_RANK, profile.rankTier().getCode());
        this.bingoElement = profile.elementTheme().name();
        this.bingoJutsuProfile = profile.jutsuProfile();
        this.xpReward = profile.rankTier().getXpReward();

        AttributeInstance maxHpAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHpAttr != null) {
            maxHpAttr.setBaseValue(profile.maxHealth());
            this.setHealth((float) profile.maxHealth());
        }
        AttributeInstance armorAttr = this.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(10.0D);
        }
        AttributeInstance attackAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(profile.rankTier() == BingoBookProfile.RankTier.S ? 8.5D : 7.0D);
        }

        if (profile.rankTier() == BingoBookProfile.RankTier.S) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarutoItems.CHAKRA_BLADES.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarutoItems.ANBU_TANTO.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
        }

        this.setCustomName(profile.formatWantedName());
        this.setCustomNameVisible(true);
        this.setGlowingTag(true);
        return profile;
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
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("IsBingoTarget", this.isBingoTarget());
        output.putString("BingoRank", this.getBingoRank());
        output.putString("BingoElement", this.bingoElement != null ? this.bingoElement : "");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        boolean bingo = input.getBooleanOr("IsBingoTarget", false);
        String rank = input.getStringOr("BingoRank", "");
        String element = input.getStringOr("BingoElement", "");
        this.entityData.set(DATA_IS_BINGO_TARGET, bingo);
        this.entityData.set(DATA_BINGO_RANK, rank);
        this.bingoElement = element;
        if (bingo) {
            BingoBookProfile.RankTier tier = BingoBookProfile.RankTier.fromString(rank);
            BingoBookProfile.ElementTheme theme = BingoBookProfile.ElementTheme.fromNameOrSeed(
                    element, this.getUUID().getLeastSignificantBits());
            this.bingoJutsuProfile = BingoBookProfile.buildJutsuProfile(tier, theme);
            this.xpReward = tier.getXpReward();
            this.setGlowingTag(true);
        } else {
            this.bingoJutsuProfile = null;
            this.xpReward = 15;
        }
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

        if (this.isBingoTarget()) {
            BingoBookProfile.RankTier tier = BingoBookProfile.RankTier.fromString(this.getBingoRank());
            BingoBookProfile.dropBingoBonusLoot(serverLevel, this, this.random, tier);
        }
    }
}
