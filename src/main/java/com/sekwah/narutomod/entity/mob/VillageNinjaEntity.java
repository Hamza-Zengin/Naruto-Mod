package com.sekwah.narutomod.entity.mob;

import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.ai.NinjaJutsuAttackGoal;
import com.sekwah.narutomod.entity.ai.NinjaJutsuCooldowns;
import com.sekwah.narutomod.entity.ai.NinjaJutsuProfile;
import com.sekwah.narutomod.entity.ai.NinjaJutsuUser;
import com.sekwah.narutomod.entity.projectile.KunaiEntity;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Village Guard Shinobi (Konoha, Suna, Kiri, Kumo, Iwa)
 * Neutral to peaceful players, defends villages from Rogue Ninjas, Akatsuki, and hostile monsters.
 * Uses village-specific headband, Jonin flak vest, and elemental jutsu AI profile.
 */
public class VillageNinjaEntity extends PathfinderMob implements NinjaJutsuUser {

    public enum VillageType {
        KONOHA("konoha"),
        SUNA("suna"),
        KIRI("kiri"),
        KUMO("kumo"),
        IWA("iwa");

        private final String id;

        VillageType(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }

        public static VillageType fromId(String id) {
            if (id != null) {
                String lower = id.toLowerCase(Locale.ROOT);
                for (VillageType type : values()) {
                    if (type.id.equals(lower)) {
                        return type;
                    }
                }
            }
            return KONOHA;
        }

        public static VillageType fromBiomePath(String biomePath) {
            if (biomePath == null) {
                return KONOHA;
            }
            String lower = biomePath.toLowerCase(Locale.ROOT);
            if (lower.contains("desert") || lower.contains("badlands")) {
                return SUNA;
            }
            if (lower.contains("ocean") || lower.contains("river") || lower.contains("swamp") || lower.contains("beach") || lower.contains("mangrove")) {
                return KIRI;
            }
            if (lower.contains("jagged") || lower.contains("frozen") || lower.contains("stony_peaks") || lower.contains("windswept")) {
                return KUMO;
            }
            if (lower.contains("mountain") || lower.contains("hill") || lower.contains("savanna") || lower.contains("dripstone")) {
                return IWA;
            }
            return KONOHA;
        }
    }

    private static final EntityDataAccessor<String> DATA_VILLAGE_TYPE =
            SynchedEntityData.defineId(VillageNinjaEntity.class, EntityDataSerializers.STRING);

    private final NinjaJutsuCooldowns jutsuCooldowns = new NinjaJutsuCooldowns(45);
    private int kunaiCooldown = 40;
    @Nullable
    private final VillageType fixedVillageType;

    public VillageNinjaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        this(type, level, null);
    }

    public VillageNinjaEntity(EntityType<? extends PathfinderMob> type, Level level, @Nullable VillageType fixedVillageType) {
        super(type, level);
        this.xpReward = 18;
        this.fixedVillageType = fixedVillageType;
        if (fixedVillageType != null) {
            this.setVillageType(fixedVillageType);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VILLAGE_TYPE, VillageType.KONOHA.getId());
    }

    public VillageType getVillageType() {
        return this.fixedVillageType != null ? this.fixedVillageType : VillageType.fromId(this.entityData.get(DATA_VILLAGE_TYPE));
    }

    public void setVillageType(VillageType type) {
        VillageType resolved = this.fixedVillageType != null ? this.fixedVillageType : (type != null ? type : VillageType.KONOHA);
        this.entityData.set(DATA_VILLAGE_TYPE, resolved.getId());
    }

    @Override
    public NinjaJutsuCooldowns getJutsuCooldowns() {
        return this.jutsuCooldowns;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 6.5D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new NinjaJutsuAttackGoal<>(this, () -> NinjaJutsuProfile.villagePreset(this.getVillageType().getId())));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25D, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AkatsukiNinjaEntity.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, RogueNinjaEntity.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                (target, serverLevel) -> !(target instanceof RogueNinjaEntity) && !(target instanceof AkatsukiNinjaEntity)));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof VillageNinjaEntity || target instanceof ShinobiVillagerEntity || target instanceof ShadowCloneEntity) {
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
        VillageType resolved;
        if (this.fixedVillageType != null) {
            resolved = this.fixedVillageType;
        } else {
            BlockPos pos = this.blockPosition();
            Holder<Biome> biomeHolder = level.getBiome(pos);
            String biomePath = biomeHolder.unwrapKey().map(k -> k.identifier().getPath()).orElse("plains");
            resolved = VillageType.fromBiomePath(biomePath);
        }
        this.setVillageType(resolved);
        this.equipForVillage(resolved, level.getRandom());
        this.setPersistenceRequired();
        return data;
    }

    public void equipForVillage(VillageType type, RandomSource random) {
        Item headband = switch (type) {
            case SUNA -> NarutoItems.HEADBAND_SAND.get();
            case KIRI -> NarutoItems.HEADBAND_MIST.get();
            case KUMO -> NarutoItems.HEADBAND_CLOUD.get();
            case IWA -> NarutoItems.HEADBAND_ROCK.get();
            case KONOHA -> NarutoItems.HEADBAND_LEAF.get();
        };
        Item vest = switch (type) {
            case SUNA -> NarutoItems.SAND_JONIN_VEST.get();
            case KIRI -> NarutoItems.MIST_JONIN_VEST.get();
            case KUMO -> NarutoItems.CLOUD_JONIN_VEST.get();
            case IWA -> NarutoItems.ROCK_JONIN_VEST.get();
            case KONOHA -> NarutoItems.JONIN_VEST.get();
        };
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(headband));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(vest));
        this.setDropChance(EquipmentSlot.HEAD, 0.05F);
        this.setDropChance(EquipmentSlot.CHEST, 0.04F);

        int roll = random.nextInt(3);
        if (roll == 0) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarutoItems.KUNAI.get()));
        } else if (roll == 1) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NarutoItems.ANBU_TANTO.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.03F);
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
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
                if (distSqr >= 25.0D && distSqr <= 100.0D && this.kunaiCooldown <= 0 && this.hasLineOfSight(target)) {
                    throwKunaiAtTarget(target);
                    this.kunaiCooldown = 50 + this.random.nextInt(25);
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
        kunai.shoot(dx, dy + horiz * 0.15D, dz, 2.25F, 3.5F);
        kunai.setBaseDamage(3.5D);
        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.2F);
        this.level().addFreshEntity(kunai);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("VillageType", this.getVillageType().getId());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVillageType(VillageType.fromId(input.getStringOr("VillageType", VillageType.KONOHA.getId())));
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel serverLevel, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(serverLevel, source, recentlyHit);
        this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.KUNAI.get(), 1 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.35F) {
            this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.CHAKRA_METAL.get(), 1));
        }
        if (this.random.nextFloat() < 0.20F) {
            this.spawnAtLocation(serverLevel, new ItemStack(NarutoItems.MILITARY_RATIONS_PILL.get(), 1));
        }
    }
}
