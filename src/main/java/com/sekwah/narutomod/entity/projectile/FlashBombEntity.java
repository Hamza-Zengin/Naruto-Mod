package com.sekwah.narutomod.entity.projectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.mob.ShinobiVillagerEntity;
import com.sekwah.narutomod.entity.mob.VillageNinjaEntity;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.network.s2c.ClientFlashEffectPacket;
import com.sekwah.narutomod.util.TemporaryLightManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Flash Bomb Entity (Hikaridama)
 * Detonates on impact in a blinding flash that illuminates the area and disorients/stuns hostile targets within 7.5 blocks.
 */
public class FlashBombEntity extends ThrowableItemProjectile {

    public static final double FLASH_RADIUS = 7.5D;
    public static final double SELF_FLASH_RADIUS = 3.0D;
    public static final int FLASH_LIGHT_DURATION_TICKS = TemporaryLightManager.DEFAULT_DURATION_TICKS;

    public record PlayerFlashParams(boolean strongFlash, float intensity, int durationTicks) {
    }

    public FlashBombEntity(EntityType<? extends FlashBombEntity> type, Level level) {
        super(type, level);
    }

    public FlashBombEntity(Level level, LivingEntity shooter) {
        super(NarutoEntities.FLASH_BOMB.get(), shooter, level, new ItemStack(NarutoItems.FLASH_BOMB.get()));
    }

    public FlashBombEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(NarutoEntities.FLASH_BOMB.get(), shooter, level, stack);
    }

    public FlashBombEntity(Level level, double x, double y, double z) {
        super(NarutoEntities.FLASH_BOMB.get(), x, y, z, level, new ItemStack(NarutoItems.FLASH_BOMB.get()));
    }

    @Override
    protected Item getDefaultItem() {
        return NarutoItems.FLASH_BOMB.get();
    }

    public static PlayerFlashParams computePlayerFlash(Vec3 eyePos, Vec3 lookAngle, Vec3 explosionCenter, double bodyDist) {
        Vec3 toExplosion = explosionCenter.subtract(eyePos);
        double eyeDistSq = toExplosion.lengthSqr();
        double dot = 1.0D;
        if (eyeDistSq > 1.0E-6D && lookAngle != null && lookAngle.lengthSqr() > 1.0E-6D) {
            dot = lookAngle.normalize().dot(toExplosion.scale(1.0D / Math.sqrt(eyeDistSq)));
        }
        float falloff = (float) Mth.clamp(1.0D - (bodyDist / FLASH_RADIUS), 0.0D, 1.0D);
        boolean strong = dot > 0.25D || bodyDist < 3.5D;
        if (strong) {
            float intensity = Mth.clamp(0.85F + 0.15F * falloff, 0.85F, 1.0F);
            return new PlayerFlashParams(true, intensity, 90);
        }
        float intensity = Mth.clamp(0.55F + 0.25F * falloff, 0.55F, 0.80F);
        return new PlayerFlashParams(false, intensity, 65);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();

            // Place a real temporary level-15 light block at impact
            BlockPos impactPos = this.blockPosition();
            if (!TemporaryLightManager.placeTemporaryLight(serverLevel, impactPos, FLASH_LIGHT_DURATION_TICKS)) {
                BlockPos fallbackPos = BlockPos.containing(this.position());
                if (!fallbackPos.equals(impactPos)) {
                    TemporaryLightManager.placeTemporaryLight(serverLevel, fallbackPos, FLASH_LIGHT_DURATION_TICKS);
                }
            }

            // Sharp flashbang pop & high-pitched ringing burst
            serverLevel.playSound(null, impactPos, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 2.0F, 1.45F);
            serverLevel.playSound(null, impactPos, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.6F, 1.7F);
            serverLevel.playSound(null, impactPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.8F, 1.85F);

            // Radiant flash + spark burst particles
            serverLevel.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF),
                    x, y + 0.5D, z, 3, 0.25D, 0.25D, 0.25D, 0.0D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    x, y + 0.4D, z, 44, 1.25D, 0.9D, 1.25D, 0.24D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.FIREWORK,
                    x, y + 0.4D, z, 40, 1.0D, 0.8D, 1.0D, 0.30D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            Entity owner = this.getOwner();
            double maxDistSq = FLASH_RADIUS * FLASH_RADIUS;
            double selfDistSq = SELF_FLASH_RADIUS * SELF_FLASH_RADIUS;
            Vec3 explosionCenter = new Vec3(x, y + 0.3D, z);
            AABB area = new AABB(
                    x - FLASH_RADIUS, y - FLASH_RADIUS, z - FLASH_RADIUS,
                    x + FLASH_RADIUS, y + FLASH_RADIUS, z + FLASH_RADIUS);
            List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    target -> target.isAlive()
                            && target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceToSqr(x, y, z) <= maxDistSq);

            for (LivingEntity target : candidates) {
                double distSq = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceToSqr(x, y, z);

                // Thrower self-protection: only mild brief flash within 3.0 blocks, zero debuffs
                if (owner != null && target == owner) {
                    if (distSq <= selfDistSq && !isProtectedFromFlash(target) && target instanceof ServerPlayer serverPlayer) {
                        sendFlashPacket(serverPlayer, 0.40F, 20);
                    }
                    continue;
                }

                if (isProtectedFromFlash(target)) {
                    continue;
                }

                if (target instanceof ServerPlayer serverPlayer) {
                    double dist = Math.sqrt(distSq);
                    PlayerFlashParams flash = computePlayerFlash(
                            serverPlayer.getEyePosition(), serverPlayer.getLookAngle(), explosionCenter, dist);
                    sendFlashPacket(serverPlayer, flash.intensity(), flash.durationTicks());
                    if (flash.strongFlash()) {
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 1));
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 0));
                    } else {
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0));
                    }
                } else if (target instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.setLastHurtByMob(null);
                    mob.getNavigation().stop();
                    mob.setDeltaMovement(0.0D, Math.min(0.0D, mob.getDeltaMovement().y), 0.0D);
                    mob.needsSync = true;
                    mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                    mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 6));
                    mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 140, 0));
                }
            }

            this.discard();
        }
    }

    private static void sendFlashPacket(ServerPlayer serverPlayer, float intensity, int durationTicks) {
        ClientFlashEffectPacket packet = new ClientFlashEffectPacket(intensity, durationTicks);
        if (serverPlayer.connection != null && serverPlayer.connection.hasChannel(packet)) {
            PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
    }

    private boolean isProtectedFromFlash(LivingEntity target) {
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return true;
        }
        Entity owner = this.getOwner();
        if (owner != null && target != owner && ShadowCloneEntity.areAllies(owner, target)) {
            return true;
        }
        return target instanceof VillageNinjaEntity
                || target instanceof ShinobiVillagerEntity
                || target instanceof AbstractVillager;
    }
}
