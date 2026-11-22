package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.JutsuCombatHelper;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
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
    public static final int TYPE_STONE_SPIKES = 10;
    public static final int TYPE_ADAMANTINE_CHAINS = 11;
    public static final int TYPE_ASH_CLOUD = 12;
    public static final int TYPE_WATER_WALL = 13;

    public static final float WATER_WALL_WIDTH = 3.6F;
    public static final float WATER_WALL_HEIGHT = 2.8F;
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

    private static final class Particles {
        static final BlockParticleOption DRIPSTONE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.POINTED_DRIPSTONE.defaultBlockState());
    }

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
        return this.getBoundingBox().inflate(1.2D, 0.5D, 1.2D);
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

        // 2. Server-side Jutsu Combat & Physical Functionality (Modularized entities handled in their own classes)
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            int lightLevel = com.sekwah.narutomod.events.JutsuLightManager.resolveLightLevelForVisualType(type);
            if (lightLevel > 0) {
                com.sekwah.narutomod.events.JutsuLightManager.updateLight(
                        serverLevel, this, this.blockPosition().above(), lightLevel);
            }
            switch (type) {
                // --- FŪTON: 3D WIND SHOCKWAVE FUNNEL (Piercing Gust & Deflection) ---
                case TYPE_WIND_SHOCKWAVE -> {
                    Player caster = this.getCasterPlayer();
                    float progress = (float) this.tickCount / (float) Math.max(1, this.getMaxLife());
                    double radius = 1.2D + progress * 1.8D;
                    AABB gustBox = this.getBoundingBox().inflate(radius, 1.2D, radius);

                    DamageSource gustDamage = caster != null
                            ? serverLevel.damageSources().playerAttack(caster)
                            : serverLevel.damageSources().generic();
                    Vec3 push = this.getDeltaMovement().normalize().scale(2.2D).add(0, 0.45D, 0);
                    for (LivingEntity living : JutsuCombatHelper.findHostileTargetsInBox(serverLevel, caster, gustBox)) {
                        living.hurtServer(serverLevel, gustDamage, 10.0F);
                        JutsuCombatHelper.applyVelocity(living, push, true);
                    }

                    List<Projectile> projectiles = serverLevel.getEntitiesOfClass(Projectile.class, gustBox, Projectile::isAlive);
                    for (Projectile projectile : projectiles) {
                        if (caster != null && (projectile.getOwner() == caster || ShadowCloneEntity.areAllies(caster, projectile.getOwner()))) {
                            if (projectile instanceof FireballJutsuEntity fireball) {
                                com.sekwah.narutomod.events.ElementalInteractionHandler.boostFireballWithWind(fireball);
                            }
                            continue;
                        }
                        projectile.setDeltaMovement(this.getDeltaMovement().scale(1.8D));
                        projectile.needsSync = true;
                    }
                }

                // --- SHIKOTSUMYAKU: 3D BONE FOREST (Expanding Spire Trap) ---
                case TYPE_BONE_FOREST -> {
                    if (this.tickCount <= 22 && this.tickCount % 7 == 0) {
                        Player caster = this.getCasterPlayer();
                        double currentRadius = (this.tickCount / 7) * 2.2D + 1.2D;
                        AABB ringBox = this.getBoundingBox().inflate(currentRadius, 1.5D, currentRadius);
                        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, ringBox,
                                e -> e.isAlive() && (caster == null || (e != caster && !ShadowCloneEntity.areAllies(caster, e))));
                        for (LivingEntity v : victims) {
                            v.hurtServer(serverLevel, caster != null ? serverLevel.damageSources().playerAttack(caster) : serverLevel.damageSources().generic(), 7.0F);
                            v.setDeltaMovement(v.getDeltaMovement().x, 0.48D, v.getDeltaMovement().z);
                            v.needsSync = true;
                            v.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1));
                        }
                    }
                }

                // --- AMATERASU: 3D BLACK FLAME PILLAR (Inextinguishable Inferno Zone) ---
                case TYPE_AMATERASU_PILLAR -> {
                    if (this.tickCount % 10 == 0) {
                        Player caster = this.getCasterPlayer();
                        AABB flameBox = this.getBoundingBox().inflate(1.8D, 1.2D, 1.8D);
                        List<LivingEntity> burning = serverLevel.getEntitiesOfClass(LivingEntity.class, flameBox,
                                e -> e.isAlive() && (caster == null || (e != caster && !ShadowCloneEntity.areAllies(caster, e))));
                        for (LivingEntity b : burning) {
                            b.hurtServer(serverLevel, caster != null ? serverLevel.damageSources().playerAttack(caster) : serverLevel.damageSources().inFire(), 5.0F);
                            b.setRemainingFireTicks(240);
                            b.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2));
                        }
                    }
                }

                // --- RAITON: 3D KIRIN LIGHTNING DRAGON (Timed Thunderous Ground Slam) ---
                case TYPE_KIRIN_DRAGON -> {
                    if (this.tickCount == 6) {
                        Player caster = this.getCasterPlayer();
                        // Ground slam moment
                        serverLevel.playSound(null, this.blockPosition(), NarutoSounds.KIRIN_STRIKE.get(), SoundSource.WEATHER, 3.5F, 0.85F);
                        serverLevel.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3.5F, 0.75F);
                        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6F, 1.0F);

                        AABB slamBox = this.getBoundingBox().inflate(6.5D, 3.0D, 6.5D);
                        List<LivingEntity> struck = serverLevel.getEntitiesOfClass(LivingEntity.class, slamBox,
                                e -> e.isAlive() && (caster == null || (e != caster && !ShadowCloneEntity.areAllies(caster, e))));
                        DamageSource kirinDamage = JutsuCombatHelper.lightningDamage(serverLevel, this, caster);
                        for (LivingEntity s : struck) {
                            s.hurtServer(serverLevel, kirinDamage, 32.0F);
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
                            s.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
                        }
                    }
                }

                case TYPE_CHIDORI_BURST -> {
                    if (this.tickCount % 8 == 0) {
                        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                                NarutoSounds.CHIDORI_LOOP.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
                    }
                    if (targetId >= 0 && this.getEffectDamage() > 0.0F) {
                        if (!(tracked instanceof LivingEntity fieldOwner) || !fieldOwner.isAlive()) {
                            this.discard();
                            return;
                        }
                        double radius = com.sekwah.narutomod.abilities.jutsus.ChidoriStreamAbility.STREAM_RADIUS;
                        double radiusSq = radius * radius;
                        Vec3 center = fieldOwner.position();
                        AABB fieldBox = fieldOwner.getBoundingBox().inflate(radius, 2.5D, radius);

                        // Continuous projectile deflection while the electric field is active
                        List<Projectile> incoming = serverLevel.getEntitiesOfClass(Projectile.class, fieldBox,
                                p -> p.isAlive() && p.getOwner() != fieldOwner
                                        && !ShadowCloneEntity.areAllies(fieldOwner, p.getOwner())
                                        && p.position().distanceToSqr(center) <= radiusSq);
                        for (Projectile projectile : incoming) {
                            Vec3 away = projectile.position().subtract(center);
                            if (away.lengthSqr() < 1.0E-4D) {
                                away = new Vec3(0.0D, 1.0D, 0.0D);
                            }
                            projectile.setDeltaMovement(away.normalize().scale(1.15D).add(0.0D, 0.22D, 0.0D));
                            projectile.needsSync = true;
                        }

                        // Continuous ambient electric sparks around the caster
                        if (this.tickCount % 2 == 0) {
                            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                    center.x, center.y + 1.0D, center.z,
                                    14, 1.9D, 0.7D, 1.9D, 0.14D,
                                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                        }

                        // Periodic lightning shockwave pulse every 10 ticks (0.5s)
                        if (this.tickCount % com.sekwah.narutomod.abilities.jutsus.ChidoriStreamAbility.PULSE_INTERVAL_TICKS == 0) {
                            float pulseDmg = Math.max(4.0F, this.getEffectDamage());
                            List<LivingEntity> shocked = serverLevel.getEntitiesOfClass(LivingEntity.class, fieldBox,
                                    e -> e.isAlive() && e != fieldOwner
                                            && !ShadowCloneEntity.areAllies(fieldOwner, e)
                                            && e.position().distanceToSqr(center) <= radiusSq);
                            DamageSource chidoriDamage = JutsuCombatHelper.lightningDamage(serverLevel, this, fieldOwner);
                            for (LivingEntity victim : shocked) {
                                float finalDmg = victim.isInWaterOrRain() ? pulseDmg * 1.25F : pulseDmg;
                                victim.hurtServer(serverLevel, chidoriDamage, finalDmg);
                                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 2));
                                Vec3 away = victim.position().subtract(center);
                                double horiz = Math.hypot(away.x, away.z);
                                if (horiz > 1.0E-4D) {
                                    victim.push((away.x / horiz) * 0.35D, 0.14D, (away.z / horiz) * 0.35D);
                                }
                                victim.needsSync = true;
                                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                        victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                                        10, 0.25D, 0.35D, 0.25D, 0.12D,
                                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                            }

                            com.sekwah.narutomod.network.PacketHandler.sentToTracking(
                                    new com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket(
                                            net.minecraft.resources.Identifier.fromNamespaceAndPath(
                                                    com.sekwah.narutomod.NarutoMod.MOD_ID, "elemental_burst"),
                                            center.add(0.0D, 1.0D, 0.0D),
                                            new Vec3(0.0D, 1.0D, 0.0D),
                                            1.9F,
                                            0x55CCFF),
                                    fieldOwner);
                        }
                    }
                }

                case TYPE_RASENGAN_SPHERE -> {
                    if (this.tickCount % 8 == 0) {
                        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                                NarutoSounds.RASENGAN_LOOP.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
                    }
                }

                // --- DOTON: DORYŪSŌ (Targeted Earth Stone Spikes Eruption) ---
                case TYPE_STONE_SPIKES -> {
                    if (this.tickCount == 2) {
                        serverLevel.playSound(null, this.blockPosition(), NarutoSounds.MUD_WALL_RISE.get(), SoundSource.PLAYERS, 1.5F, 0.85F);
                        serverLevel.playSound(null, this.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.8F, 0.7F);
                        serverLevel.sendParticles(Particles.DRIPSTONE,
                                this.getX(), this.getY() + 0.6D, this.getZ(),
                                36, 1.6D, 0.8D, 1.6D, 0.16D,
                                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                    }
                    if (this.tickCount == 3 || this.tickCount == 12) {
                        Player caster = this.getCasterPlayer();
                        float baseDmg = Math.max(4.0F, this.getEffectDamage());
                        float hitDmg = this.tickCount == 3 ? baseDmg * 0.70F : baseDmg * 0.35F;
                        AABB spikeBox = this.getBoundingBox().inflate(3.0D, 2.2D, 3.0D);
                        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, spikeBox,
                                e -> e.isAlive() && (caster == null || (!e.equals(caster) && !ShadowCloneEntity.areAllies(caster, e))));
                        for (LivingEntity v : victims) {
                            v.hurtServer(serverLevel, caster != null ? serverLevel.damageSources().playerAttack(caster) : serverLevel.damageSources().generic(), hitDmg);
                            if (this.tickCount == 3) {
                                v.setDeltaMovement(v.getDeltaMovement().x * 0.5D, 0.68D, v.getDeltaMovement().z * 0.5D);
                                v.needsSync = true;
                                v.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
                            } else {
                                v.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                            }
                        }
                    }
                }

                // --- UZUMAKI FŪINJUTSU: KONGŌ FŪSA (Adamantine Sealing Chains) ---
                case TYPE_ADAMANTINE_CHAINS -> {
                    final double sealRadius = 3.4D;
                    final double sealHalfHeight = 2.5D;
                    if (this.tickCount == 1) {
                        serverLevel.sendParticles(ParticleTypes.END_ROD,
                                this.getX(), this.getY() + 0.5D, this.getZ(),
                                18, 1.4D, 0.4D, 1.4D, 0.08D,
                                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                    }
                    boolean doPin = this.tickCount % 2 == 0 && this.tickCount <= 52;
                    boolean doPulse = this.tickCount == 3 || this.tickCount == 18 || this.tickCount == 33 || this.tickCount == 48;
                    Player caster = (doPin || doPulse) ? this.getCasterPlayer() : null;
                    if (doPin) {
                        List<LivingEntity> pinned = JutsuCombatHelper.findHostileTargetsInRadius(
                                serverLevel, caster, this.position(), sealRadius, sealHalfHeight);
                        for (LivingEntity victim : pinned) {
                            Vec3 toCenter = this.position().subtract(victim.position());
                            double pullX = Mth.clamp(toCenter.x * 0.08D, -0.18D, 0.18D);
                            double pullZ = Mth.clamp(toCenter.z * 0.08D, -0.18D, 0.18D);
                            double clampedY = Math.min(0.0D, victim.getDeltaMovement().y);
                            JutsuCombatHelper.applyVelocity(victim, new Vec3(pullX, clampedY, pullZ), false);
                        }
                    }
                    if (this.tickCount == 3 || this.tickCount == 18 || this.tickCount == 33 || this.tickCount == 48) {
                        float baseDmg = Math.max(4.0F, this.getEffectDamage());
                        float pulseDmg = (this.tickCount == 3) ? baseDmg * 0.40F : baseDmg * 0.20F;
                        serverLevel.playSound(null, this.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS,
                                1.35F, this.tickCount == 3 ? 0.82F : 1.08F);
                        serverLevel.sendParticles(ParticleTypes.END_ROD,
                                this.getX(), this.getY() + 0.85D, this.getZ(),
                                14, 1.35D, 0.65D, 1.35D, 0.04D,
                                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                                this.getX(), this.getY() + 0.45D, this.getZ(),
                                18, 1.7D, 0.4D, 1.7D, 0.14D,
                                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

                        List<LivingEntity> victims = JutsuCombatHelper.findHostileTargetsInRadius(
                                serverLevel, caster, this.position(), sealRadius, sealHalfHeight);
                        DamageSource source = JutsuCombatHelper.playerOrMagicDamage(serverLevel, caster);
                        for (LivingEntity victim : victims) {
                            victim.hurtServer(serverLevel, source, pulseDmg);
                            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 3, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, false, false));
                            if (victim instanceof Player targetPlayer) {
                                var targetData = targetPlayer.getData(NinjaCapabilityHandler.NINJA_DATA);
                                targetData.lockChakraRegen(120);
                                float siphon = (this.tickCount == 3) ? 12.0F : 4.0F;
                                targetData.setChakra(Math.max(0.0F, targetData.getChakra() - siphon));
                            }
                        }
                    } else if (this.tickCount == 12 || this.tickCount == 27 || this.tickCount == 42) {
                        serverLevel.sendParticles(ParticleTypes.END_ROD,
                                this.getX(), this.getY() + 0.6D, this.getZ(),
                                6, 1.5D, 0.4D, 1.5D, 0.02D,
                                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                    }
                }

                // --- KATON: HAISEKISHŌ (Ash Pile Burning Cloud & Chain Ignition) ---
                case TYPE_ASH_CLOUD -> {
                    final double cloudRadius = com.sekwah.narutomod.abilities.jutsus.AshPileBurningAbility.CLOUD_RADIUS;
                    if (this.tickCount % 3 == 0) {
                        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                                this.getX(), this.getY() + 0.9D, this.getZ(),
                                10, 1.45D, 0.55D, 1.45D, 0.02D,
                                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                        serverLevel.sendParticles(ParticleTypes.ASH,
                                this.getX(), this.getY() + 1.0D, this.getZ(),
                                14, 1.6D, 0.7D, 1.6D, 0.01D,
                                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                    }

                    // Check for Fire/Explosive ignition triggers every 4 ticks
                    if (!this.ashDetonated && this.tickCount % 4 == 0) {
                        AABB cloudBox = this.getBoundingBox().inflate(cloudRadius, 2.0D, cloudRadius);
                        List<Projectile> igniters = serverLevel.getEntitiesOfClass(Projectile.class, cloudBox,
                                p -> p.isAlive() && (p instanceof FireballJutsuEntity
                                        || p instanceof com.sekwah.narutomod.entity.projectile.ExplosiveKunaiEntity
                                        || p.isOnFire()));
                        if (!igniters.isEmpty()) {
                            detonateAshCloud(serverLevel);
                            return;
                        }
                    }

                    // Periodic searing ash tick damage + Blindness + Slowness II
                    if (!this.ashDetonated && this.tickCount % 10 == 0) {
                        Player caster = this.getCasterPlayer();
                        float baseDmg = Math.max(6.0F, this.getEffectDamage());
                        float dotDmg = baseDmg * 0.12F;
                        List<LivingEntity> victims = JutsuCombatHelper.findHostileTargetsInRadius(
                                serverLevel, caster, this.position(), cloudRadius, 2.2D);
                        DamageSource source = JutsuCombatHelper.playerOrMagicDamage(serverLevel, caster);
                        for (LivingEntity victim : victims) {
                            victim.hurtServer(serverLevel, source, dotDmg);
                            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
                            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0, false, false));
                        }
                    }

                    // Final auto-detonation right before expiry
                    if (!this.ashDetonated && this.tickCount >= this.getMaxLife() - 2) {
                        detonateAshCloud(serverLevel);
                        return;
                    }
                }

                // --- SUITON: SUIJINHEKI (Water Encampment Wall & Projectile Dampening) ---
                case TYPE_WATER_WALL -> {
                    Player caster = this.getCasterPlayer();
                    if (this.tickCount % 3 == 0) {
                        serverLevel.sendParticles(ParticleTypes.SPLASH,
                                this.getX(), this.getY() + 1.1D, this.getZ(),
                                12, 1.35D, 0.75D, 1.35D, 0.08D,
                                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                    }

                    AABB barrierBox = this.getWaterWallBarrierBox();
                    List<Entity> incoming = serverLevel.getEntities(this, barrierBox.inflate(4.0D),
                            e -> e.isAlive()
                                    && !(e instanceof JutsuVisualEffectEntity)
                                    && (e instanceof Projectile || e instanceof JutsuProjectile)
                                    && !this.isOwnedByAlly(e)
                                    && (caster == null || !(e instanceof Projectile p && (p.getOwner() == caster || ShadowCloneEntity.areAllies(caster, p.getOwner())))));
                    for (Entity entity : incoming) {
                        AABB sweptBox = entity.getBoundingBox().expandTowards(entity.getDeltaMovement()).inflate(0.35D);
                        if (!barrierBox.intersects(entity.getBoundingBox()) && !barrierBox.intersects(sweptBox)) {
                            continue;
                        }
                        if (entity instanceof FireballJutsuEntity fireball) {
                            if (!fireball.isImpacted()) {
                                fireball.extinguishByWaterWall(serverLevel);
                            }
                            continue;
                        }
                        boolean hadHorizontalMotion = entity.getDeltaMovement().horizontalDistanceSqr() > 0.01D;
                        if (entity instanceof Projectile projectile && projectile.isOnFire()) {
                            projectile.clearFire();
                            serverLevel.playSound(null, projectile.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.1F, 1.1F);
                            serverLevel.sendParticles(ParticleTypes.CLOUD,
                                    projectile.getX(), projectile.getY() + 0.3D, projectile.getZ(),
                                    12, 0.4D, 0.4D, 0.4D, 0.05D,
                                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                        }
                        if (entity instanceof JutsuProjectile jutsuProj) {
                            if (jutsuProj.getElement() == ChakraElement.FIRE) {
                                jutsuProj.explodeAndDiscard();
                                continue;
                            }
                            entity.setDeltaMovement(0.0D, -0.25D, 0.0D);
                            entity.setNoGravity(false);
                            entity.needsSync = true;
                            if (hadHorizontalMotion) {
                                serverLevel.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 1.1F);
                                serverLevel.sendParticles(ParticleTypes.SPLASH,
                                        entity.getX(), entity.getY() + 0.3D, entity.getZ(),
                                        10, 0.35D, 0.35D, 0.35D, 0.08D,
                                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                            }
                            jutsuProj.weaken(35);
                        } else if (entity instanceof Projectile projectile) {
                            projectile.setDeltaMovement(0.0D, -0.25D, 0.0D);
                            projectile.setNoGravity(false);
                            projectile.needsSync = true;
                            if (hadHorizontalMotion) {
                                serverLevel.playSound(null, projectile.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.85F, 1.2F);
                                serverLevel.sendParticles(ParticleTypes.SPLASH,
                                        projectile.getX(), projectile.getY() + 0.2D, projectile.getZ(),
                                        8, 0.3D, 0.3D, 0.3D, 0.06D,
                                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                            }
                        }
                    }

                    if (this.tickCount % 6 == 0) {
                        float pushDmg = this.tickCount == 6 ? Math.max(4.0F, this.getEffectDamage() * 0.5F) : 1.5F;
                        List<LivingEntity> pushed = JutsuCombatHelper.findHostileTargetsInRadius(
                                serverLevel, caster, this.position(), 3.0D, 2.2D);
                        DamageSource source = JutsuCombatHelper.playerOrMagicDamage(serverLevel, caster);
                        for (LivingEntity target : pushed) {
                            if (target.isOnFire()) {
                                target.clearFire();
                            }
                            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false));
                            if (this.tickCount == 6 || this.tickCount % 18 == 0) {
                                target.hurtServer(serverLevel, source, pushDmg);
                            }
                            Vec3 away = target.position().subtract(caster != null ? caster.position() : this.position());
                            double horiz = Math.hypot(away.x, away.z);
                            if (horiz > 1.0E-4D) {
                                JutsuCombatHelper.applyVelocity(target, new Vec3((away.x / horiz) * 0.65D, 0.20D, (away.z / horiz) * 0.65D), false);
                            }
                        }
                    }
                }
            }

            // Expiration
            if (this.tickCount >= this.getMaxLife()) {
                this.discard();
            }
        }
    }

    private boolean ashDetonated = false;

    public void detonateAshCloud(ServerLevel serverLevel) {
        if (this.ashDetonated || this.getVisualType() != TYPE_ASH_CLOUD) {
            return;
        }
        this.ashDetonated = true;
        Player caster = this.getCasterPlayer();
        float baseDmg = Math.max(8.0F, this.getEffectDamage());
        float burstDmg = baseDmg * 0.75F;
        double radius = com.sekwah.narutomod.abilities.jutsus.AshPileBurningAbility.CLOUD_RADIUS + 0.4D;

        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.7F, 0.9F);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.3F, 0.65F);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY() + 0.9D, this.getZ(),
                2, 0.8D, 0.4D, 0.8D, 0.02D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        serverLevel.sendParticles(ParticleTypes.FLAME,
                this.getX(), this.getY() + 0.8D, this.getZ(),
                48, 1.6D, 0.7D, 1.6D, 0.14D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        List<LivingEntity> victims = JutsuCombatHelper.findHostileTargetsInRadius(
                serverLevel, caster, this.position(), radius, 2.5D);
        DamageSource source = caster != null ? serverLevel.damageSources().playerAttack(caster) : serverLevel.damageSources().inFire();
        for (LivingEntity victim : victims) {
            victim.hurtServer(serverLevel, source, burstDmg);
            victim.setRemainingFireTicks(100);
            Vec3 away = victim.position().subtract(this.position());
            double horiz = Math.hypot(away.x, away.z);
            if (horiz > 1.0E-4D) {
                JutsuCombatHelper.applyVelocity(victim, new Vec3((away.x / horiz) * 1.15D, 0.48D, (away.z / horiz) * 1.15D), true);
            } else {
                JutsuCombatHelper.applyVelocity(victim, new Vec3(0.0D, 0.55D, 0.0D), true);
            }
        }
        this.discard();
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
