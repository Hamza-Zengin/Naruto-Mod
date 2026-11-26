package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.JutsuCombatHelper;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.visual.AshCloudVisualBehavior;
import com.sekwah.narutomod.entity.jutsuprojectile.visual.JutsuVisualBehavior;
import com.sekwah.narutomod.events.ElementalInteractionHandler;
import com.sekwah.narutomod.events.JutsuLightManager;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Functional 3D Jutsu Visual & Physical Combat Entity.
 * Renders custom multi-part 3D models while executing authentic server-side physics,
 * projectile collisions, physical barrier defense, and combat mechanics.
 */
public class JutsuVisualEffectEntity extends Entity implements JutsuProjectile {

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
    public static final int TYPE_STONE_SPIKES = 10;
    public static final int TYPE_ADAMANTINE_CHAINS = 11;
    public static final int TYPE_ASH_CLOUD = 12;
    public static final int TYPE_WATER_WALL = 13;

    public static final float WATER_WALL_WIDTH = 5.2F;
    public static final float WATER_WALL_HEIGHT = 3.2F;
    public static final float EARTH_WALL_WIDTH = 3.2F;
    public static final float EARTH_WALL_HEIGHT = 2.8F;

    private static final EntityDataAccessor<Integer> DATA_VISUAL_TYPE =
            SynchedEntityData.defineId(JutsuVisualEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX_LIFE =
            SynchedEntityData.defineId(JutsuVisualEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(JutsuVisualEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(JutsuVisualEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(JutsuVisualEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<net.minecraft.world.entity.EntityReference<LivingEntity>>> DATA_CASTER_UUID =
            SynchedEntityData.defineId(JutsuVisualEffectEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VISUAL_TYPE, TYPE_KIRIN_DRAGON);
        builder.define(DATA_MAX_LIFE, 40);
        builder.define(DATA_TARGET_ID, -1);
        builder.define(DATA_YAW, 0.0F);
        builder.define(DATA_DAMAGE, 22.0F);
        builder.define(DATA_CASTER_UUID, Optional.empty());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        int type = this.getVisualType();
        if (type == TYPE_WATER_WALL) {
            return EntityDimensions.scalable(WATER_WALL_WIDTH, WATER_WALL_HEIGHT);
        }
        if (type == TYPE_EARTH_WALL) {
            return EntityDimensions.scalable(EARTH_WALL_WIDTH, EARTH_WALL_HEIGHT);
        }
        return super.getDimensions(pose);
    }

    @Override
    public void refreshDimensions() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        super.refreshDimensions();
        this.setPos(x, y, z);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_VISUAL_TYPE.equals(key)) {
            if (this.getVisualType() == TYPE_EARTH_WALL) {
                this.noPhysics = false;
            }
            this.refreshDimensions();
        }
    }

    @Override
    public boolean isPickable() {
        int type = this.getVisualType();
        return this.isAlive() && (type == TYPE_WATER_WALL || type == TYPE_EARTH_WALL);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        int type = this.getVisualType();
        return this.isAlive() && (type == TYPE_WATER_WALL || type == TYPE_EARTH_WALL);
    }

    public AABB getWaterWallBarrierBox() {
        return this.getBoundingBox().inflate(0.8D, 0.5D, 0.8D);
    }

    public static boolean isBlockedByWaterWall(Vec3 pos, Vec3 wallOrigin, double halfWidth, double height) {
        if (pos == null || wallOrigin == null) {
            return false;
        }
        return pos.x >= wallOrigin.x - halfWidth && pos.x <= wallOrigin.x + halfWidth
                && pos.y >= wallOrigin.y - 0.5D && pos.y <= wallOrigin.y + height + 0.5D
                && pos.z >= wallOrigin.z - halfWidth && pos.z <= wallOrigin.z + halfWidth;
    }

    public boolean isOwnedByAlly(@Nullable Entity other) {
        if (other == null) {
            return false;
        }
        Optional<UUID> casterUuidOpt = this.getCasterUUID();
        if (casterUuidOpt.isEmpty()) {
            return false;
        }
        UUID casterUuid = casterUuidOpt.get();
        UUID otherRootUuid = ShadowCloneEntity.resolveRootOwnerUUID(other);
        if (otherRootUuid != null && casterUuid.equals(otherRootUuid)) {
            return true;
        }
        if (casterUuid.equals(other.getUUID())) {
            return true;
        }
        Player caster = this.getCasterPlayer();
        return caster != null && (other == caster || ShadowCloneEntity.areAllies(caster, other));
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
    public void tick() {
        super.tick();

        int type = this.getVisualType();
        int targetId = this.getTargetId();

        // 1. Position tracking or movement velocity
        Entity tracked = null;
        if (targetId >= 0) {
            tracked = this.level().getEntity(targetId);
            if (tracked != null && tracked.isAlive()) {
                this.setPos(tracked.getX(), tracked.getY(), tracked.getZ());
            }
        } else {
            Vec3 vel = this.getDeltaMovement();
            if (vel.lengthSqr() > 1.0E-5D) {
                this.setPos(this.getX() + vel.x, this.getY() + vel.y, this.getZ() + vel.z);
            }
        }

        // 2. Server-side Jutsu Combat & Physical Functionality (delegated to JutsuVisualBehavior)
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            int lightLevel = JutsuLightManager.resolveLightLevelForVisualType(type);
            if (lightLevel > 0) {
                JutsuLightManager.updateLight(
                        serverLevel, this, this.blockPosition().above(), lightLevel);
            }
            JutsuVisualBehavior.forType(type).tickServer(this, serverLevel, tracked);

            // Expiration
            if (this.isAlive() && this.tickCount >= this.getMaxLife()) {
                this.discard();
            }
        }
    }

    private boolean ashDetonated = false;

    public boolean isAshDetonated() {
        return this.ashDetonated;
    }

    public void setAshDetonated(boolean ashDetonated) {
        this.ashDetonated = ashDetonated;
    }

    public void detonateAshCloud(ServerLevel serverLevel) {
        AshCloudVisualBehavior.detonate(this, serverLevel);
    }

    public DamageSource createKirinLightningDamage(ServerLevel serverLevel, @Nullable Player caster) {
        return JutsuCombatHelper.lightningDamage(serverLevel, this, caster);
    }

    public DamageSource createChidoriLightningDamage(ServerLevel serverLevel, LivingEntity fieldOwner) {
        return JutsuCombatHelper.lightningDamage(serverLevel, this, fieldOwner);
    }

    public void boostAlliedFireball(FireballJutsuEntity fireball) {
        ElementalInteractionHandler.boostFireballWithWind(fireball);
    }

    public void extinguishFireballWithWaterWall(ServerLevel serverLevel, FireballJutsuEntity fireball) {
        if (!fireball.isImpacted()) {
            fireball.extinguishByWaterWall(serverLevel);
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
        this.refreshDimensions();
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

    public float getEffectDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    public void setEffectDamage(float damage) {
        this.entityData.set(DATA_DAMAGE, damage);
    }

    private int weakenedAmount = 0;

    @Override
    public ChakraElement getElement() {
        return switch (this.getVisualType()) {
            case TYPE_WATER_WALL, TYPE_WATER_VORTEX -> ChakraElement.WATER;
            case TYPE_EARTH_WALL, TYPE_STONE_SPIKES -> ChakraElement.EARTH;
            case TYPE_WIND_SHOCKWAVE -> ChakraElement.WIND;
            case TYPE_AMATERASU_PILLAR, TYPE_ASH_CLOUD -> ChakraElement.FIRE;
            case TYPE_KIRIN_DRAGON, TYPE_CHIDORI_BURST -> ChakraElement.LIGHTNING;
            default -> ChakraElement.NONE;
        };
    }

    @Override
    public int getJutsuPower() {
        if (this.getVisualType() == TYPE_WATER_WALL) {
            return 100;
        }
        return Math.max(40, Math.round(this.getEffectDamage() * 4.0F));
    }

    @Override
    public void weaken(int amount) {
        int type = this.getVisualType();
        if (type == TYPE_WATER_WALL || type == TYPE_EARTH_WALL) {
            return;
        }
        this.weakenedAmount += Math.max(0, amount);
        if (this.getJutsuPower() - this.weakenedAmount <= 0) {
            this.explodeAndDiscard();
        }
    }

    @Override
    public void explodeAndDiscard() {
        int type = this.getVisualType();
        if (type == TYPE_WATER_WALL || type == TYPE_EARTH_WALL) {
            return;
        }
        this.discard();
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
        this.setVisualType(input.getIntOr("VisualType", TYPE_KIRIN_DRAGON));
        this.setMaxLife(input.getIntOr("MaxLife", 40));
        this.setEffectYaw(input.getFloatOr("EffectYaw", 0.0F));
        this.setEffectDamage(input.getFloatOr("EffectDamage", 22.0F));
        input.read("CasterUUID", UUIDUtil.CODEC).ifPresent(this::setCasterUUID);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("VisualType", this.getVisualType());
        output.putInt("MaxLife", this.getMaxLife());
        output.putFloat("EffectYaw", this.getEffectYaw());
        output.putFloat("EffectDamage", this.getEffectDamage());
        this.getCasterUUID().ifPresent(uuid -> output.store("CasterUUID", UUIDUtil.CODEC, uuid));
    }
}
