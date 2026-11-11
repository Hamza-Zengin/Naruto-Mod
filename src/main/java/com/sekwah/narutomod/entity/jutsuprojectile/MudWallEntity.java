package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Doton: Mud Wall Entity (Doryūheki - 土遁·土流壁)
 * Physical stone/earth barrier entity with solid AABB collision, health pool, duration limit,
 * and incoming projectile interception/deflection.
 */
public class MudWallEntity extends Entity implements JutsuProjectile {

    public static final float DEFAULT_WIDTH = 3.2F;
    public static final float DEFAULT_HEIGHT = 2.8F;
    public static final float DEFAULT_MAX_HEALTH = 60.0F;
    public static final int DEFAULT_MAX_LIFE = 160;

    @Override
    public ChakraElement getElement() {
        return ChakraElement.EARTH;
    }

    @Override
    public int getJutsuPower() {
        return Math.round(this.getHealth());
    }

    @Override
    public void weaken(int amount) {
        this.hurt(this.damageSources().generic(), (float) amount);
    }

    @Override
    public void explodeAndDiscard() {
        if (this.level() instanceof ServerLevel serverLevel) {
            this.crumbleAndDiscard(serverLevel);
        } else {
            this.discard();
        }
    }

    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(MudWallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_MAX_LIFE =
            SynchedEntityData.defineId(MudWallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_WALL_YAW =
            SynchedEntityData.defineId(MudWallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_CASTER_UUID =
            SynchedEntityData.defineId(MudWallEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public MudWallEntity(EntityType<? extends MudWallEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.blocksBuilding = true;
    }

    public static MudWallEntity create(Level level, double x, double y, double z, int maxLife, float yaw, UUID casterUUID) {
        MudWallEntity entity = new MudWallEntity(NarutoEntities.MUD_WALL.get(), level);
        entity.setPos(x, y, z);
        entity.setMaxLife(maxLife);
        entity.setHealth(DEFAULT_MAX_HEALTH);
        entity.setWallYaw(yaw);
        entity.setYRot(yaw);
        entity.setCasterUUID(casterUUID);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HEALTH, DEFAULT_MAX_HEALTH);
        this.entityData.define(DATA_MAX_LIFE, DEFAULT_MAX_LIFE);
        this.entityData.define(DATA_WALL_YAW, 0.0F);
        this.entityData.define(DATA_CASTER_UUID, Optional.empty());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public AABB getPhysicalBarrierBox() {
        return this.getBoundingBox().inflate(0.3D, 0.0D, 0.3D);
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.isAlive();
    }

    @Override
    public boolean isPickable() {
        return this.isAlive();
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return other != this && (other.canBeCollidedWith() || other.isPushable() || other instanceof Projectile);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            AABB wallBox = this.getPhysicalBarrierBox();
            List<Entity> collidingEntities = serverLevel.getEntities(this, wallBox);

            for (Entity entity : collidingEntities) {
                if (entity instanceof JutsuProjectile jutsu) {
                    this.handleClash(jutsu);
                    if (!this.isAlive()) {
                        return;
                    }
                } else if (entity instanceof Projectile projectile) {
                    // Block & deflect incoming projectiles against the physical AABB box
                    projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.5D));
                    projectile.hasImpulse = true;
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1.2F, 0.85F);
                    this.hurt(serverLevel.damageSources().thrown(projectile, projectile.getOwner()), 6.0F);
                } else if (entity instanceof LivingEntity living) {
                    // Physically push living entities out of the solid wall volume
                    double dx = entity.getX() - this.getX();
                    double dz = entity.getZ() - this.getZ();
                    double distSq = dx * dx + dz * dz;
                    if (distSq > 1.0E-4D) {
                        double dist = Math.sqrt(distSq);
                        double factor = 0.35D / dist;
                        living.setDeltaMovement(living.getDeltaMovement().add(dx * factor, 0.05D, dz * factor));
                    }
                    living.hurtMarked = true;
                }
            }

            if (this.tickCount >= this.getMaxLife() || this.getHealth() <= 0.0F) {
                this.crumbleAndDiscard(serverLevel);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || amount <= 0.0F) {
            return false;
        }
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            this.markHurt();
            float remaining = Math.max(0.0F, this.getHealth() - amount);
            this.setHealth(remaining);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1.0F, 0.75F);
            BlockParticleOption rockParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
            serverLevel.sendParticles(rockParticle,
                    this.getX(), this.getY() + 1.2D, this.getZ(),
                    10, 0.8D, 0.8D, 0.8D, 0.1D);

            if (remaining <= 0.0F) {
                this.crumbleAndDiscard(serverLevel);
            }
            return true;
        }
        return true;
    }

    private void crumbleAndDiscard(ServerLevel serverLevel) {
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.5F, 0.7F);
        BlockParticleOption rockParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState());
        serverLevel.sendParticles(rockParticle,
                this.getX(), this.getY() + 1.0D, this.getZ(),
                30, 1.2D, 1.0D, 1.2D, 0.15D);
        this.discard();
    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH);
    }

    public void setHealth(float health) {
        this.entityData.set(DATA_HEALTH, Math.max(0.0F, health));
    }

    public int getMaxLife() {
        return this.entityData.get(DATA_MAX_LIFE);
    }

    public void setMaxLife(int maxLife) {
        this.entityData.set(DATA_MAX_LIFE, maxLife);
    }

    public float getWallYaw() {
        return this.entityData.get(DATA_WALL_YAW);
    }

    public void setWallYaw(float yaw) {
        this.entityData.set(DATA_WALL_YAW, yaw);
    }

    public Optional<UUID> getCasterUUID() {
        return this.entityData.get(DATA_CASTER_UUID);
    }

    public void setCasterUUID(UUID uuid) {
        this.entityData.set(DATA_CASTER_UUID, Optional.ofNullable(uuid));
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
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Health")) {
            this.setHealth(tag.getFloat("Health"));
        }
        if (tag.contains("MaxLife")) {
            this.setMaxLife(tag.getInt("MaxLife"));
        }
        if (tag.contains("WallYaw")) {
            this.setWallYaw(tag.getFloat("WallYaw"));
        }
        if (tag.hasUUID("CasterUUID")) {
            this.setCasterUUID(tag.getUUID("CasterUUID"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", this.getHealth());
        tag.putInt("MaxLife", this.getMaxLife());
        tag.putFloat("WallYaw", this.getWallYaw());
        this.getCasterUUID().ifPresent(uuid -> tag.putUUID("CasterUUID", uuid));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
