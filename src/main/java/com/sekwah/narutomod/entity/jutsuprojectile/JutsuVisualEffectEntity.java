package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Functional 3D Jutsu Visual & Physical Combat Entity.
 * Renders custom multi-part 3D models while executing authentic server-side physics,
 * projectile collisions, physical barrier defense, and combat mechanics.
 */
public class JutsuVisualEffectEntity extends Entity {

    public static final int TYPE_KIRIN_DRAGON = 0;
    public static final int TYPE_WOOD_PRISON = 1;
    public static final int TYPE_BONE_FOREST = 2;
    public static final int TYPE_RASENGAN_SPHERE = 3;
    public static final int TYPE_CHIDORI_BURST = 4;
    public static final int TYPE_WATER_VORTEX = 5;
    public static final int TYPE_WIND_SHOCKWAVE = 6;
    public static final int TYPE_AMATERASU_PILLAR = 7;
    public static final int TYPE_SHADOW_BIND = 8;
    public static final int TYPE_EARTH_WALL = 9;

    private static final EntityDataAccessor<Integer> DATA_VISUAL_TYPE =
            SynchedEntityData.defineId(JutsuVisualEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX_LIFE =
            SynchedEntityData.defineId(JutsuVisualEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(JutsuVisualEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(JutsuVisualEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_CASTER_UUID =
            SynchedEntityData.defineId(JutsuVisualEffectEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public JutsuVisualEffectEntity(EntityType<? extends JutsuVisualEffectEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static JutsuVisualEffectEntity create(Level level, double x, double y, double z,
                                                 int visualType, int maxLife, float yaw) {
        JutsuVisualEffectEntity entity = new JutsuVisualEffectEntity(NarutoEntities.JUTSU_VISUAL_EFFECT.get(), level);
        entity.setPos(x, y, z);
        entity.setVisualType(visualType);
        entity.setMaxLife(maxLife);
        entity.setEffectYaw(yaw);
        entity.setYRot(yaw);
        if (visualType == TYPE_EARTH_WALL) {
            entity.noPhysics = false;
        }
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_VISUAL_TYPE, TYPE_KIRIN_DRAGON);
        this.entityData.define(DATA_MAX_LIFE, 40);
        this.entityData.define(DATA_TARGET_ID, -1);
        this.entityData.define(DATA_YAW, 0.0F);
        this.entityData.define(DATA_CASTER_UUID, Optional.empty());
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
    public void tick() {
        super.tick();

        int type = this.getVisualType();
        int targetId = this.getTargetId();

        // 1. Position tracking or movement velocity
        if (targetId >= 0) {
            Entity tracked = this.level().getEntity(targetId);
            if (tracked != null && tracked.isAlive()) {
                this.setPos(tracked.getX(), tracked.getY(), tracked.getZ());
            }
        } else {
            Vec3 vel = this.getDeltaMovement();
            if (vel.lengthSqr() > 1.0E-5D) {
                this.setPos(this.getX() + vel.x, this.getY() + vel.y, this.getZ() + vel.z);
            }
        }

        // 2. Server-side Jutsu Combat & Physical Functionality (Modularized entities handled in their own classes)
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            Player caster = this.getCasterPlayer();

            switch (type) {
                // --- FŪTON: 3D WIND SHOCKWAVE FUNNEL (Piercing Gust & Deflection) ---
                case TYPE_WIND_SHOCKWAVE -> {
                    float progress = (float) this.tickCount / (float) Math.max(1, this.getMaxLife());
                    double radius = 1.2D + progress * 1.8D;
                    AABB gustBox = this.getBoundingBox().inflate(radius, 1.2D, radius);

                    List<Entity> entities = serverLevel.getEntities(this, gustBox);
                    for (Entity e : entities) {
                        if (e instanceof LivingEntity living && living.isAlive() && (caster == null || living != caster)) {
                            living.hurt(caster != null ? serverLevel.damageSources().playerAttack(caster) : serverLevel.damageSources().generic(), 10.0F);
                            Vec3 push = this.getDeltaMovement().normalize().scale(2.2D).add(0, 0.45D, 0);
                            living.setDeltaMovement(push);
                            living.hurtMarked = true;
                        } else if (e instanceof Projectile projectile) {
                            projectile.setDeltaMovement(this.getDeltaMovement().scale(1.8D));
                        }
                    }
                }

                // --- SHIKOTSUMYAKU: 3D BONE FOREST (Expanding Spire Trap) ---
                case TYPE_BONE_FOREST -> {
                    if (this.tickCount <= 22 && this.tickCount % 7 == 0) {
                        double currentRadius = (this.tickCount / 7) * 2.2D + 1.2D;
                        AABB ringBox = this.getBoundingBox().inflate(currentRadius, 1.5D, currentRadius);
                        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, ringBox,
                                e -> e.isAlive() && (caster == null || e != caster));
                        for (LivingEntity v : victims) {
                            v.hurt(caster != null ? serverLevel.damageSources().playerAttack(caster) : serverLevel.damageSources().generic(), 7.0F);
                            v.setDeltaMovement(v.getDeltaMovement().x, 0.48D, v.getDeltaMovement().z);
                            v.hurtMarked = true;
                            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                        }
                    }
                }

                // --- AMATERASU: 3D BLACK FLAME PILLAR (Inextinguishable Inferno Zone) ---
                case TYPE_AMATERASU_PILLAR -> {
                    if (this.tickCount % 10 == 0) {
                        AABB flameBox = this.getBoundingBox().inflate(1.8D, 1.2D, 1.8D);
                        List<LivingEntity> burning = serverLevel.getEntitiesOfClass(LivingEntity.class, flameBox,
                                e -> e.isAlive() && (caster == null || e != caster));
                        for (LivingEntity b : burning) {
                            b.hurt(caster != null ? serverLevel.damageSources().playerAttack(caster) : serverLevel.damageSources().inFire(), 5.0F);
                            b.setRemainingFireTicks(240);
                            b.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2));
                        }
                    }
                }

                // --- RAITON: 3D KIRIN LIGHTNING DRAGON (Timed Thunderous Ground Slam) ---
                case TYPE_KIRIN_DRAGON -> {
                    if (this.tickCount == 6) {
                        // Ground slam moment
                        serverLevel.playSound(null, this.blockPosition(), NarutoSounds.KIRIN_STRIKE.get(), SoundSource.WEATHER, 3.5F, 0.85F);
                        serverLevel.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3.5F, 0.75F);
                        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6F, 1.0F);

                        AABB slamBox = this.getBoundingBox().inflate(6.5D, 3.0D, 6.5D);
                        List<LivingEntity> struck = serverLevel.getEntitiesOfClass(LivingEntity.class, slamBox,
                                e -> e.isAlive() && (caster == null || e != caster));
                        for (LivingEntity s : struck) {
                            s.hurt(serverLevel.damageSources().lightningBolt(), 32.0F);
                            double dx = s.getX() - this.getX();
                            double dz = s.getZ() - this.getZ();
                            double distSq = dx * dx + dz * dz;
                            if (distSq > 1.0E-4D) {
                                double dist = Math.sqrt(distSq);
                                double factor = 2.2D / dist;
                                s.push(dx * factor, 0.75D, dz * factor);
                            } else {
                                s.push(0, 0.75D, 0);
                            }
                            s.setRemainingFireTicks(100);
                            s.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                        }
                    }
                }

                case TYPE_CHIDORI_BURST -> {
                    if (this.tickCount % 8 == 0) {
                        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                                NarutoSounds.CHIDORI_LOOP.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
                    }
                }

                case TYPE_RASENGAN_SPHERE -> {
                    if (this.tickCount % 8 == 0) {
                        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                                NarutoSounds.RASENGAN_LOOP.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
                    }
                }
            }

            // Expiration
            if (this.tickCount >= this.getMaxLife()) {
                this.discard();
            }
        }
    }

    public int getVisualType() {
        return this.entityData.get(DATA_VISUAL_TYPE);
    }

    public void setVisualType(int type) {
        this.entityData.set(DATA_VISUAL_TYPE, type);
        if (type == TYPE_EARTH_WALL) {
            this.noPhysics = false;
        }
    }

    public int getMaxLife() {
        return this.entityData.get(DATA_MAX_LIFE);
    }

    public void setMaxLife(int maxLife) {
        this.entityData.set(DATA_MAX_LIFE, maxLife);
    }

    public int getTargetId() {
        return this.entityData.get(DATA_TARGET_ID);
    }

    public void setTargetId(int entityId) {
        this.entityData.set(DATA_TARGET_ID, entityId);
    }

    public float getEffectYaw() {
        return this.entityData.get(DATA_YAW);
    }

    public void setEffectYaw(float yaw) {
        this.entityData.set(DATA_YAW, yaw);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setVisualType(tag.getInt("VisualType"));
        this.setMaxLife(tag.getInt("MaxLife"));
        this.setEffectYaw(tag.getFloat("EffectYaw"));
        if (tag.hasUUID("CasterUUID")) {
            this.setCasterUUID(tag.getUUID("CasterUUID"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("VisualType", this.getVisualType());
        tag.putInt("MaxLife", this.getMaxLife());
        tag.putFloat("EffectYaw", this.getEffectYaw());
        Optional<UUID> uuidOpt = this.getCasterUUID();
        uuidOpt.ifPresent(uuid -> tag.putUUID("CasterUUID", uuid));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
