package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Mokuton: Wood Prison Entity (Shichūrō / Jukai Kōtan)
 * Physical wooden cage entity that traps targets inside its AABB, preventing horizontal movement and jumping.
 */
public class WoodPrisonEntity extends Entity {

    public static final int DEFAULT_MAX_LIFE = 80;
    public static final double CAGE_RADIUS = 4.5D;
    public static final double CAGE_HEIGHT = 3.5D;

    private static final EntityDataAccessor<Integer> DATA_MAX_LIFE =
            SynchedEntityData.defineId(WoodPrisonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_CAGE_YAW =
            SynchedEntityData.defineId(WoodPrisonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<net.minecraft.world.entity.EntityReference<LivingEntity>>> DATA_CASTER_UUID =
            SynchedEntityData.defineId(WoodPrisonEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);

    private static final class Particles {
        static final BlockParticleOption WOOD = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_WOOD.defaultBlockState());
        static final BlockParticleOption LEAF = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AZALEA_LEAVES.defaultBlockState());
    }

    public WoodPrisonEntity(EntityType<? extends WoodPrisonEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public static WoodPrisonEntity create(Level level, double x, double y, double z, int maxLife, float yaw, UUID casterUUID) {
        WoodPrisonEntity entity = new WoodPrisonEntity(NarutoEntities.WOOD_PRISON.get(), level);
        entity.setPos(x, y, z);
        entity.setMaxLife(maxLife);
        entity.setCageYaw(yaw);
        entity.setYRot(yaw);
        entity.setCasterUUID(casterUUID);
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MAX_LIFE, DEFAULT_MAX_LIFE);
        builder.define(DATA_CAGE_YAW, 0.0F);
        builder.define(DATA_CASTER_UUID, Optional.empty());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(3.0F, 3.0F);
    }

    public AABB getCageTrapBox() {
        return new AABB(
                this.getX() - CAGE_RADIUS, this.getY() - 1.5D, this.getZ() - CAGE_RADIUS,
                this.getX() + CAGE_RADIUS, this.getY() + CAGE_HEIGHT, this.getZ() + CAGE_RADIUS
        );
    }

    /**
     * Suppresses horizontal escape and vertical jumping for a trapped entity inside the Wood Prison cage.
     */
    public void restrainTarget(LivingEntity target) {
        double pullX = (this.getX() - target.getX()) * 0.35D;
        double pullZ = (this.getZ() - target.getZ()) * 0.35D;
        // Clamp any upward jumping velocity and pin target down
        double pinnedY = Math.min(target.getDeltaMovement().y, -0.3D);
        target.setDeltaMovement(new Vec3(pullX, pinnedY, pullZ));
        target.setJumping(false);
        target.needsSync = true;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Player caster = this.getCasterPlayer();
            AABB cageBox = this.getCageTrapBox();
            List<LivingEntity> trappedTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, cageBox,
                    e -> e.isAlive() && (caster == null || e != caster));

            // Every tick: strictly lock movement and block jumping inside the cage
            for (LivingEntity target : trappedTargets) {
                this.restrainTarget(target);
                MobEffectInstance existingSlow = target.getEffect(MobEffects.SLOWNESS);
                if (existingSlow == null || existingSlow.getAmplifier() < 6
                        || existingSlow.getDuration() <= 10 || this.tickCount % 10 == 0) {
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 6, false, false));
                }
            }

            // Periodic constriction wave: crushing nature damage, life siphon, and root particles
            if (this.tickCount % 14 == 0) {
                for (int i = 0; i < 12; i++) {
                    double angle = (i * Math.PI / 6.0D) + (this.tickCount * 0.15D);
                    double r = 2.0D;
                    double px = this.getX() + Math.cos(angle) * r;
                    double pz = this.getZ() + Math.sin(angle) * r;
                    serverLevel.sendParticles(Particles.WOOD, px, this.getY() + 1.2D, pz, 3, 0.2D, 0.5D, 0.2D, 0.05D,
                            net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                    serverLevel.sendParticles(Particles.LEAF, px, this.getY() + 1.8D, pz, 1, 0.2D, 0.2D, 0.2D, 0.02D,
                            net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                }

                for (LivingEntity target : trappedTargets) {
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
                    target.hurtServer(serverLevel, caster != null ? serverLevel.damageSources().playerAttack(caster) : serverLevel.damageSources().magic(), 3.5F);
                    if (caster != null) {
                        caster.heal(0.8F);
                    }
                }
                serverLevel.playSound(null, this.blockPosition(), SoundEvents.VINE_STEP, SoundSource.PLAYERS, 1.4F, 0.7F);
            }

            if (this.tickCount >= this.getMaxLife()) {
                this.discard();
            }
        }
    }

    public int getMaxLife() {
        return this.entityData.get(DATA_MAX_LIFE);
    }

    public void setMaxLife(int maxLife) {
        this.entityData.set(DATA_MAX_LIFE, maxLife);
    }

    public float getCageYaw() {
        return this.entityData.get(DATA_CAGE_YAW);
    }

    public void setCageYaw(float yaw) {
        this.entityData.set(DATA_CAGE_YAW, yaw);
    }

    public Optional<UUID> getCasterUUID() {
        return this.entityData.get(DATA_CASTER_UUID).map(net.minecraft.world.entity.EntityReference::getUUID);
    }

    public void setCasterUUID(UUID uuid) {
        this.entityData.set(DATA_CASTER_UUID, Optional.ofNullable(uuid).map(net.minecraft.world.entity.EntityReference::of));
    }

    public Player getCasterPlayer() {
        Optional<UUID> uuidOpt = this.getCasterUUID();
        if (uuidOpt.isPresent() && this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getServer().getPlayerList().getPlayer(uuidOpt.get());
        }
        return null;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setMaxLife(input.getIntOr("MaxLife", DEFAULT_MAX_LIFE));
        this.setCageYaw(input.getFloatOr("CageYaw", 0.0F));
        input.read("CasterUUID", UUIDUtil.CODEC).ifPresent(this::setCasterUUID);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("MaxLife", this.getMaxLife());
        output.putFloat("CageYaw", this.getCageYaw());
        this.getCasterUUID().ifPresent(uuid -> output.store("CasterUUID", UUIDUtil.CODEC, uuid));
    }
}
