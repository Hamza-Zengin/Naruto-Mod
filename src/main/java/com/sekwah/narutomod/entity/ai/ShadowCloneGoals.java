package com.sekwah.narutomod.entity.ai;

import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * AI goals extracted from {@link ShadowCloneEntity} for follow, owner-assist targeting, and jutsu combat.
 */
public final class ShadowCloneGoals {

    private ShadowCloneGoals() {}

    public static class CloneFollowOwnerGoal extends Goal {
        private static final double START_FOLLOW_DIST_SQ = 36.0D;
        private static final double STOP_FOLLOW_DIST_SQ = 9.0D;
        private static final double TELEPORT_DIST_SQ = 900.0D;

        private final ShadowCloneEntity clone;
        private final double speedModifier;
        private int timeToRecalcPath;

        public CloneFollowOwnerGoal(ShadowCloneEntity clone, double speedModifier) {
            this.clone = clone;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.clone.getCommandMode() != ShadowCloneEntity.MODE_FOLLOW) {
                return false;
            }
            Player owner = this.clone.getOwner();
            if (owner == null || owner.isSpectator() || !owner.isAlive()) {
                return false;
            }
            return this.clone.distanceToSqr(owner) > START_FOLLOW_DIST_SQ;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.clone.getCommandMode() != ShadowCloneEntity.MODE_FOLLOW) {
                return false;
            }
            Player owner = this.clone.getOwner();
            if (owner == null || owner.isSpectator() || !owner.isAlive()) {
                return false;
            }
            return this.clone.distanceToSqr(owner) > STOP_FOLLOW_DIST_SQ;
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
        }

        @Override
        public void stop() {
            this.clone.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player owner = this.clone.getOwner();
            if (owner == null) return;

            this.clone.getLookControl().setLookAt(owner, 10.0F, (float) this.clone.getMaxHeadXRot());

            double distSq = this.clone.distanceToSqr(owner);
            if (distSq > TELEPORT_DIST_SQ && owner.onGround()) {
                teleportNearOwner(owner);
                return;
            }

            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                this.clone.getNavigation().moveTo(owner, this.speedModifier);
            }
        }

        private void teleportNearOwner(Player owner) {
            for (int i = 0; i < 10; ++i) {
                int dx = this.clone.getRandom().nextIntBetweenInclusive(-3, 3);
                int dz = this.clone.getRandom().nextIntBetweenInclusive(-3, 3);
                int dy = this.clone.getRandom().nextIntBetweenInclusive(-1, 1);
                BlockPos targetPos = owner.blockPosition().offset(dx, dy, dz);
                if (this.clone.level().getBlockState(targetPos).isAir()
                        && this.clone.level().getBlockState(targetPos.above()).isAir()
                        && !this.clone.level().getBlockState(targetPos.below()).isAir()) {
                    this.clone.snapTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D,
                            this.clone.getYRot(), this.clone.getXRot());
                    this.clone.getNavigation().stop();
                    return;
                }
            }
        }
    }

    public static class CloneOwnerHurtByTargetGoal extends TargetGoal {
        private final ShadowCloneEntity clone;
        private LivingEntity ownerLastHurtBy;
        private int timestamp;

        public CloneOwnerHurtByTargetGoal(ShadowCloneEntity clone) {
            super(clone, false);
            this.clone = clone;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (this.clone.getCommandMode() == ShadowCloneEntity.MODE_HOLD
                    && this.clone.getOwner() != null
                    && this.clone.distanceToSqr(this.clone.getOwner()) > 100.0D) {
                return false;
            }
            Player owner = this.clone.getOwner();
            if (owner == null) {
                return false;
            }
            this.ownerLastHurtBy = owner.getLastHurtByMob();
            int i = owner.getLastHurtByMobTimestamp();
            return i != this.timestamp
                    && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT)
                    && !this.ownerLastHurtBy.getUUID().equals(owner.getUUID());
        }

        @Override
        public void start() {
            this.mob.setTarget(this.ownerLastHurtBy);
            Player owner = this.clone.getOwner();
            if (owner != null) {
                this.timestamp = owner.getLastHurtByMobTimestamp();
            }
            super.start();
        }
    }

    public static class CloneOwnerHurtTargetGoal extends TargetGoal {
        private final ShadowCloneEntity clone;
        private LivingEntity ownerLastHurt;
        private int timestamp;

        public CloneOwnerHurtTargetGoal(ShadowCloneEntity clone) {
            super(clone, false);
            this.clone = clone;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (this.clone.getCommandMode() == ShadowCloneEntity.MODE_HOLD
                    && this.clone.getOwner() != null
                    && this.clone.distanceToSqr(this.clone.getOwner()) > 100.0D) {
                return false;
            }
            Player owner = this.clone.getOwner();
            if (owner == null) {
                return false;
            }
            this.ownerLastHurt = owner.getLastHurtMob();
            int i = owner.getLastHurtMobTimestamp();
            return i != this.timestamp
                    && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT)
                    && !this.ownerLastHurt.getUUID().equals(owner.getUUID());
        }

        @Override
        public void start() {
            this.mob.setTarget(this.ownerLastHurt);
            Player owner = this.clone.getOwner();
            if (owner != null) {
                this.timestamp = owner.getLastHurtMobTimestamp();
            }
            super.start();
        }
    }

    public static class CloneJutsuAttackGoal extends Goal {
        private static final float CLONE_JUTSU_DAMAGE_MULTIPLIER = 0.65F;
        private static final int NORMAL_COOLDOWN_TICKS = 120;
        private static final int FAST_COOLDOWN_TICKS = 80;

        private final ShadowCloneEntity clone;

        public CloneJutsuAttackGoal(ShadowCloneEntity clone) {
            this.clone = clone;
        }

        @Override
        public boolean canUse() {
            if (this.clone.getUnlockedCloneJutsus().isEmpty()) return false;
            if (this.clone.getCloneJutsuCooldown() > 0) return false;
            LivingEntity target = this.clone.getTarget();
            return target != null && target.isAlive() && this.clone.hasLineOfSight(target);
        }

        @Override
        public void start() {
            LivingEntity target = this.clone.getTarget();
            if (target == null || !(this.clone.level() instanceof ServerLevel serverLevel)) return;

            double dist = this.clone.distanceTo(target);
            List<Identifier> candidates = new ArrayList<>();
            for (Identifier id : this.clone.getUnlockedCloneJutsus()) {
                String path = id.getPath();
                if ("fireball".equals(path) && dist >= 6.0D && dist <= 20.0D) candidates.add(id);
                else if ("water_bullet".equals(path) && dist >= 5.0D && dist <= 20.0D) candidates.add(id);
                else if ("wind_breaker".equals(path) && dist >= 3.5D && dist <= 12.0D) candidates.add(id);
                else if (("rasengan".equals(path) || "chidori".equals(path)) && dist <= 4.5D) candidates.add(id);
            }

            if (candidates.isEmpty()) return;

            Identifier chosen = candidates.get(this.clone.getRandom().nextInt(candidates.size()));
            this.clone.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.clone.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            serverLevel.playSound(null, this.clone.blockPosition(), NarutoSounds.JUTSU_CAST.get(), SoundSource.PLAYERS, 0.9F, 1.1F);

            Vec3 dir = target.getEyePosition().subtract(this.clone.getEyePosition()).normalize();
            String path = chosen.getPath();

            if ("fireball".equals(path)) {
                FireballJutsuEntity fireball = new FireballJutsuEntity(this.clone, dir.x, dir.y, dir.z);
                Vec3 spawnPos = this.clone.getEyePosition().add(dir.scale(1.3));
                fireball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                serverLevel.addFreshEntity(fireball);
            } else if ("water_bullet".equals(path)) {
                WaterBulletEntity bullet = new WaterBulletEntity(serverLevel, this.clone);
                Vec3 spawnPos = this.clone.getEyePosition().add(dir.scale(1.3));
                bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                bullet.setDamage(10.0F * CLONE_JUTSU_DAMAGE_MULTIPLIER);
                bullet.setMaxLife(25);
                bullet.setDeltaMovement(dir.scale(1.1));
                serverLevel.addFreshEntity(bullet);
            } else if ("wind_breaker".equals(path)) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0, target.getZ(), 18, 0.5, 0.5, 0.5, 0.15,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                target.hurtServer(serverLevel, this.clone.damageSources().mobAttack(this.clone), 8.0F * CLONE_JUTSU_DAMAGE_MULTIPLIER);
                target.setDeltaMovement(dir.scale(1.6).add(0, 0.35, 0));
            } else if ("rasengan".equals(path)) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.3, 0.3, 0.3, 0.15,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                target.hurtServer(serverLevel, this.clone.damageSources().mobAttack(this.clone), 14.0F * CLONE_JUTSU_DAMAGE_MULTIPLIER);
                serverLevel.playSound(null, target.blockPosition(), NarutoSounds.RASENGAN.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            } else if ("chidori".equals(path)) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 24, 0.4, 0.4, 0.4, 0.2,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                target.hurtServer(serverLevel, this.clone.damageSources().mobAttack(this.clone), 14.0F * CLONE_JUTSU_DAMAGE_MULTIPLIER);
                serverLevel.playSound(null, target.blockPosition(), NarutoSounds.CHIDORI.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            }

            this.clone.setCloneJutsuCooldown(this.clone.isFastJutsuCooldown() ? FAST_COOLDOWN_TICKS : NORMAL_COOLDOWN_TICKS);
        }
    }
}
