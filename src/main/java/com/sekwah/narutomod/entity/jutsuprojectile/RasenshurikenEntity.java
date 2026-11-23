package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.damagesource.NarutoDamageSource;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.network.s2c.ClientCameraShakePacket;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import com.sekwah.narutomod.util.WorldProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Fūton: Rasenshuriken 3D Projectile Entity
 * High-speed spinning 4-bladed wind chakra shuriken that expands into a cellular vortex dome upon impact.
 */
public class RasenshurikenEntity extends AbstractHurtingProjectile implements JutsuProjectile {

    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_IMPACTED =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RasenshurikenEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_VORTEX_TICKS =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RasenshurikenEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    public int time;
    public int lifeSpan = 80;
    private int jutsuPower = 80;

    @Override
    public ChakraElement getElement() {
        return ChakraElement.WIND;
    }

    @Override
    public int getJutsuPower() {
        return this.jutsuPower;
    }

    @Override
    public void weaken(int amount) {
        this.jutsuPower = Math.max(0, this.jutsuPower - amount);
        if (this.jutsuPower <= 0) {
            this.explodeAndDiscard();
        }
    }

    @Override
    public void explodeAndDiscard() {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.5D, this.getZ(), 30, 0.8D, 0.8D, 0.8D, 0.1D,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 1.2F);
        }
        this.discard();
    }

    public RasenshurikenEntity(EntityType<RasenshurikenEntity> entityConstructor, Level level) {
        super(entityConstructor, level);
        this.time = 0;
        this.accelerationPower = 0.32D;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IMPACTED, false);
        builder.define(DATA_VORTEX_TICKS, 0);
    }

    public RasenshurikenEntity(EntityType<? extends AbstractHurtingProjectile> type, double x, double y, double z, double xVel, double yVel, double zVel, Level level) {
        super(type, x, y, z, new Vec3(xVel, yVel, zVel), level);
        this.accelerationPower = 0.32D;
    }

    public RasenshurikenEntity(LivingEntity shooter, double xVel, double yVel, double zVel) {
        this(NarutoEntities.RASENSHURIKEN.get(), shooter.getX(), shooter.getEyeY() - 0.1f, shooter.getZ(), xVel, yVel, zVel, shooter.level());
        Vec3 look = shooter.getLookAngle();
        this.setPos(shooter.getX() + look.x * 0.65D, (shooter.getEyeY() - 0.1D) + look.y * 0.65D, shooter.getZ() + look.z * 0.65D);
        this.setOwner(shooter);
        this.setRot(shooter.getYRot(), shooter.getXRot());
        this.accelerationPower = 0.32D;
        Vec3 dir = new Vec3(xVel, yVel, zVel);
        if (dir.lengthSqr() > 1.0E-6D) {
            this.setDeltaMovement(dir.normalize().scale(0.95D * dir.length()));
            this.needsSync = true;
        }
    }

    @Override
    public void tick() {
        if (this.isImpacted()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.time++;
            int vt = this.getVortexTicks() + 1;
            if (!this.level().isClientSide()) {
                this.entityData.set(DATA_VORTEX_TICKS, vt);
            }
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                com.sekwah.narutomod.events.JutsuLightManager.updateLight(
                        serverLevel, this, this.blockPosition(),
                        com.sekwah.narutomod.events.JutsuLightManager.LIGHT_CHAKRA);
                Entity owner = this.getOwner();
                double radius = 5.5D;
                float baseDmg;
                if (owner instanceof Player player) {
                    var nd = player.getData(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA);
                    baseDmg = com.sekwah.narutomod.abilities.NarutoAbilities.RASENSHURIKEN.get().getDamage(nd);
                    int mastery = nd.getMastery(com.sekwah.narutomod.abilities.NarutoAbilities.RASENSHURIKEN.getId());
                    int tier = com.sekwah.narutomod.abilities.Ability.getMasteryTier(mastery);
                    if (tier >= 5) radius = 7.0D;
                    else if (tier >= 3) radius = 6.2D;
                } else {
                    baseDmg = com.sekwah.narutomod.abilities.NarutoAbilities.RASENSHURIKEN.get().getDamage();
                }

                // Spiral wind dome particles
                for (int i = 0; i < 12; i++) {
                    double angle = (vt * 0.45) + (i * Math.PI / 6.0);
                    double r = 1.5 + (i % 3) * 1.4;
                    double px = this.getX() + Math.cos(angle) * r;
                    double pz = this.getZ() + Math.sin(angle) * r;
                    double py = this.getY() + (this.random.nextDouble() - 0.3) * 2.5;
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, px, py, pz, 1, 0.1, 0.1, 0.1, 0.0,
                            net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                    serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 2, 0.15, 0.15, 0.15, 0.02,
                            net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                }

                // Multi-hit cellular wind blades & pull towards vortex center
                if (vt % 4 == 0) {
                    AABB box = this.getBoundingBox().inflate(radius);
                    List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                            e -> e.isAlive() && e != owner && !com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(owner, e));
                    for (LivingEntity target : targets) {
                        double dx = this.getX() - target.getX();
                        double dz = this.getZ() - target.getZ();
                        double distSq = dx * dx + dz * dz;

                        // Line of Sight check via ClipContext
                        HitResult los = serverLevel.clip(new ClipContext(
                                this.position(), target.getEyePosition(),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                        if (los.getType() != HitResult.Type.MISS) {
                            double blockDist = this.distanceToSqr(los.getLocation().x, los.getLocation().y, los.getLocation().z);
                            if (blockDist < distSq * 0.85D) {
                                continue; // Obstructed by terrain
                            }
                        }

                        if (distSq > 1.0E-4D) {
                            double dist = Math.sqrt(distSq);
                            double factor = 0.35D / dist;
                            target.setDeltaMovement(dx * factor, 0.12, dz * factor);
                        }
                        target.needsSync = true;
                        target.setInvulnerableTime(0);
                        target.hurtServer(serverLevel, NarutoDamageSource.rasenshuriken(this, owner), baseDmg);
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2));
                    }
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 1.6f);
                }

                if (vt % 4 == 0) {
                    this.emitClientVortexEffect(serverLevel, 1.35F);
                }

                if (vt >= 28) {
                    this.applyErodedCraterTrail(serverLevel, owner);
                    this.emitClientVortexEffect(serverLevel, 2.2F);
                    this.sendCameraShakeToNearbyPlayers(serverLevel, 1.55F, 22, 36.0F);
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0,
                            net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5f, 1.2f);
                    this.discard();
                }
            }
            return;
        }

        if (!this.level().isClientSide() && !this.isImpacted()) {
            Vec3 start = this.position();
            Vec3 delta = this.getDeltaMovement();
            Vec3 end = start.add(delta);
            HitResult hitResult = this.level().clip(new ClipContext(
                    start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hitResult instanceof BlockHitResult blockHit && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = blockHit.getBlockPos();
                BlockState hitState = this.level().getBlockState(hitPos);
                if (hitState.is(Blocks.LIGHT) || hitState.getCollisionShape(this.level(), hitPos).isEmpty()) {
                    hitResult = BlockHitResult.miss(end, blockHit.getDirection(), hitPos);
                }
            }
            if (hitResult.getType() != HitResult.Type.MISS) {
                end = hitResult.getLocation();
            }
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    this.level(), this, start, end, this.getBoundingBox().expandTowards(delta).inflate(1.0D), this::canHitEntity);
            if (entityHit != null) {
                hitResult = entityHit;
            }
            if (hitResult.getType() != HitResult.Type.MISS && !net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this, hitResult)) {
                this.onHit(hitResult);
                return;
            }
        }

        super.tick();
        ++this.time;

        if (!this.level().isClientSide() && !this.isImpacted() && this.isAlive()) {
            AABB clashBox = this.getBoundingBox().inflate(0.4D);
            List<Entity> others = this.level().getEntities(this, clashBox,
                    e -> e instanceof JutsuProjectile
                            && (!(e instanceof JutsuVisualEffectEntity visual) || JutsuProjectile.isBarrierForProjectile(visual, this.getOwner()))
                            && e.isAlive() && e != this && !this.isPassengerOfSameVehicle(e));
            for (Entity e : others) {
                if (e instanceof JutsuProjectile other && this.isAlive() && e.isAlive()) {
                    this.handleClash(other);
                    if (!this.isAlive()) {
                        return;
                    }
                }
            }
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (this.isAlive()) {
                int lightLevel = com.sekwah.narutomod.events.JutsuLightManager.resolveLightLevelForEntityClass(this.getClass());
                if (lightLevel > 0) {
                    com.sekwah.narutomod.events.JutsuLightManager.updateLight(
                            serverLevel, this, this.blockPosition(), lightLevel);
                }
            }
            if (this.time % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3, this.getZ(), 3, 0.4, 0.1, 0.4, 0.02,
                        net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.3, this.getZ(), 2, 0.3, 0.1, 0.3, 0.01,
                        net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            }
            if (this.time % 5 == 0) {
                serverLevel.playSound(null, this.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6f, 1.8f);
            }
        }

        if (this.lifeSpan-- <= 0) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity instanceof JutsuVisualEffectEntity visual) {
            return JutsuProjectile.isBarrierForProjectile(visual, this.getOwner());
        }
        return super.canHitEntity(entity) && entity != this.getOwner()
                && !com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(this.getOwner(), entity);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (!this.level().isClientSide() && hitResult instanceof BlockHitResult blockHitResult) {
            BlockState hitState = this.level().getBlockState(blockHitResult.getBlockPos());
            hitState.onProjectileHit(this.level(), hitState, blockHitResult, this);
        }
        if (!this.isImpacted()) {
            this.entityData.set(DATA_IMPACTED, true);
            this.entityData.set(DATA_VORTEX_TICKS, 0);
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                this.playSound(SoundEvents.TRIDENT_THUNDER.value(), 2.0f, 1.5f);
                this.emitClientVortexEffect(serverLevel, 1.6F);
                this.sendCameraShakeToNearbyPlayers(serverLevel, 0.85F, 12, 32.0F);
            }
        }
    }

    private void emitClientVortexEffect(ServerLevel serverLevel, float scale) {
        BlockState surfaceState = resolveSurfaceState(serverLevel, this.blockPosition());
        int blockStateId = Block.getId(surfaceState);
        ClientJutsuEffectPacket packet = new ClientJutsuEffectPacket(
                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "rasenshuriken_vortex"),
                this.position(),
                new Vec3(0.0D, 1.0D, 0.0D),
                scale,
                blockStateId
        );
        PacketDistributor.sendToPlayersTrackingEntity(this, packet);
    }

    private void sendCameraShakeToNearbyPlayers(ServerLevel serverLevel, float intensity, int durationTicks, float maxRadius) {
        ClientCameraShakePacket shakePacket = new ClientCameraShakePacket(intensity, durationTicks, this.position(), maxRadius);
        double maxDistSqr = (double) maxRadius * maxRadius;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(this) <= maxDistSqr) {
                PacketDistributor.sendToPlayer(player, shakePacket);
            }
        }
    }

    private void applyErodedCraterTrail(ServerLevel serverLevel, @Nullable Entity owner) {
        BlockPos center = this.blockPosition();
        int radius = 3;
        int radiusSqr = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSqr) {
                    continue;
                }
                for (int dy = 1; dy >= -2; dy--) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = serverLevel.getBlockState(pos);
                    if (!state.isAir() && state.getFluidState().isEmpty()) {
                        BlockState eroded = getErodedCraterState(state);
                        if (eroded != null && eroded != state && WorldProtectionHelper.canModifyBlock(serverLevel, pos, owner)) {
                            serverLevel.setBlockAndUpdate(pos, eroded);
                        }
                        break;
                    }
                }
            }
        }
    }

    public static BlockState resolveSurfaceState(ServerLevel level, BlockPos center) {
        for (int dy = 0; dy >= -3; dy--) {
            BlockPos check = center.offset(0, dy, 0);
            BlockState state = level.getBlockState(check);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return state;
            }
        }
        return Blocks.DIRT.defaultBlockState();
    }

    /**
     * Maps natural/structural surface blocks to eroded crater variants without deleting terrain.
     * Returns null if the block should remain unchanged.
     */
    @Nullable
    public static BlockState getErodedCraterState(BlockState original) {
        if (original.is(Blocks.GRASS_BLOCK)) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (original.is(Blocks.PODZOL) || original.is(Blocks.MYCELIUM)) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (original.is(Blocks.STONE)) {
            return Blocks.COBBLESTONE.defaultBlockState();
        }
        if (original.is(Blocks.DEEPSLATE)) {
            return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        if (original.is(Blocks.STONE_BRICKS)) {
            return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
        if (original.is(Blocks.DEEPSLATE_BRICKS)) {
            return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        }
        return null;
    }

    public boolean isImpacted() {
        return this.entityData.get(DATA_IMPACTED);
    }

    public int getVortexTicks() {
        return this.entityData.get(DATA_VORTEX_TICKS);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.CLOUD;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }
}
