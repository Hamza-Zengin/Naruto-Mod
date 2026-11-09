package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Unified 3D Jutsu Visual & Projectile Effect Entity.
 * Renders custom multi-part 3D models (Kirin Dragon, Mokuton Root Dome, Kaguya Bone Forest,
 * Rasengan Sphere, Chidori Lightning Cage, Suiton Water Vortex, Fuuton Wind Funnel,
 * Amaterasu Black Flame Pillar, Kage Mane Shadow Bind, and Doton Earth Bulwark).
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
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_VISUAL_TYPE, TYPE_KIRIN_DRAGON);
        this.entityData.define(DATA_MAX_LIFE, 40);
        this.entityData.define(DATA_TARGET_ID, -1);
        this.entityData.define(DATA_YAW, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();

        int targetId = this.getTargetId();
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

        if (!this.level().isClientSide && this.tickCount >= this.getMaxLife()) {
            this.discard();
        }
    }

    public int getVisualType() {
        return this.entityData.get(DATA_VISUAL_TYPE);
    }

    public void setVisualType(int type) {
        this.entityData.set(DATA_VISUAL_TYPE, type);
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
        return distance < 16384.0D; // Visible up to 128 blocks away
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setVisualType(tag.getInt("VisualType"));
        this.setMaxLife(tag.getInt("MaxLife"));
        this.setEffectYaw(tag.getFloat("EffectYaw"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("VisualType", this.getVisualType());
        tag.putInt("MaxLife", this.getMaxLife());
        tag.putFloat("EffectYaw", this.getEffectYaw());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
