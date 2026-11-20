package com.sekwah.narutomod.entity.ai;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.jutsus.KawarimiJutsuAbility;
import com.sekwah.narutomod.entity.jutsuprojectile.EarthBulletEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.MudWallEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.VacuumSphereEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Modular, state-based tactical Jutsu AI Goal for Ninja NPCs.
 * Evaluates distance, line of sight, health percentage, and shared/per-ability cooldowns
 * to alternate between ranged elemental projectiles, hand-seal chakra effects,
 * and critical-health defensive reflexes (Kawarimi evasion or Mud Wall).
 */
public class NinjaJutsuAttackGoal<T extends PathfinderMob & NinjaJutsuUser> extends Goal {

    private static final Identifier HAND_SEAL_SPARK_ID =
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "hand_seal_spark");

    private static final class Particles {
        static final BlockParticleOption COBBLESTONE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState());
        static final BlockParticleOption STONE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
    }

    public enum State {
        IDLE,
        HAND_SEAL_WINDUP,
        DEFENSIVE_REFLEX
    }

    private final T mob;
    private final java.util.function.Supplier<NinjaJutsuProfile> profileSupplier;

    private State state = State.IDLE;
    @Nullable
    private NinjaJutsuType pendingAction;
    private int windupTicksRemaining;

    public NinjaJutsuAttackGoal(T mob, NinjaJutsuProfile profile) {
        this(mob, () -> profile);
    }

    public NinjaJutsuAttackGoal(T mob, java.util.function.Supplier<NinjaJutsuProfile> profileSupplier) {
        this.mob = mob;
        this.profileSupplier = profileSupplier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Pure tactical decision function shared by the goal and unit tests.
     * Priority 1: Critical health (<= criticalHealthRatio) under close threat -> Kawarimi or Mud Wall.
     * Priority 2: Ranged distance (min..max) with Line of Sight -> Elemental jutsu.
     */
    @Nullable
    public static NinjaJutsuType selectTacticalAction(
            NinjaJutsuProfile profile,
            NinjaJutsuCooldowns cooldowns,
            double distanceToTarget,
            boolean hasLineOfSight,
            float healthRatio,
            int randomChoiceIndex) {

        if (cooldowns.getGlobalJutsuCooldown() > 0) {
            return null;
        }

        // Priority 1: Critical health + close threat -> Defensive reflex
        if (healthRatio <= profile.getCriticalHealthRatio() && distanceToTarget <= profile.getCloseThreatDistance()) {
            if (profile.isEnabled(NinjaJutsuType.KAWARIMI) && cooldowns.isReady(NinjaJutsuType.KAWARIMI)) {
                return NinjaJutsuType.KAWARIMI;
            }
            if (profile.isEnabled(NinjaJutsuType.MUD_WALL) && cooldowns.isReady(NinjaJutsuType.MUD_WALL)) {
                return NinjaJutsuType.MUD_WALL;
            }
        }

        // Mid/Close range Lightning discharge (Chidori Spark)
        if (hasLineOfSight && distanceToTarget <= 7.5D
                && profile.isEnabled(NinjaJutsuType.CHIDORI_SPARK) && cooldowns.isReady(NinjaJutsuType.CHIDORI_SPARK)) {
            return NinjaJutsuType.CHIDORI_SPARK;
        }

        // Priority 2: Ranged combat (8 - 24 blocks) with clear Line of Sight
        if (hasLineOfSight
                && distanceToTarget >= profile.getMinRangedDistance()
                && distanceToTarget <= profile.getMaxRangedDistance()) {
            List<NinjaJutsuType> rangedCandidates = new ArrayList<>(4);
            if (profile.isEnabled(NinjaJutsuType.FIREBALL) && cooldowns.isReady(NinjaJutsuType.FIREBALL)) {
                rangedCandidates.add(NinjaJutsuType.FIREBALL);
            }
            if (profile.isEnabled(NinjaJutsuType.WATER_BULLET) && cooldowns.isReady(NinjaJutsuType.WATER_BULLET)) {
                rangedCandidates.add(NinjaJutsuType.WATER_BULLET);
            }
            if (profile.isEnabled(NinjaJutsuType.EARTH_BULLET) && cooldowns.isReady(NinjaJutsuType.EARTH_BULLET)) {
                rangedCandidates.add(NinjaJutsuType.EARTH_BULLET);
            }
            if (profile.isEnabled(NinjaJutsuType.VACUUM_SPHERES) && cooldowns.isReady(NinjaJutsuType.VACUUM_SPHERES)) {
                rangedCandidates.add(NinjaJutsuType.VACUUM_SPHERES);
            }
            if (!rangedCandidates.isEmpty()) {
                int idx = Math.floorMod(randomChoiceIndex, rangedCandidates.size());
                return rangedCandidates.get(idx);
            }
        }

        return null;
    }

    @Override
    public boolean canUse() {
        if (this.mob.level().isClientSide()) {
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        NinjaJutsuCooldowns cooldowns = this.mob.getJutsuCooldowns();
        double dist = this.mob.distanceTo(target);
        boolean los = this.mob.hasLineOfSight(target);
        float healthRatio = this.mob.getMaxHealth() > 0.0F ? this.mob.getHealth() / this.mob.getMaxHealth() : 1.0F;

        NinjaJutsuType chosen = selectTacticalAction(
                this.profileSupplier.get(),
                cooldowns,
                dist,
                los,
                healthRatio,
                this.mob.getRandom().nextInt(16));

        if (chosen == null) {
            return false;
        }

        this.pendingAction = chosen;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.state == State.IDLE || this.pendingAction == null) {
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || this.pendingAction == null || !(this.mob.level() instanceof ServerLevel serverLevel)) {
            this.resetState();
            return;
        }

        this.mob.getNavigation().stop();
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.pendingAction == NinjaJutsuType.KAWARIMI || this.pendingAction == NinjaJutsuType.MUD_WALL) {
            this.state = State.DEFENSIVE_REFLEX;
            this.executeDefensiveReflex(serverLevel, target, this.pendingAction);
            this.resetState();
            return;
        }

        NinjaJutsuProfile profile = this.profileSupplier.get();

        // Ranged jutsu: lock global cooldown immediately to prevent same-tick or overlapping casts
        this.state = State.HAND_SEAL_WINDUP;
        this.windupTicksRemaining = Math.max(1, profile.getHandSealWindupTicks());
        this.mob.getJutsuCooldowns().setGlobalJutsuCooldown(
                profile.getGlobalCooldownTicks() + this.windupTicksRemaining);

        this.sendHandSealEffect(serverLevel, target, this.pendingAction);
        serverLevel.playSound(null, this.mob.blockPosition(), NarutoSounds.SEAL_A.get(), SoundSource.HOSTILE, 0.9F, 1.05F);
    }

    @Override
    public void tick() {
        if (this.state != State.HAND_SEAL_WINDUP || this.pendingAction == null) {
            return;
        }

        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive() || !(this.mob.level() instanceof ServerLevel serverLevel)) {
            this.resetState();
            return;
        }

        this.mob.getNavigation().stop();
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.windupTicksRemaining > 0) {
            if (this.windupTicksRemaining % 5 == 0) {
                this.sendHandSealEffect(serverLevel, target, this.pendingAction);
            }
            this.windupTicksRemaining--;
            return;
        }

        if (this.mob.hasLineOfSight(target)) {
            this.executeRangedJutsu(serverLevel, target, this.pendingAction);
        }
        this.resetState();
    }

    @Override
    public void stop() {
        this.resetState();
    }

    private void resetState() {
        this.state = State.IDLE;
        this.pendingAction = null;
        this.windupTicksRemaining = 0;
    }

    private void executeRangedJutsu(ServerLevel serverLevel, LivingEntity target, NinjaJutsuType type) {
        NinjaJutsuProfile profile = this.profileSupplier.get();
        Vec3 dir = target.getEyePosition().subtract(this.mob.getEyePosition());
        if (dir.lengthSqr() < 1.0E-5D) {
            dir = this.mob.getLookAngle();
        } else {
            dir = dir.normalize();
        }

        this.mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        this.sendHandSealEffect(serverLevel, target, type);

        if (type == NinjaJutsuType.FIREBALL) {
            serverLevel.playSound(null, this.mob.blockPosition(), NarutoSounds.JUTSU_CAST.get(), SoundSource.HOSTILE, 1.1F, 0.95F);
            serverLevel.playSound(null, this.mob.blockPosition(), NarutoSounds.FIREBALL_CAST.get(), SoundSource.HOSTILE, 1.2F, 0.95F);
            FireballJutsuEntity fireball = new FireballJutsuEntity(this.mob, dir.x, dir.y, dir.z);
            Vec3 spawnPos = this.mob.getEyePosition().add(dir.scale(1.1D));
            fireball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            serverLevel.addFreshEntity(fireball);

            int cd = profile.getFireballCooldownTicks()
                    + (profile.getFireballCooldownJitter() > 0 ? this.mob.getRandom().nextInt(profile.getFireballCooldownJitter()) : 0);
            this.mob.getJutsuCooldowns().trigger(NinjaJutsuType.FIREBALL, cd, profile.getGlobalCooldownTicks());
        } else if (type == NinjaJutsuType.WATER_BULLET) {
            serverLevel.playSound(null, this.mob.blockPosition(), NarutoSounds.JUTSU_CAST.get(), SoundSource.HOSTILE, 1.0F, 1.1F);
            serverLevel.playSound(null, this.mob.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 1.2F, 1.0F);
            WaterBulletEntity bullet = new WaterBulletEntity(serverLevel, this.mob);
            Vec3 spawnPos = this.mob.getEyePosition().add(dir.scale(1.25D));
            bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            bullet.setDamage(profile.getWaterBulletDamage());
            bullet.setMaxLife(profile.getWaterBulletMaxLife());
            bullet.setDeltaMovement(dir.scale(profile.getWaterBulletSpeed()));
            bullet.alignRotationToMovement();
            serverLevel.addFreshEntity(bullet);

            serverLevel.sendParticles(ParticleTypes.SPLASH, spawnPos.x, spawnPos.y, spawnPos.z,
                    16, 0.35D, 0.35D, 0.35D, 0.08D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            int cd = profile.getWaterBulletCooldownTicks()
                    + (profile.getWaterBulletCooldownJitter() > 0 ? this.mob.getRandom().nextInt(profile.getWaterBulletCooldownJitter()) : 0);
            this.mob.getJutsuCooldowns().trigger(NinjaJutsuType.WATER_BULLET, cd, profile.getGlobalCooldownTicks());
        } else if (type == NinjaJutsuType.EARTH_BULLET) {
            serverLevel.playSound(null, this.mob.blockPosition(), NarutoSounds.JUTSU_CAST.get(), SoundSource.HOSTILE, 1.0F, 0.9F);
            serverLevel.playSound(null, this.mob.blockPosition(), NarutoSounds.MUD_WALL_RISE.get(), SoundSource.HOSTILE, 1.1F, 1.0F);
            EarthBulletEntity bullet = new EarthBulletEntity(serverLevel, this.mob);
            Vec3 spawnPos = this.mob.getEyePosition().add(dir.scale(1.25D));
            bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            bullet.setYRot(this.mob.getYRot());
            bullet.setXRot(this.mob.getXRot());
            bullet.setDamage(profile.getEarthBulletDamage());
            bullet.setMaxLife(profile.getEarthBulletMaxLife());
            bullet.setDeltaMovement(dir.scale(profile.getEarthBulletSpeed()));
            serverLevel.addFreshEntity(bullet);

            serverLevel.sendParticles(Particles.COBBLESTONE, spawnPos.x, spawnPos.y, spawnPos.z,
                    14, 0.3D, 0.3D, 0.3D, 0.08D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            int cd = profile.getEarthBulletCooldownTicks()
                    + (profile.getEarthBulletCooldownJitter() > 0 ? this.mob.getRandom().nextInt(profile.getEarthBulletCooldownJitter()) : 0);
            this.mob.getJutsuCooldowns().trigger(NinjaJutsuType.EARTH_BULLET, cd, profile.getGlobalCooldownTicks());
        } else if (type == NinjaJutsuType.VACUUM_SPHERES) {
            serverLevel.playSound(null, this.mob.blockPosition(), NarutoSounds.JUTSU_CAST.get(), SoundSource.HOSTILE, 1.0F, 1.2F);
            serverLevel.playSound(null, this.mob.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 1.1F, 1.35F);
            Vec3 spawnPos = this.mob.getEyePosition().add(dir.scale(1.1D));
            Vec3 right = new Vec3(-dir.z, 0.0D, dir.x);
            if (right.lengthSqr() > 1.0E-5D) {
                right = right.normalize();
            }
            for (int i = -1; i <= 1; i++) {
                Vec3 shotDir = dir.add(right.scale(i * 0.06D)).normalize();
                VacuumSphereEntity sphere = new VacuumSphereEntity(serverLevel, this.mob);
                sphere.setPos(spawnPos.x + right.x * (i * 0.25D), spawnPos.y, spawnPos.z + right.z * (i * 0.25D));
                sphere.setYRot(this.mob.getYRot());
                sphere.setXRot(this.mob.getXRot());
                sphere.setDamage(profile.getVacuumSpheresDamage());
                sphere.setMaxLife(profile.getVacuumSpheresMaxLife());
                sphere.setDeltaMovement(shotDir.scale(profile.getVacuumSpheresSpeed()));
                serverLevel.addFreshEntity(sphere);
            }

            serverLevel.sendParticles(ParticleTypes.CLOUD, spawnPos.x, spawnPos.y, spawnPos.z,
                    12, 0.25D, 0.25D, 0.25D, 0.08D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            int cd = profile.getVacuumSpheresCooldownTicks()
                    + (profile.getVacuumSpheresCooldownJitter() > 0 ? this.mob.getRandom().nextInt(profile.getVacuumSpheresCooldownJitter()) : 0);
            this.mob.getJutsuCooldowns().trigger(NinjaJutsuType.VACUUM_SPHERES, cd, profile.getGlobalCooldownTicks());
        } else if (type == NinjaJutsuType.CHIDORI_SPARK) {
            serverLevel.playSound(null, this.mob.blockPosition(), NarutoSounds.CHIDORI.get(), SoundSource.HOSTILE, 0.9F, 1.15F);
            var effect = com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                    serverLevel,
                    this.mob.getX(), this.mob.getY() + 0.9D, this.mob.getZ(),
                    com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_CHIDORI_BURST,
                    14, this.mob.getYRot());
            effect.setCasterUUID(this.mob.getUUID());
            effect.setEffectDamage(0.0F);
            serverLevel.addFreshEntity(effect);

            if (this.mob.distanceTo(target) <= 8.5D) {
                target.hurtServer(serverLevel, serverLevel.damageSources().lightningBolt(), profile.getChidoriSparkDamage());
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
            }
            PacketHandler.sentToTracking(new ClientJutsuEffectPacket(
                    Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "elemental_burst"),
                    target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
                    dir, 1.2F, 0x55BBFF), target);
            this.mob.getJutsuCooldowns().trigger(NinjaJutsuType.CHIDORI_SPARK, profile.getChidoriSparkCooldownTicks(), profile.getGlobalCooldownTicks());
        }
    }

    private void executeDefensiveReflex(ServerLevel serverLevel, LivingEntity target, NinjaJutsuType type) {
        NinjaJutsuProfile profile = this.profileSupplier.get();
        if (type == NinjaJutsuType.KAWARIMI) {
            if (this.performSafeKawarimi(serverLevel, target)) {
                this.mob.getJutsuCooldowns().trigger(
                        NinjaJutsuType.KAWARIMI,
                        profile.getKawarimiCooldownTicks(),
                        profile.getGlobalCooldownTicks());
                return;
            }
            // Fallback to Mud Wall if Kawarimi has no safe destination and Mud Wall is ready
            if (profile.isEnabled(NinjaJutsuType.MUD_WALL)
                    && this.mob.getJutsuCooldowns().getAbilityCooldown(NinjaJutsuType.MUD_WALL) <= 0) {
                this.performMudWall(serverLevel, target);
                this.mob.getJutsuCooldowns().trigger(
                        NinjaJutsuType.MUD_WALL,
                        profile.getMudWallCooldownTicks(),
                        profile.getGlobalCooldownTicks());
            }
        } else if (type == NinjaJutsuType.MUD_WALL) {
            this.performMudWall(serverLevel, target);
            this.mob.getJutsuCooldowns().trigger(
                    NinjaJutsuType.MUD_WALL,
                    profile.getMudWallCooldownTicks(),
                    profile.getGlobalCooldownTicks());
        }
    }

    private boolean performSafeKawarimi(ServerLevel serverLevel, LivingEntity target) {
        NinjaJutsuProfile profile = this.profileSupplier.get();
        Vec3 oldPos = this.mob.position();
        Vec3 away = oldPos.subtract(target.position());
        Vec3 horizAway = new Vec3(away.x, 0.0D, away.z);
        if (horizAway.lengthSqr() < 1.0E-4D) {
            Vec3 look = this.mob.getLookAngle();
            horizAway = new Vec3(-look.x, 0.0D, -look.z);
        }
        if (horizAway.lengthSqr() < 1.0E-4D) {
            horizAway = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizAway = horizAway.normalize();
        }

        double dist = profile.getKawarimiDistance();
        Vec3 candidateCenter = oldPos.add(horizAway.scale(dist));
        Vec3 safePos = KawarimiJutsuAbility.findSafeOpenPosition(serverLevel, this.mob, candidateCenter, 5);
        if (safePos == null) {
            return false;
        }

        serverLevel.sendParticles(ParticleTypes.POOF,
                oldPos.x, oldPos.y + 0.9D, oldPos.z,
                20, 0.3D, 0.5D, 0.3D, 0.04D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        serverLevel.playSound(null, BlockPos.containing(oldPos), NarutoSounds.CLONE_POOF.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        this.sendHandSealEffect(serverLevel, target, NinjaJutsuType.KAWARIMI);

        this.mob.teleportTo(safePos.x, safePos.y, safePos.z);
        this.mob.resetFallDistance();
        this.mob.getNavigation().stop();

        serverLevel.sendParticles(ParticleTypes.POOF,
                safePos.x, safePos.y + 0.9D, safePos.z,
                14, 0.25D, 0.4D, 0.25D, 0.03D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        if (profile.getKawarimiSpeedDurationTicks() > 0) {
            this.mob.addEffect(new MobEffectInstance(
                    MobEffects.SPEED,
                    profile.getKawarimiSpeedDurationTicks(),
                    profile.getKawarimiSpeedAmplifier()));
        }
        return true;
    }

    private void performMudWall(ServerLevel serverLevel, LivingEntity target) {
        NinjaJutsuProfile profile = this.profileSupplier.get();
        Vec3 toTarget = target.position().subtract(this.mob.position());
        Vec3 horizDir = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (horizDir.lengthSqr() < 1.0E-4D) {
            Vec3 look = this.mob.getLookAngle();
            horizDir = new Vec3(look.x, 0.0D, look.z).normalize();
        } else {
            horizDir = horizDir.normalize();
        }

        Vec3 wallPos = this.mob.position().add(horizDir.scale(2.2D));
        BlockPos basePos = BlockPos.containing(wallPos.x, this.mob.getY(), wallPos.z);
        float wallYaw = (float) (Mth.atan2(horizDir.z, horizDir.x) * (180.0D / Math.PI)) - 90.0F;

        this.mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        this.sendHandSealEffect(serverLevel, target, NinjaJutsuType.MUD_WALL);

        serverLevel.playSound(null, basePos, NarutoSounds.MUD_WALL_RISE.get(), SoundSource.HOSTILE, 1.3F, 0.9F);
        serverLevel.playSound(null, basePos, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1.5F, 0.75F);

        serverLevel.sendParticles(Particles.STONE,
                wallPos.x, this.mob.getY() + 0.3D, wallPos.z,
                20, 1.1D, 0.35D, 1.1D, 0.08D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

        MudWallEntity mudWall = MudWallEntity.create(
                serverLevel,
                wallPos.x,
                this.mob.getY(),
                wallPos.z,
                profile.getMudWallMaxLifeTicks(),
                wallYaw,
                this.mob.getUUID());
        mudWall.setHealth(profile.getMudWallHealth());
        serverLevel.addFreshEntity(mudWall);
    }

    private void sendHandSealEffect(ServerLevel serverLevel, LivingEntity target, NinjaJutsuType type) {
        Vec3 dir = target.getEyePosition().subtract(this.mob.getEyePosition());
        if (dir.lengthSqr() < 1.0E-5D) {
            dir = this.mob.getLookAngle();
        } else {
            dir = dir.normalize();
        }
        Vec3 handPos = this.mob.getEyePosition().add(dir.scale(0.55D)).subtract(0.0D, 0.25D, 0.0D);
        PacketHandler.sentToTracking(new ClientJutsuEffectPacket(
                HAND_SEAL_SPARK_ID,
                handPos,
                dir,
                1.0F,
                type.getColorHex()
        ), this.mob);
    }
}
